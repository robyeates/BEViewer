package co.rob.state;

import co.rob.ui.CopyableLineProvider;
import co.rob.util.*;
import co.rob.ui.highlights.ImageHighlightProducer;
import co.rob.pojo.ImageLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

/**
 * The <code>ImageView</code> class provides image view from page bytes
 * formatted with user highlights.
 */

public class ImageView implements CopyableLineProvider {

    private static final Logger logger = LoggerFactory.getLogger(ImageView.class);

    private final int HEX_BYTES_PER_LINE = 16;
    private final int TEXT_BYTES_PER_LINE = 64;

    // resources
    private final ImageHighlightProducer imageHighlightProducer;

    // cached state
    private ImageModel.ImagePage imagePage;
    private List<byte[]> userHighlights = new ArrayList<>(); // managed by this model
    private boolean highlightMatchCase;

    // calculated state
    private final List<ImageLine> lines = new ArrayList<>();

    // input used to generate image view
    private String pageForensicPath = "";
    private byte[] pageBytes = new byte[0];
    private boolean[] pageHighlightFlags = new boolean[0];
   // private boolean useHexPath = false;
    //private ImageLineFormat imageLineFormat = ImageLineFormat.HEX_FORMAT;
    private int fontSize = 12;

    // this output state allows listeners to know the type of the last change
    private ChangeType changeType = ChangeType.IMAGE_PAGE_CHANGED; // indicates fullest change

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    /**
     * Constructs an Image view and registers it to listen to image model changes.
     */
    @Inject
    public ImageView(ImageModel imageModel, UserHighlightModel userHighlightModel) {
        // resources
        this.imageHighlightProducer = new ImageHighlightProducer();

        UserTextFormatSettings.getInstance().addImageLineFormatPropertyChangeListener(event -> {
            if (event.getPropertyName().equals(UserTextFormatSettings.LINE_FORMAT_PATH_PROPERTY_CHANGE)) {
                lineFormatChanged();
            } else if (event.getPropertyName().equals(UserTextFormatSettings.PATH_FORMAT_PROPERTY_CHANGE)) {
                setLines(ChangeType.FORENSIC_PATH_NUMERIC_BASE_CHANGED);
            } else {
                logger.error("Unknown Event Property found [{}] [{}]", event.getPropertyName(), event.getNewValue());
            }
        });

        // image model listener
        imageModel.addImageModelChangedListener(_ -> {
            ImageModel.ImagePage imagePage = imageModel.getImagePage();
            setImagePage(imagePage);
        });

        // user highlight listener
        userHighlightModel.addUserHighlightModelChangedListener(_ -> {
            List<byte[]> userHighlights = userHighlightModel.getUserHighlightByteVector();
            boolean highlightMatchCase = userHighlightModel.isHighlightMatchCase();
            setUserHighlights(userHighlights, highlightMatchCase);
        });

        // initialize the ImageView model
        setImagePage(imageModel.getImagePage());
    }

    /**
     * Constructs an Image view that is not wired to other models, useful for generating Reports.
     *///TODO DI ImageView and invoke instance
//  @Inject
//  public ImageView() {
//      lineFormatChanged();
//  }

    /**
     * Sets the image view for the given image page.
     */
    public void setImagePage(ImageModel.ImagePage imagePage) {
        this.imagePage = imagePage; // save for use with imageHighlightProducer.getPageHighlightFlags

        // set state that changes for this call
        // set page values
        pageForensicPath = imagePage.pageForensicPath();
        pageBytes = imagePage.pageBytes();
        pageHighlightFlags = imageHighlightProducer.getPageHighlightFlags(imagePage, userHighlights, highlightMatchCase);

        // make change
        setLines(ChangeType.IMAGE_PAGE_CHANGED);
    }

    /**
     * Returns the image page that is currently set in the image view.
     */
    public ImageModel.ImagePage getImagePage() {
        return imagePage;
    }

    /**
     * Sets the user highlight values for the image view.
     */
    public void setUserHighlights(List<byte[]> userHighlights, boolean highlightMatchCase) {
        // set state changes for this call
        this.userHighlights = userHighlights;
        this.highlightMatchCase = highlightMatchCase;
        pageHighlightFlags = imageHighlightProducer.getPageHighlightFlags(imagePage, userHighlights, highlightMatchCase);

        // make the changes
        setLines(ChangeType.USER_HIGHLIGHT_CHANGED);
    }

    /**
     * Returns the forensic path numeric base associated with the currently active image.
     */
    private boolean getUseHexPath() {
        return UserTextFormatSettings.getPathFormat() == PathFormat.HEX_FORMAT;
    }

    /**
     * Sets the line format associated with the currently active image.
     */
    public void lineFormatChanged() {
        //this.imageLineFormat = imageLineFormat;
        setLines(ChangeType.LINE_FORMAT_CHANGED);
    }

    /**
     * Sets font size.
     */
    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
        setLines(ChangeType.FONT_SIZE_CHANGED);
    }

    /**
     * Returns the font size.
     */
    public int getFontSize() {
        return fontSize;
    }

    /**
     * Returns the requested paintable line.
     *
     * @param line the line number of the line to return
     */
    public ImageLine getLine(int line) {
        if (line >= lines.size()) {
            throw new IndexOutOfBoundsException();
        }
        return lines.get(line);
    }

    /**
     * Returns the number of paintable lines.
     */
    public int getNumLines() {
        return lines.size();
    }

    /**
     * Recalculates the lines in the view.
     *
     * @param changeType the change type associated with this action
     */
    private void setLines(ChangeType changeType) {

        //this.changeType = changeType;

        // clear any old lines
        lines.clear();

        // set image lines based on the line format
        if (UserTextFormatSettings.getLineFormat() == ImageLineFormat.HEX_FORMAT) {
            setHexLines();
        } else if (UserTextFormatSettings.getLineFormat() == ImageLineFormat.TEXT_FORMAT) {
            setTextLines();
        } else {
            throw new IllegalArgumentException();
        }
        propertyChangeSupport.firePropertyChange("setLines", null, changeType);
    }

    // generate lines in hex view format
    private void setHexLines() {
        final int MAX_CHARS = 80;

        // establish forensic path offset
        long forensicPathOffset = ForensicPath.getOffset(pageForensicPath);

        // establish width of widest forensic path field
        String widestForensicPath = ForensicPath.getAdjustedPath(pageForensicPath, forensicPathOffset + ImageModel.PAGE_SIZE);
        String printableWidestForensicPath = ForensicPath.getPrintablePath(widestForensicPath, getUseHexPath());

        // work through bytes, preparing image lines
        int pageOffset; // start of page is 0
        for (pageOffset = 0; pageOffset < pageBytes.length; pageOffset += HEX_BYTES_PER_LINE) {

            // image line attributes to be prepared
            final String lineForensicPath;
            final String text;
            final int[] highlightIndexes = new int[MAX_CHARS];
            int numHighlights = 0;

            // prepare this image line
            StringBuilder builder = new StringBuilder(MAX_CHARS);

            // determine the line's forensic path based on the page offset
            lineForensicPath = ForensicPath.getAdjustedPath(pageForensicPath, forensicPathOffset + pageOffset);

            // format: forensic path, binary values where legal, hex values
            // format: [00000000]00000000  ..fslfejl.......  00000000 00000000 00000000 00000000

            // add the forensic path
            String printablePath = ForensicPath.getPrintablePath(lineForensicPath, getUseHexPath());
            builder.append(printablePath);

            // tab out varying width
            builder.append(" ".repeat(Math.max(0, printableWidestForensicPath.length() - printablePath.length())));

            // add spacing between path and data
            builder.append("  ");

            // add the ascii values
            for (int i = 0; i < HEX_BYTES_PER_LINE; i++) {

                if (pageOffset + i < pageBytes.length) {
                    // there are more bytes

                    // if the byte is to be highlighted, record the byte highlight index
                    if (pageHighlightFlags[pageOffset + i]) {
                        highlightIndexes[numHighlights++] = builder.length();
                    }

                    // add byte text
                    byte b = pageBytes[pageOffset + i];
                    if (b > 31 && b < 127) {
                        builder.append((char) b);    // printable
                    } else {
                        builder.append(".");    // not printable
                    }

                } else {
                    builder.append(" ");    // past EOF
                }
            }

            // add the HEX_BYTES_PER_LINE hex values
            for (int i = 0; i < HEX_BYTES_PER_LINE; i++) {

                // pre-space each hex byte
                if (i % 4 == 0) {
                    // double-space bytes between word boundaries
                    builder.append("  ");
                } else {
                    // single-space bytes within word boundaries
                    builder.append(" ");
                }

                // check for EOF
                if (pageOffset + i < pageBytes.length) {
                    // there are more bytes

                    // if the byte is to be highlighted, record the byte highlight index
                    // for the high and low hex characters that represent the byte
                    if (pageHighlightFlags[pageOffset + i]) {
                        highlightIndexes[numHighlights++] = builder.length();
                        highlightIndexes[numHighlights++] = builder.length() + 1;
                    }

                    // add the hex byte
                    try {
                        builder.append(String.format("%1$02x", pageBytes[pageOffset + i]));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                } else {
                    // there are no more bytes
                    builder.append("  ");    // past EOF
                }
            }

            // create the line's text from the temporary text buffer
            text = builder.toString();

            // create the image line
            ImageLine imageLine = new ImageLine(lineForensicPath, text, highlightIndexes, numHighlights);
            lines.add(imageLine);
        }
    }

    // generate lines in text view format
    private void setTextLines() {

        // establish forensic path offset
        long forensicPathOffset = ForensicPath.getOffset(pageForensicPath);

        // establish width of widest forensic path field
        String widestForensicPath = ForensicPath.getAdjustedPath(pageForensicPath, forensicPathOffset + ImageModel.PAGE_SIZE);
        String printableWidestForensicPath = ForensicPath.getPrintablePath(widestForensicPath, getUseHexPath());

        // work through bytes, preparing image lines
        int pageOffset;
        for (pageOffset = 0; pageOffset < pageBytes.length; pageOffset += TEXT_BYTES_PER_LINE) {

            // image line attributes to be prepared
            final String lineForensicPath;
            final String text;
            final int[] highlightIndexes = new int[TEXT_BYTES_PER_LINE];
            int numHighlights = 0;

            // prepare this image line
            StringBuilder builder = new StringBuilder(TEXT_BYTES_PER_LINE);

            // determine the line's forensic path based on the page offset
            lineForensicPath = ForensicPath.getAdjustedPath(pageForensicPath, forensicPathOffset + pageOffset);


            // format: forensic path, binary values where legal
            // format: [00000000]00000000  ..fslfejl.............................

            // add the forensic path
            String printablePath = ForensicPath.getPrintablePath(lineForensicPath, getUseHexPath());
            builder.append(printablePath);

            // tab out varying width
            builder.append(" ".repeat(Math.max(0, printableWidestForensicPath.length() - printablePath.length())));

            // add spacing between path and data
            builder.append("  ");

            // add the ascii values
            for (int i = 0; i < TEXT_BYTES_PER_LINE; i++) {

                if (pageOffset + i < pageBytes.length) {
                    // there are more bytes

                    // if the byte is to be highlighted, record the byte highlight index
                    if (pageHighlightFlags[pageOffset + i]) {
                        highlightIndexes[numHighlights++] = builder.length();
                    }

                    // add byte text
                    byte b = pageBytes[pageOffset + i];
                    if (b > 31 && b < 127) {
                        builder.append((char) b);    // printable
                    } else {
                        builder.append(".");    // not printable
                    }
                }
            }

            // create the line's text from the temporary text buffer
            text = builder.toString();

            // create the image line
            ImageLine imageLine = new ImageLine(lineForensicPath, text, highlightIndexes, numHighlights);
            lines.add(imageLine);
        }
    }

    /**
     * Returns the image line number containing the feature line given the line format.
     */
    public int getForensicPathLineIndex(String forensicPath) {

        // get bytes per line
        int bytesPerLine;
        if (UserTextFormatSettings.getLineFormat() == ImageLineFormat.HEX_FORMAT) {
            bytesPerLine = HEX_BYTES_PER_LINE;
        } else if (UserTextFormatSettings.getLineFormat() == ImageLineFormat.TEXT_FORMAT) {
            bytesPerLine = TEXT_BYTES_PER_LINE;
        } else {
            throw new IllegalArgumentException();
        }
        // get index based on the line format
        long pathOffset = ForensicPath.getOffset(forensicPath);
        long pageOffset = ForensicPath.getOffset(pageForensicPath);
        int imageLine = (int) ((pathOffset - pageOffset) / bytesPerLine);

        // normalize
        if (imageLine < 0 || imageLine > lines.size()) {
            return -1;
        } else {
            return imageLine;
        }
    }



    /**
     * Implements CopyableLineInterface to provide a copyable line as a String
     */
    public String getCopyableLine(int line) {
        return getLine(line).text();
    }


    /**
     * Adds an <code>PropertyChangeListener</code> to PropertyChangeSupport.
     */
    public void addImageViewChangedListener(PropertyChangeListener imageViewChangedListener) {
        propertyChangeSupport.addPropertyChangeListener(imageViewChangedListener);
    }

    /**
     * Removes <code>PropertyChangeListener</code> from PropertyChangeSupport.
     */
    public void removeImageViewChangedListener(PropertyChangeListener imageViewChangedListener) {
        propertyChangeSupport.removePropertyChangeListener(imageViewChangedListener);
    }

    /**
     * The <code>ChangeType</code> class identifies the type of the change that was last requested.
     */
    public enum ChangeType {
        IMAGE_PAGE_CHANGED("Image page changed"),
        USER_HIGHLIGHT_CHANGED("User highlight changed"),
        FORENSIC_PATH_NUMERIC_BASE_CHANGED("Forensic path numeric base changed"),
        LINE_FORMAT_CHANGED("Line format changed"),
        FONT_SIZE_CHANGED("Font size changed");

        private final String name;

        ChangeType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}

