package co.rob.state;

import co.rob.api.ImageReader;
import co.rob.api.ImageReader.ImageReaderResponse;
import co.rob.pojo.FeatureLine;
import co.rob.ui.dialog.WError;
import co.rob.ui.dialog.WIndeterminateProgress;
import co.rob.ui.selection.FeatureLineSelectionManager;
import co.rob.util.ForensicPath;
import co.rob.util.file.FileTools;
import co.rob.util.log.UncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.concurrent.CompletableFuture;

import static co.rob.io.features.FeatureLineUtils.isBlank;

/**
 * The <code>ImageModel</code> class tracks image configuration parameters and fires
 * an image changed event when the model input changes.
 * Use <code>isBusy()</code> or a change listener to identify when the image read is completed.
 */

public class ImageModel {

    private static final Logger logger = LoggerFactory.getLogger(UncaughtExceptionHandler.class);

    // feature attributes that define this model
    private FeatureLine featureLine;
    private String pageForensicPath = "";

    // derived attributes that define this model
    private byte[] pageBytes = new byte[0];
    private byte[] paddedPageBytes = new byte[0];
    private boolean[] pageHighlightFlags = new boolean[0];

    private long paddedPageOffset = 0;
    private int paddingPrefixSize = 0;

    private ImageReaderResponse response = new ImageReaderResponse(new byte[0], 0);

    // values returned by image reader thread
    private long imageSize = 0;

    // model state
    private boolean busy = false;
    private boolean imageSelectionChanged = false;

    // resources
    private CompletableFuture<ImageReaderResponse> readerFuture;
    private final FeatureLineSelectionManager featureLineSelectionManager;
    private final WIndeterminateProgress busyIndicator = new WIndeterminateProgress("Reading Image");
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    /**
     * The default size of the page to be read, {@value}.
     */
    public static final int PAGE_SIZE = 65536;

    private final Runnable fireChanged = () -> {
        // fire signal that the model changed
        propertyChangeSupport.firePropertyChange("imageModel", null, null);
    };

    /**
     * Constructs an Image model and registers it to listen to feature line selection manager changes.
     */
    @Inject
    public ImageModel(FeatureLineSelectionManager featureLineSelectionManager) {
        // resources
        this.featureLineSelectionManager = featureLineSelectionManager;

        // feature line selection manager changed listener
        this.featureLineSelectionManager.addFeatureLineSelectionManagerChangedListener((_, _, newValue) -> {
            // disregard request if this is a histogram line
            if (ForensicPath.isHistogram(newValue.firstField())) {
                return;
            }
            setImageSelection(newValue);
        });
    }

    /**
     * Constructs an Image model that is not wired to other models, useful for generating Reports.
     */
   // public ImageModel() {
   //     featureLineSelectionManager = null;
   // }

    // ************************************************************
    // synchronized reader state control
    // ************************************************************

    /**
     * Sets the image selection image file and forensic path.
     */
    public synchronized void setImageSelection(FeatureLine featureLine) {
        imageSelectionChanged = true;  // synchronized
        this.featureLine = featureLine;
        this.pageForensicPath = ForensicPath.getAlignedPath(featureLine.forensicPath());
        manageModelChanges();
    }

    /**
     * Changes the forensic path to an inclusive aligned page value.
     */
    public synchronized void setImageSelection(String forensicPath) {
        imageSelectionChanged = true;  // synchronized
        this.pageForensicPath = ForensicPath.getAlignedPath(forensicPath);
        manageModelChanges();
    }

    // ************************************************************
    // polling and returned data
    // ************************************************************

    /**
     * Indicates whether the model is busy reading an image.
     *
     * @return true if busy, false if not
     */
    public synchronized boolean isBusy() {
        return busy;
    }

    /**
     * Returns the Image Page structure associated with the currently active image.
     */
    public synchronized ImagePage getImagePage() {
        if (busy) {
            logger.info("ImageModel.getImagePage: note: blank image page provided while busy.");
            return new ImagePage(null, "", new byte[0], new byte[0], 0, PAGE_SIZE, 0);
        }
        return new ImagePage(featureLine, pageForensicPath, pageBytes, paddedPageBytes, paddingPrefixSize, PAGE_SIZE, imageSize);
    }

    // ************************************************************
    // set model inputs and start the image reader thread
    // ************************************************************

    /**
     * Called internally and by ImageReaderThread to reschedule or resolve a read request.
     */
    public synchronized void manageModelChanges() {
        busy = true;

        // NOTE: should use ForensicPath.getPrintablePath() but isHex
        // is not visible to ImageModel.  Also, this is what is sent to the reader.
        // Either way, isHex should be moved into a class of its own to have
        // uniform visibility everywhere.
        String text = "Image file: " + FileTools.getAbsolutePath(featureLine.actualImageFile())
                + "\nFeature path: " + featureLine.forensicPath()
                + "\nRequested read path: " + pageForensicPath;
        logger.info(text);
        busyIndicator.startProgress(text);

        if (readerFuture == null || readerFuture.isDone()) {

            if (isBlank(featureLine)) {
                readerFuture = null;
                imageSelectionChanged = false;
            }

            if (imageSelectionChanged) {
                SwingUtilities.invokeLater(fireChanged);

                long pageOffset = ForensicPath.getOffset(pageForensicPath);
                long paddedPageOffset = Math.max(0, pageOffset - PAGE_SIZE);
                if (paddedPageOffset > imageSize) {
                    paddedPageOffset = pageOffset;
                }
                paddingPrefixSize = (int) (pageOffset - paddedPageOffset);
                String paddedForensicPath = ForensicPath.getAdjustedPath(pageForensicPath, paddedPageOffset);

                // async run instead of Thread
                readerFuture = CompletableFuture.supplyAsync(() ->
                        ImageReaderTask.read(featureLine.featuresFile(), paddedForensicPath,
                                paddingPrefixSize + PAGE_SIZE + PAGE_SIZE)
                );

                readerFuture.whenComplete((response, ex) -> {
                    if (ex != null) {
                        // capture exception on EDT
                        SwingUtilities.invokeLater(() -> {
                            WError.showErrorLater("Unable to read the Image.\n" +
                                            "file: '" + featureLine.actualImageFile() + "' forensic path: '" + paddedForensicPath + "'",
                                    "Error reading Image", new Exception(ex));
                            paddedPageBytes = new byte[0];
                            pageBytes = new byte[0];
                            imageSize = 0;
                            busyIndicator.stopProgress();
                            busy = false;
                            fireChanged.run();
                        });
                    } else {
                        // integrate response on EDT
                        SwingUtilities.invokeLater(() -> {
                            paddedPageBytes = response.bytes();
                            setPageBytes();
                            imageSize = response.totalSizeAtPath();
                            busyIndicator.stopProgress();
                            busy = false;
                            fireChanged.run();
                        });
                    }
                });

                imageSelectionChanged = false;

            } else {
                // no change, just update state
                paddedPageBytes = new byte[0];
                pageBytes = new byte[0];
                imageSize = 0;
                busyIndicator.stopProgress();
                busy = false;
                SwingUtilities.invokeLater(fireChanged);
            }
        } else {
            // previous future still running → ignore
        }
    }

    // this acts on class-local variables
    private void setPageBytes() {
        // return page bytes from within padded page bytes
        int availablePrefixSize = paddingPrefixSize;
        if (availablePrefixSize > paddedPageBytes.length) {
            // the prefix could not fully be read
            availablePrefixSize = paddedPageBytes.length;
        }
        int availablePageSize = paddedPageBytes.length - availablePrefixSize;
        if (availablePageSize > PAGE_SIZE) {
            // remove postfix padding
            availablePageSize = PAGE_SIZE;
        }

        // set pageBytes[] from paddedPageBytes[]
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(availablePageSize);
        outStream.write(paddedPageBytes, availablePrefixSize, availablePageSize);
        pageBytes = outStream.toByteArray();
    }

    // ************************************************************
    // listener registry
    // ************************************************************

    /**
     * Adds an <code>PropertyChangeListener</code> to the listener list.
     *
     * @param imageChangedListener the <code>PropertyChangeListener</code> to be added
     */
    public void addImageModelChangedListener(PropertyChangeListener imageChangedListener) {
        propertyChangeSupport.addPropertyChangeListener(imageChangedListener);
    }

    /**
     * Removes <code>PropertyChangeListener</code> from the listener list.
     *
     * @param imageChangedListener the <code>PropertyChangeListener</code> to be removed
     */
    public void removeImageModelChangedListener(PropertyChangeListener imageChangedListener) {
        propertyChangeSupport.removePropertyChangeListener(imageChangedListener);
    }

    /**
     * @param pageForensicPath path used by model
     */
    public record ImagePage(FeatureLine featureLine, String pageForensicPath, byte[] pageBytes, byte[] paddedPageBytes,
                            int paddingPrefixSize, int defaultPageSize, long imageSize) {
    }

    /**
     * Replacing the legacy <code>ImageReaderThread</code> in order to
     * - Avoid <code>Thread</code> manual interactions
     * - Let <code>CompletableFuture</code> track all state, completion and response
     * - Consolidate error-handling in <code>ImageModel</code>
     */
    public static class ImageReaderTask {
        public static ImageReaderResponse read(File imageFile, String forensicPath, int numBytes) {
            try (ImageReader reader = new ImageReader(imageFile)) {
                ImageReaderResponse response = reader.read(forensicPath, numBytes);
                if (response.bytes().length == 0) {
                    WError.showMessageLater("No bytes were read from the image path, likely because the image file is not available.", "No Data");
                }
                return response;
            } catch (Exception e) {
                WError.showErrorLater("Unable to read the Image.\n" +
                                "file: '" + imageFile + "' forensic path: '" + forensicPath + "'",
                        "Error reading Image", e);
                return new ImageReaderResponse(new byte[0], 0);
            }
        }
    }
}

