package co.rob.io.print;

import co.rob.DaggerContext;
import co.rob.ui.selection.HighlightColorSelectionModel;
import co.rob.util.PathFormat;
import co.rob.util.UserTextFormatSettings;
import co.rob.util.file.FileTools;
import co.rob.util.ForensicPath;
import co.rob.pojo.ImageLine;
import co.rob.state.ImageModel;
import co.rob.state.ImageView;
import co.rob.ui.dialog.WError;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Objects;

public class ImageRangePrinter implements Printable {

    // global working values valid for duration of print call
    private final ImageView imageView;
    private final int minSelectionIndex;
    private final int maxSelectionIndex;
    private final HighlightColorSelectionModel highlightColorSelectionModel;

    private Graphics2D g2d;
    private Font textFont;
    private Font monoFont;
    private int fontHeight;
    private int ascent;
    private int monospaceWidth;

    /**
     * Print the feature associated with this image view and range.
     */
    public ImageRangePrinter(ImageView imageView, int minSelectionIndex, int maxSelectionIndex) {
        this.imageView = imageView;
        imageView.lineFormatChanged();
        this.minSelectionIndex = minSelectionIndex;
        this.maxSelectionIndex = maxSelectionIndex;
        highlightColorSelectionModel = DaggerContext.get().highlightColorSelectionModel();
    }

    /**
     * Initiates the print job for the selected image range.
     */
    public void printImageRange() {
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        printerJob.setPrintable(this);

        if (printerJob.printDialog()) {
            try {
                printerJob.print();
            } catch (PrinterException e) {
                WError.showError("Image range print request failed", "Print failure", e);
            }
        }
    }

    /**
     * Java's `PrinterJob` calls `print` to render each page. This method calculates
     * and paints the content for the specified page.
     *
     * @param graphics   The `Graphics` context to draw on.
     * @param pageFormat The page format.
     * @param pageIndex  The zero-based page index to be rendered.
     * @return `PAGE_EXISTS` if content was printed, `NO_SUCH_PAGE` otherwise.
     */
    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        // Validate the printable content
        var imagePage = imageView.getImagePage();
        if (imagePage.featureLine().actualImageFile() == null || imagePage.featureLine().featuresFile() == null) {
            WError.showError("Invalid Image content", "Print Error", null);
            return NO_SUCH_PAGE;
        }

        // Establish rendering metrics
        this.g2d = (Graphics2D) graphics;
        setupFontsAndMetrics(g2d);

        // Calculate line range for the current page
        var pageMetrics = calculatePageMetrics(pageFormat, pageIndex);
        if (pageMetrics.linesToPrint() <= 0) {
            return NO_SUCH_PAGE;
        }

        // Paint the content
        int x = (int) pageFormat.getImageableX();
        int y = (int) pageFormat.getImageableY();

        if (pageIndex == 0) {
            y = paintHeader(x, y, imagePage);
            y += fontHeight; // Space after header
        }

        paintImageLines(x, y, pageMetrics.startLine(), pageMetrics.linesToPrint());

        return PAGE_EXISTS;
    }

    private void setupFontsAndMetrics(Graphics2D g) {
        textFont = new Font("serif", Font.PLAIN, 10);
        monoFont = new Font("monospaced", Font.PLAIN, 10);
        FontMetrics fontMetrics = g.getFontMetrics(monoFont);
        fontHeight = fontMetrics.getHeight();
        ascent = fontMetrics.getAscent();
        monospaceWidth = fontMetrics.charWidth(' ');
    }

    private int paintHeader(int x, int y, ImageModel.ImagePage imagePage) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(textFont);

        String actualImageFile = FileTools.getAbsolutePath(imagePage.featureLine().actualImageFile());
        String featuresFile = Objects.requireNonNull(imagePage.featureLine().featuresFile()).getAbsolutePath();
        String forensicPath = ForensicPath.getPrintablePath(imagePage.featureLine().forensicPath(), UserTextFormatSettings.getPathFormat() == PathFormat.HEX_FORMAT);

        y = paintPair(x, y, "Image File", actualImageFile);
        y = paintPair(x, y, "Feature File", featuresFile);
        y = paintPair(x, y, "Forensic Path", forensicPath);

        return y;
    }

    private int paintPair(int x, int y, String key, String value) {
        // NOTE: Graphics paint bug: A default font is used when text includes ellipses ("...")
        // so don't specify any font.

        // paint pair and increment y
        g2d.drawString(key, x, y + ascent);
        g2d.drawString(value, x + monospaceWidth * 16, y + ascent);
        return y + fontHeight;
    }

    private void paintImageLines(int x, int y, int startLine, int count) {
        g2d.setFont(monoFont);
        for (int i = 0; i < count; i++) {
            ImageLine imageLine = imageView.getLine(startLine + i);

            // Draw highlight background
            g2d.setColor(highlightColorSelectionModel.getSelectedColor());
            for (int index : imageLine.highlightIndexes()) {
                g2d.fillRect(x + index * monospaceWidth, y, monospaceWidth, fontHeight);
            }

            // Draw text
            g2d.setColor(Color.BLACK);
            g2d.drawString(imageLine.text(), x, y + ascent);

            y += fontHeight; // Move to the next line
        }
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    private PageMetrics calculatePageMetrics(PageFormat pageFormat, int pageIndex) {
        final int headerLines = 4;
        final int linesPerPage = (int) (pageFormat.getImageableHeight() / fontHeight);

        return switch (pageIndex) {
            case 0 -> {
                int maxLinesOnPage = linesPerPage - headerLines;
                int availableLines = maxSelectionIndex - minSelectionIndex + 1;
                int linesToPrint = Math.min(availableLines, maxLinesOnPage);
                yield new PageMetrics(minSelectionIndex, linesToPrint);
            }
            default -> {
                int startLine = minSelectionIndex + (pageIndex * linesPerPage) - headerLines;
                int availableLines = maxSelectionIndex - startLine + 1;
                int linesToPrint = Math.min(availableLines, linesPerPage);
                yield new PageMetrics(startLine, linesToPrint);
            }
        };
    }

    private record PageMetrics(int startLine, int linesToPrint) {
    }
}

