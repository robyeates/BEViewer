package co.rob.state;

import co.rob.ui.CopyableLineProvider;
import co.rob.pojo.FeatureLine;
import co.rob.io.features.FeaturesParserTask;
import co.rob.ui.dialog.WProgress;
import co.rob.ui.selection.ReportSelectionManager;
import co.rob.util.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The <code>FeaturesModel</code> class provides a configured listing of features.
 * The features listing content depends on the feature file selected,
 * the filter used, case sensitivity, and whether hex is used in the path format.
 * The image file is recorded because the features file is associated with it.
 * <p>For optimization, the features listing is generated on a separate dispatch thread.
 * A change signal is thrown when the listing is ready.
 * <p>There is no action when there is no change, tested by object equality.
 */
public class FeaturesModel implements CopyableLineProvider {

    private static final Logger logger = LoggerFactory.getLogger(FeaturesModel.class);

    // the feature line table is cleared during computation and set upon completion
    //STATE!
    private FastFeatureLineTable featureLineTable = new FastFeatureLineTable(null, null);

    // private Model properties
    private final ModelType modelType; // features and histogram or else referenced features
    private ModelRole modelRole = ModelRole.FEATURES_ROLE; // serving feature or histogram entries
    private final ReportSelectionManager reportSelectionManager;

    // feature values
    private File imageFile = null;
    private File featuresFile = null;
    private byte[] filterBytes = new byte[0];
    private boolean filterMatchCase = false;    // only false is allowed for Referenced Features
    private int fontSize = 12;

    // resources
    public WProgress progressBar = new WProgress("Reading");

    // this output state allows listeners to know the type of the last change, nope broken stuff
    // private ChangeType changeType = ChangeType.FEATURES_CHANGED; // indicates fullest change
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Creates FeaturesModel with a role.
     */
    @Inject
    public FeaturesModel(ReportSelectionManager reportSelectionManager, ModelType modelType) {

        this.reportSelectionManager = reportSelectionManager;
        this.reportSelectionManager.addReportSelectionManagerChangedListener(getReportSelectionManagerChangedListener());
        this.modelType = modelType;
        UserTextFormatSettings.getInstance().addImageLineFormatPropertyChangeListener(event -> {
            if (event.getPropertyName().equals(UserTextFormatSettings.LINE_FORMAT_PATH_PROPERTY_CHANGE)) {
                logger.debug("Ignoring LINE_FORMAT_CHANGE event");
            } else if (event.getPropertyName().equals(UserTextFormatSettings.PATH_FORMAT_PROPERTY_CHANGE)) {
                //pass it through
                propertyChangeSupport.firePropertyChange("FeaturesModel.useHexPath", ChangeType.FORMAT_CHANGED, event.getNewValue() == ImageLineFormat.HEX_FORMAT);
            } else {
                logger.error("Unknown Event Property found [{}] [{}]", event.getPropertyName(), event.getNewValue());
            }
        });
    }

    /**
     * Returns the model type: FEATURES_OR_HISTOGRAM or REFERENCED_FEATURES.
     */
    public ModelType getModelType() {
        return modelType;
    }

    /**
     * Returns the model role: FEATURES_ROLE or HISTOGRAM_ROLE.
     */
    public ModelRole getModelRole() {
        return modelRole;
    }

    /**
     * Sets the requested fields then begins recalculating the feature lines.
     *
     * @param imageFile    the file to reference as the image source
     * @param featuresFile the file to be used as the feature source
     */
    public void setReport(File imageFile, File featuresFile, ModelRole modelRole) {
        logger.info("FeaturesModel.setReport image file: {}, featuresFile: {}", imageFile, featuresFile);
        // ignore if object equality
        if (imageFile == this.imageFile && featuresFile == this.featuresFile) {
            return;
        }

        this.modelRole = modelRole;
        this.imageFile = imageFile;
        this.featuresFile = featuresFile;
        runFeaturesParser();
    }

    /**
     * Sets the requested field then begins recalculating the feature lines.
     *
     * @param filterBytes the filter text to filter feature entries
     */
    public void setFilterBytes(final byte[] filterBytes) {
        // no change
        if (UTF8Tools.bytesMatch(filterBytes, this.filterBytes)) {
            return;
        }

        // change
        this.filterBytes = filterBytes;
        runFeaturesParser();
    }

    /**
     * Sets the requested field then begins recalculating the feature lines.
     *
     * @param filterMatchCase whether to ignore case sensitivity when filtering
     */
    public void setFilterMatchCase(final boolean filterMatchCase) {
        // ignore if same
        if (filterMatchCase == this.filterMatchCase) {
            return;
        }

        // only false is allowed for Referenced Features
        if (modelType == ModelType.REFERENCED_FEATURES) {
            throw new RuntimeException("Request not allowed for Referenced Features");
        }

        this.filterMatchCase = filterMatchCase;

        runFeaturesParser();
    }

    /**
     * Returns the filter string associated with the features model
     * used to generate this filter set.
     *
     * @return the filter string associated with the features model
     * used to generate this filter set
     */
    public byte[] getFilterBytes() {
        return filterBytes;
    }

    /**
     * Returns the filter case sensitivity used to generate this filter set       .
     *
     * @return true if case is ignored, false if case is significant
     */
    public boolean isFilterMatchCase() {
        return filterMatchCase;
    }

    /**
     * Sets the path format associated with the view
     *
     * @param useHexPath whether path is formatted in hex
     */
//    private void setUseHexPath(boolean useHexPath) {
//        //no; featureline takes this as an input parameter   if (modelRole == ModelRole.HISTOGRAM_ROLE) throw new RuntimeException("Invalid Usage");
//        // ignore if object equality
//        if (useHexPath == this.useHexPath) {
//            return;
//        }
//        this.useHexPath = useHexPath;
//        propertyChangeSupport.firePropertyChange("FeaturesModel.useHexPath", ChangeType.FORMAT_CHANGED, useHexPath);
//    }

    /**
     * Returns the path format associated with the features model.
     *
     * @return the path format associated with the features model
     */
    private boolean getUseHexPath() {
        return UserTextFormatSettings.getPathFormat() == PathFormat.HEX_FORMAT;
    }

    /**
     * Sets font size.
     */
    public void setFontSize(int fontSize) {
        // ignore if object equality
        if (fontSize == this.fontSize) {
            return;
        }
        this.fontSize = fontSize;
        //WLog.log("FeaturesModel.setFontSize");
        // signal model changed
        propertyChangeSupport.firePropertyChange("FeaturesModel.setFontSize", ChangeType.FORMAT_CHANGED, fontSize);
    }

    /**
     * Returns the font size.
     */
    public int getFontSize() {
        return fontSize;
    }

    /**
     * The FeaturesParserThread invokes this on the Swing thread
     * after the FeatureLineTable has been prepared.
     */
    public void setFeatureLineTable(FastFeatureLineTable newFeatureLineTable) {
        // set the new feature line attributes
        logger.info("Feature Line Table set [{}]", newFeatureLineTable);
        featureLineTable = newFeatureLineTable;

        // signal model changed
        propertyChangeSupport.firePropertyChange("FeaturesModel.setFeatureLineTable", ChangeType.FEATURES_CHANGED, null);
    }

    /**
     * Returns the image file associated with the features model.
     *
     * @return the image file associated with the features model
     */
    public File getImageFile() {
        return imageFile;
    }

    /**
     * Returns the features file associated with the features model.
     *
     * @return the features file associated with the features model
     */
    public File getFeaturesFile() {
        return featuresFile;
    }

    /**
     * Returns the number of active feature lines.
     * The number of active feature lines may be less than the number of features
     * if a filter is active.
     *
     * @return the total number of feature lines
     * or the total number of filtered feature lines, if filtered
     */
    public int getTotalLines() {
        return featureLineTable.size();
    }

    /**
     * Returns the number of bytes of the widest line,
     * which is typically wider than the actual feature.
     *
     * @return the width, in bytes, of the widest line
     */
    public int getWidestLineLength() {
        return featureLineTable.getWidestLineLength();
    }

    /**
     * Returns the feature line corresponding to the given line number.
     *
     * @param lineNumber the line number of the feature line to be returned
     * @return the feature line
     */
    public FeatureLine getFeatureLine(int lineNumber) {
        return featureLineTable.get(lineNumber);
    }

    /**
     * Implements CopyableLineInterface to provide a copyable line as a String
     */
    public String getCopyableLine(int line) {
        FeatureLine featureLine = getFeatureLine(line);
        return ForensicPath.getPrintablePath(featureLine.forensicPath(), getUseHexPath()) + "\t" + featureLine.formattedFeature();
    }

    /**
     * Adds an <code>PropertyChangeListener</code> to the listener list.
     *
     * @param featuresModelChangedListener the <code>PropertyChangeListener</code> to be added
     */
    public void addFeaturesModelChangedListener(PropertyChangeListener featuresModelChangedListener) {
        propertyChangeSupport.addPropertyChangeListener(featuresModelChangedListener);
    }

    /**
     * Removes <code>PropertyChangeListener</code> from the listener list.
     *
     * @param featuresModelChangedListener the <code>PropertyChangeListener</code> to be removed
     */
    public void removeFeaturesModelChangedListener(PropertyChangeListener featuresModelChangedListener) {
        propertyChangeSupport.removePropertyChangeListener(featuresModelChangedListener);
    }

    /**
     * Starts the process that, upon successful completion,
     * sets the completion flag and the feature line table.
     * Features are cleared when the process starts and feature listeners are notified.
     * Upon completion, the completion flag is set and feature listeners are notified.
     *
     * <p>To avoid stalling the AWT event dispatching thread,
     * parsing is performed on a separate parser dispatch thread.
     * If this function is called while a previous run is still active,
     * the running parser dispatch thread is terminated and a new thread is started.
     * Upon completion, the new filter results are accepted
     * by code run on the AWT event dispatching thread.
     * When the parsing takes time to run, a visual progress bar is displayed.
     */
    private void runFeaturesParser() {
        logger.info("FeaturesModel.runFeaturesParserThread: {} imageFile: {}, featuresFile: {}, filterBytes as string: {}, filterMatchCase: {}",
                this, imageFile, featuresFile, new String(filterBytes), filterMatchCase);

        Objects.requireNonNull(filterBytes);

        // clear old lines immediately so the view is clean while the new lines are being prepared
        featureLineTable = new FastFeatureLineTable(null, null);
        propertyChangeSupport.firePropertyChange("FeaturesModel.runFeaturesParser",ChangeType.FEATURES_CHANGED, null);
        // start the parser task
        executor.submit(new FeaturesParserTask(this, imageFile, featuresFile, filterBytes, filterMatchCase));
    }

    // ****************************************
    // Too large for inline definition
    // ****************************************
    private PropertyChangeListener getReportSelectionManagerChangedListener() {
        return evt -> {
            // do not respond to user view preference change
            if (reportSelectionManager.getChangeType() == ReportSelectionManager.ChangeType.PREFERENCE_CHANGED) {
                return;
            }
            // set the image file
            imageFile = reportSelectionManager.getImageFile();
            // set the features file and model role based on the features model type
            // and the report selection type
            if (modelType == ModelType.FEATURES_OR_HISTOGRAM) {
                // this model shows feature or histogram entries
                if (reportSelectionManager.getReportSelectionType() == ReportSelectionManager.ReportSelectionType.HISTOGRAM) {
                    // set role and features file to histogram
                    modelRole = ModelRole.HISTOGRAM_ROLE;
                    featuresFile = reportSelectionManager.getHistogramFile();
                } else {
                    // set role and features file to features
                    modelRole = ModelRole.FEATURES_ROLE;
                    featuresFile = reportSelectionManager.getFeaturesFile();
                }
                // set the report
                runFeaturesParser();
            } else if (modelType == ModelType.REFERENCED_FEATURES) {
                // this model shows referenced features
                modelRole = ModelRole.FEATURES_ROLE;
                // use features file
                featuresFile = reportSelectionManager.getReferencedFeaturesFile();
                // clear any features filtering
                filterBytes = new byte[0];
                // set the report
                runFeaturesParser();
            } else {
                throw new RuntimeException("Invalid type");
            }
        };
    }

    /**
     * The <code>ChangeType</code> class identifies the type of the change that was last requested.
     */
    public record ChangeType(String name) {
        public static final ChangeType FEATURES_CHANGED = new ChangeType("Feature list changed");
        public static final ChangeType FORMAT_CHANGED = new ChangeType("Path format or font changed");

        @NotNull
        public String toString() {
            return name;
        }
    }

    /**
     * The <code>ModelType</code> class identifies which instance the features model is.
     */
    public record ModelType(String name) {
        public static final ModelType FEATURES_OR_HISTOGRAM = new ModelType("Features or histogram, upper window");
        public static final ModelType REFERENCED_FEATURES = new ModelType("Referenced featues, lower window");

        @NotNull
        public String toString() {
            return name;
        }
    }

    /**
     * The <code>ModelRole</code> class identifies whether the model serves histogram or feature data
     */
    public record ModelRole(String name) {
        public static final ModelRole FEATURES_ROLE = new ModelRole("Features role");
        public static final ModelRole HISTOGRAM_ROLE = new ModelRole("Histogram role");

        @NotNull
        public String toString() {
            return name;
        }
    }

}

