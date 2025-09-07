package co.rob.io.print;

import co.rob.pojo.FeatureLine;
import co.rob.state.FeaturesModel;
import co.rob.state.ImageView;
import co.rob.ui.dialog.WError;
import co.rob.ui.highlights.FeatureHighlightIndex;
import co.rob.ui.highlights.FeatureHighlightIndexService;
import co.rob.ui.selection.HighlightColorSelectionModel;
import co.rob.ui.selection.RangeSelectionManager;
import co.rob.util.ForensicPath;
import co.rob.util.PathFormat;
import co.rob.util.UserTextFormatSettings;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.List;

@Singleton
public class FeatureRangePrinter implements Printable {

    private final RangeSelectionManager rangeSelectionManager;
    private FeatureHighlightIndexService featureHighlightIndexService;//TODO initialise?!?

    // global working values valid for duration of print call
    FeaturesModel featuresModel = null;
    private final FeaturesModel referencedFeaturesModel;
    int minSelectionIndex = -1;
    int maxSelectionIndex = -1;
    Graphics g;
    Font textFont;
    Font monoFont;
    FontMetrics textFontMetrics;
    FontMetrics monoFontMetrics;
    int monoTabWidth;
    int monoFontHeight;
    int ascent;
    int monospaceWidth;
    int x;
    int y;
    int linesPerPage;
    int headerSize;
    private final HighlightColorSelectionModel highlightColorSelectionModel;
    private final ImageView imageView;

    @Inject
    public FeatureRangePrinter(RangeSelectionManager rangeSelectionManager,
                               HighlightColorSelectionModel highlightColorSelectionModel,
                               FeatureHighlightIndexService featureHighlightIndexService,
                               ImageView imageView,
                               FeaturesModel featuresModel,
                               FeaturesModel referencedFeaturesModel) {
        this.rangeSelectionManager = rangeSelectionManager;
        this.highlightColorSelectionModel = highlightColorSelectionModel;
        this.imageView = imageView;
        imageView.lineFormatChanged();
        this.featuresModel = featuresModel;
        this.referencedFeaturesModel = referencedFeaturesModel;
    }

    /**
     * Print range
     */
    public void printRange() {
       // SelectionManagerComponents selectionManagerComponents = DaggerSelectionManagerComponents.create();
       // var rangeSelectionManager = selectionManagerComponents.rangeSelectionManager();
       // highlightColorSelectionModel = selectionManagerComponents.highlightColorSelectionModel();
        // this should only be called when valid
        if (!rangeSelectionManager.hasSelection()) {
            throw new RuntimeException("invalid request");
        }

        // forward print request to the appropriate range printer
        int minSelectionIndex = rangeSelectionManager.getMinSelectionIndex();
        int maxSelectionIndex = rangeSelectionManager.getMaxSelectionIndex();
        if (rangeSelectionManager.getProvider() == imageView) {
            new ImageRangePrinter(imageView, minSelectionIndex, maxSelectionIndex).printImageRange();
        } else if (rangeSelectionManager.getProvider() == featuresModel) {
            printFeatureRange(featuresModel, minSelectionIndex, maxSelectionIndex);
        } else if (rangeSelectionManager.getProvider() == referencedFeaturesModel) {
            printFeatureRange(referencedFeaturesModel, minSelectionIndex, maxSelectionIndex);
        } else {
            throw new RuntimeException("invalid request");
        }
    }

    /**
     * Print the feature associated with this image view and range.
     */
    public void printFeatureRange(FeaturesModel featuresModel, int minSelectionIndex, int maxSelectionIndex) {
        this.featuresModel = featuresModel;
        this.minSelectionIndex = minSelectionIndex;
        this.maxSelectionIndex = maxSelectionIndex;
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        printerJob.setPrintable(this);
        boolean userSaysOK = printerJob.printDialog();
        if (userSaysOK) {
            try {
                printerJob.print();
            } catch (PrinterException e) {
                WError.showError("Feature range print request failed", "Print failure", e);
            }
        }
    }

    /**
     * Java's PrinterJob calls print to print indexed pages to Graphics g.
     * This code has been adapted from BasicImageUI.
     */
    public int print(Graphics g, PageFormat pageFormat, int page) throws PrinterException {

        // validate that the image page is printable
        if (featuresModel.getFeaturesFile() == null) {
            WError.showError("Invalid Feature content", "Print Error", null);
            return NO_SUCH_PAGE;
        }

        // establish working values
        this.g = g;
        textFont = new Font("serif", Font.PLAIN, 10);
        monoFont = new Font("monospaced", Font.PLAIN, 10);
        textFontMetrics = g.getFontMetrics(textFont);
        monoFontMetrics = g.getFontMetrics(monoFont);
        monoTabWidth = monoFontMetrics.stringWidth(" ") * 8;
        monoFontMetrics = g.getFontMetrics(monoFont);
        monoFontHeight = monoFontMetrics.getHeight();
        ascent = monoFontMetrics.getAscent();
        monospaceWidth = monoFontMetrics.stringWidth(" ");
        x = (int) (pageFormat.getImageableX());
        y = (int) (pageFormat.getImageableY());
        linesPerPage = (int) (pageFormat.getImageableHeight() / monoFontHeight);
        if (featuresModel.getFilterBytes().length == 0) {
            headerSize = 1 + 1;
        } else {
            headerSize = 2 + 1;
        }

        // calculate the start line and count value for painting this page
        int startLine;
        int count;
        if (page == 0) {
            // start line
            startLine = minSelectionIndex;

            // count
            int countAvailable = maxSelectionIndex - minSelectionIndex + 1;
            int maxCount = linesPerPage - headerSize;
            count = Math.min(countAvailable, maxCount);

        } else {
            // start line
            int pageOffset = linesPerPage * page - headerSize;
            startLine = pageOffset + minSelectionIndex;

            // count
            int countAvailable = maxSelectionIndex - startLine + 1;
            int maxCount = linesPerPage;
            count = Math.min(countAvailable, maxCount);
        }

        // paint this page
        if (page == 0) {
            // if this is page 0 then print header information and the first lines of the image that fit
            // paint header
            paintHeader();

            // paint first page of feature lines
            paintFeatureLines(startLine, count);

            // page 0 always exists
            return PAGE_EXISTS;
        } else {

            // only paint additional pages if they have content
            if (count > 0) {
                paintFeatureLines(startLine, count);
                return PAGE_EXISTS;
            } else {
                return NO_SUCH_PAGE;
            }
        }
    }

    private void paintFeatureLines(int startLine, int count) {
        // paint the formatted Feature lines
        g.setFont(monoFont);
        for (int i = 0; i < count; i++) {

            // get feature line values for the feature line at this index
            FeatureLine featureLine = featuresModel.getFeatureLine(startLine + i);
            final String featureString = featureLine.formattedFeature();
            final char[] featureCharArray = featureString.toCharArray();

            // calculate the text and geometry of the feature line's prefix
            String prefixString = ForensicPath.getPrintablePath(featureLine.forensicPath(), UserTextFormatSettings.getPathFormat() == PathFormat.HEX_FORMAT);
            int prefixWidth = monoFontMetrics.stringWidth(prefixString);
            int tabbedPrefixWidth = prefixWidth + (monoTabWidth - (prefixWidth % monoTabWidth));

            // paint the highlight background for any highlights
            g.setColor(highlightColorSelectionModel.getSelectedColor());    // desired color.
            List<FeatureHighlightIndex> highlightIndexes = featureHighlightIndexService.getHighlightIndexes(featureCharArray);
            for (FeatureHighlightIndex highlightIndex : highlightIndexes) { //TODO this is duplicate code from BasicFeaturesUI
                int beginIndex = highlightIndex.beginIndex();
                int length = highlightIndex.length();
                int xHighlightStart = tabbedPrefixWidth + textFontMetrics.charsWidth(featureCharArray, 0, beginIndex);
                int xHighlightWidth = textFontMetrics.charsWidth(featureCharArray, beginIndex, length);
                g.fillRect(x + xHighlightStart, y, xHighlightWidth, monoFontHeight);
            }

            // set the color for drawing text
            g.setColor(Color.BLACK);    // desired color.

            // draw the prefix of the feature line
            g.setFont(monoFont);
            g.drawString(prefixString, x, y + ascent);

            // draw the Feature portion of the feature line
            g.setFont(textFont);
            g.drawString(featureString, x + tabbedPrefixWidth, y + ascent);
// NOTE: g.drawChars() has Y alignment issues on long lists while g.drawString() does not.

            // move to the next line
            y += monoFontHeight;
        }
    }

    private void paintHeader() {
        g.setColor(Color.BLACK);

        // NOTE: Graphics paint bug: A larger font is used when text includes ellipses ("...").
        g.setFont(textFont);

        // get header information
        String fileType;
        if (featuresModel.getModelRole() == FeaturesModel.ModelRole.FEATURES_ROLE) {
            fileType = "Feature File";
        } else if (featuresModel.getModelRole() == FeaturesModel.ModelRole.HISTOGRAM_ROLE) {
            fileType = "Histogram File";
        } else {
            throw new RuntimeException("bad state");
        }
        String filePath = featuresModel.getFeaturesFile().getAbsolutePath();
        byte[] filterBytes = featuresModel.getFilterBytes();

        // paint header information
        paintValues(fileType, filePath);

        // print filter string if it is being used
        if (filterBytes.length > 0) {
            paintValues("Feature Filter", new String(filterBytes));
        }

        // allow a space between header fields and image lines
        y += monoFontHeight;
    }

    private void paintValues(String key, String value) {
        // NOTE: Graphics paint bug: A default font is used when text includes ellipses ("...")
        // so don't specify any font.

        // paint pair and increment y
        g.drawString(key, x, y + ascent);
        g.drawString(value, x + monospaceWidth * 16, y + ascent);
        y += monoFontHeight;
    }
}

