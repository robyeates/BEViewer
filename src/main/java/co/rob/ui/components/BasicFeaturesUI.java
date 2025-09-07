package co.rob.ui.components;

import co.rob.DaggerContext;
import co.rob.pojo.FeatureLine;
import co.rob.state.FeaturesModel;
import co.rob.state.UserHighlightModel;
import co.rob.ui.components.listeners.HighlightColorSelectionModelChangeListener;
import co.rob.ui.components.listeners.RangeSelectionManagerChangedListener;
import co.rob.ui.highlights.FeatureHighlightIndex;
import co.rob.ui.highlights.FeatureHighlightIndexService;
import co.rob.ui.selection.FeatureLineSelectionManager;
import co.rob.ui.selection.HighlightColorSelectionModel;
import co.rob.ui.selection.RangeSelectionManager;
import co.rob.ui.selection.ReportSelectionManager;
import co.rob.util.ForensicPath;
import co.rob.util.GenericPropertyChangeListener;
import co.rob.util.PathFormat;
import co.rob.util.UserTextFormatSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import static co.rob.io.features.FeatureLineUtils.isBad;

/**
 * A basic implementation of {@code FeaturesUI}.
 */
public class BasicFeaturesUI extends FeaturesUI {

    private static final Logger logger = LoggerFactory.getLogger(BasicFeaturesUI.class);
    private FeatureHighlightIndexService featureHighlightIndexService;
    private HighlightColorSelectionModel highlightColorSelectionModel;
    private FeatureLineSelectionManager featureLineSelectionManager;
    private ReportSelectionManager reportSelectionManager;
    private UserHighlightModel userHighlightModel;
    private FeaturesModel referencedFeaturesModel;

    private FeaturesComponent featuresComponent;
    private FeaturesModel featuresModel;    // FeaturesComponent's data model
    private RangeSelectionManager rangeSelectionManager; // FeaturesComponent's range selection manager

    private RangeSelectionManagerChangedListener rangeSelectionManagerChangedListener;
    private FeaturesModelChangedListener featuresModelChangedListener;
    private UserHighlightModelChangedListener userHighlightModelChangedListener;
    private FeatureLineSelectionManagerChangedListener featureLineSelectionManagerChangedListener;
    private HighlightColorSelectionModelChangeListener highlightColorSelectionModelChangeListener;

    private int mouseDownLine;
    private int selectedLine = -1;

    private FontMetrics textMetrics;

    // cached values set on mouse drag and cleared on model change
    private boolean makingRangeSelection = false;
    private int minSelectionIndex = -1;
    private int maxSelectionIndex = -1;
    private FeaturesUIMouseAdapter featuresUIMouseAdapter;

    public BasicFeaturesUI() {

    }

    /**
     * Returns a new instance of <code>BasicFeaturesUI</code>.
     *
     * @param c the <code>FeaturesComponent</code> object (not used)
     * @see ComponentUI#createUI
     * @return a new <code>BasicFeaturesUI</code> object
     * 🛠️ Looks unused but some Swing reflection requires it
     */
    public static ComponentUI createUI(JComponent c) {
        return new BasicFeaturesUI();
    }

    /**
     * Binds <code>BasicFeaturesUI</code> with <code>FeaturesComponent</code>.
     *
     * @param c the FeaturesComponent
     */
    @Inject
    public void installUI(JComponent c) {

        featuresComponent = (FeaturesComponent) c;
        featuresModel = ((FeaturesComponent) c).getFeaturesModel();

        rangeSelectionManager = ((FeaturesComponent) c).getRangeSelectionManager();
        rangeSelectionManagerChangedListener = new RangeSelectionManagerChangedListener(rangeSelectionManager, featuresModel, minSelectionIndex, maxSelectionIndex, featuresComponent);
        rangeSelectionManager.addRangeSelectionManagerChangedListener(rangeSelectionManagerChangedListener);

        featureHighlightIndexService = DaggerContext.get().featureHighlightIndexService();

        highlightColorSelectionModel = DaggerContext.get().highlightColorSelectionModel();
        highlightColorSelectionModelChangeListener = new HighlightColorSelectionModelChangeListener(featuresComponent);
        highlightColorSelectionModel.addPropertyChangeListener(highlightColorSelectionModelChangeListener);

        featureLineSelectionManager = DaggerContext.get().featureLineSelectionManager();
        reportSelectionManager = DaggerContext.get().reportSelectionManager();

        userHighlightModel = DaggerContext.get().userHighlightModel();
        referencedFeaturesModel = DaggerContext.get().referencedFeaturesModel();

        featuresUIMouseAdapter = new FeaturesUIMouseAdapter();
        featuresComponent.addMouseListener(featuresUIMouseAdapter);
        featuresComponent.addMouseMotionListener(featuresUIMouseAdapter);

        featuresModelChangedListener = new FeaturesModelChangedListener();
        featuresModel.addFeaturesModelChangedListener(featuresModelChangedListener);

        userHighlightModelChangedListener = new UserHighlightModelChangedListener();
        userHighlightModel.addUserHighlightModelChangedListener(userHighlightModelChangedListener);

        featureLineSelectionManagerChangedListener = new FeatureLineSelectionManagerChangedListener();
        featureLineSelectionManager.addFeatureLineSelectionManagerChangedListener(featureLineSelectionManagerChangedListener);

        // NOTE: do not use wheel listener in this component
        // because then wheel events won't go to jscrollpane.

        // respond to keystrokes
        setKeyboardListener();
    }

    /**
     * Unbinds <code>BasicFeaturesUI</code> from <code>FeaturesComponent</code>.
     *
     * @param c the <code>FeaturesComponent</code> object (not used)
     */
    public void uninstallUI(JComponent c) {
        featuresComponent.removeMouseListener(featuresUIMouseAdapter);
        featuresComponent.removeMouseMotionListener(featuresUIMouseAdapter);
        featuresModel.removeFeaturesModelChangedListener(featuresModelChangedListener);
        userHighlightModel.removeUserHighlightModelChangedListener(userHighlightModelChangedListener);
        featureLineSelectionManager.removeFeatureLineSelectionManagerChangedListener(featureLineSelectionManagerChangedListener);
        rangeSelectionManager.removeRangeSelectionManagerChangedListener(rangeSelectionManagerChangedListener);
        highlightColorSelectionModel.removePropertyChangeListener(highlightColorSelectionModelChangeListener);
        featuresModelChangedListener = null;
        userHighlightModelChangedListener = null;
        featureLineSelectionManagerChangedListener = null;
        featuresComponent = null;
        featuresModel = null;
    }

    /**
     * Returns the dimension in points of the view space
     * required to render the view provided by <code>ImageView</code>.
     */
    private Dimension getViewDimension() {
        final int width;
        final int height;

        if (featuresModel.getTotalLines() == 0) {
            width = 0;
            height = 0;
        } else {
            int lineHeight = featuresModel.getTotalLines();
            FeatureLine featureLine = featuresModel.getFeatureLine(0);
            String printableForensicPath = ForensicPath.getPrintablePath(featureLine.forensicPath(), UserTextFormatSettings.getPathFormat() == PathFormat.HEX_FORMAT);
            FontMetrics tempFontMetrics = featuresComponent.getFontMetrics(getTextFont());
            width = tempFontMetrics.stringWidth("W") * ((printableForensicPath.length() + 4) // 4 is plenty to account for extra decimal places
                    + featuresModel.getWidestLineLength());
            height = tempFontMetrics.getHeight() * lineHeight;
        }
        return new Dimension(width, height);
    }

    /**
     * Returns the text font.
     */
    public Font getTextFont() {
        // set monospaced plain for readability
        return new Font("serif", Font.PLAIN, featuresModel.getFontSize());
    }

    /**
     * Returns the prefix font.
     */
    private Font getPrefixFont() {
        // set monospaced plain for readability
        return new Font("monospaced", Font.PLAIN, featuresModel.getFontSize());
    }

    /**
     * Invoked by Swing to draw the lines of text for the Features Component.
     * NOTE: Issue: bug in Utilities.drawTabbedText when using large values of y.
     *      Utilities.drawTabbedText(Segment text, int x, int y, Graphics g, null, 0);
     * Instead, use Graphics.drawString(String string, int x, int y).
     * NOTE: adapted from javax.swing.text.PlainView
     * NOTE: use SIMPLE_SCROLL_MODE in JViewport else clip gets overoptimized
     * and fails to render background color changes properly.
     * NOTE: for highlighting, we use setColor(Color.YELLOW) rather than take an L&F color
     * from UIManager because UIManager has no equivalent.
     * NOTE: this model does not support tabs.  Specifically, it manages the one expected
     * tab after the forensic path or historam but does not manage any other tabs.
     * If tab management is required in the future, either replace tab with space (simple)
     * or call g.drawString multiple times, once per tab, setting y for each tabstop width.
     * NOTE: Swing hangs if array is too large, so strings are truncated in FeatureFieldFormatter.
     * TODO - "Instead of overriding paint, you could use a JList<FeatureLine> with a ListCellRenderer<FeatureLine>."
     * TODO - See if all this manual work is work it..
     */
    public void paint(Graphics graphics, JComponent component) {

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB); // optional, for text

        Rectangle alloc = new Rectangle(component.getPreferredSize());
        Rectangle clip = g2.getClipBounds(); // size of view clip within entire space

        // define geometry based on font
        Font textFont = getTextFont();
        Font prefixFont = getPrefixFont();
        textMetrics = g2.getFontMetrics(textFont); // do not recalculate for duration of paint call
        FontMetrics prefixMetrics = g2.getFontMetrics(prefixFont); // do not recalculate for duration of paint call

        int fontHeight = textMetrics.getHeight();
        int ascent = textMetrics.getAscent();
        int tabWidth = prefixMetrics.stringWidth("w") * 8;

        int linesAbove = getLinesAbove(alloc, clip, fontHeight);
        int linesBelow = getLinesBelow(alloc, clip, fontHeight);
        int linesTotal = alloc.height / fontHeight;
        int endLine = Math.min(featuresModel.getTotalLines(), linesTotal - linesBelow);

        paintBackgroundIfHuge(g2, alloc);

        // draw each visible line
        int y = alloc.y + linesAbove * fontHeight;
        for (int line = linesAbove; line < endLine; line++) {
            FeatureLine featureLine = featuresModel.getFeatureLine(line);
            if (isBad(featureLine)) {
                reportSelectionManager.setReportSelection(null, null);
                return;
            }
            paintFeatureLine(g2, component, featureLine, line, y, ascent, prefixFont, prefixMetrics, textFont, tabWidth, fontHeight);
            y += fontHeight;
        }
    }
    private int getLinesAbove(Rectangle alloc, Rectangle clip, int fontHeight) {
        return Math.max(0, (clip.y - alloc.y) / fontHeight);
    }

    private int getLinesBelow(Rectangle alloc, Rectangle clip, int fontHeight) {
        int heightBelow = (alloc.y + alloc.height) - (clip.y + clip.height);
        return Math.max(0, heightBelow / fontHeight);
    }

    private void paintBackgroundIfHuge(Graphics g2, Rectangle alloc) {
        if (alloc.width > 20000 || alloc.height > 20000) {
            g2.setColor(UIManager.getColor("List.background"));
            g2.fillRect(alloc.x, alloc.y, 20000, 20000);
        }
    }

    private void paintFeatureLine(Graphics g2, JComponent component,
                                  FeatureLine featureLine, int line, int y, int ascent,
                                  Font prefixFont, FontMetrics prefixMetrics,
                                  Font textFont, int tabWidth, int fontHeight) {

        String prefixString = ForensicPath.getPrintablePath(featureLine.forensicPath(), UserTextFormatSettings.getPathFormat() == PathFormat.HEX_FORMAT);
        int prefixWidth = prefixMetrics.stringWidth(prefixString);
        int tabbedPrefixWidth = prefixWidth + (tabWidth - (prefixWidth % tabWidth));

        String featureString = featureLine.formattedFeature();
        char[] featureChars = featureString.toCharArray();
        int textWidth = textMetrics.charsWidth(featureChars, 0, featureChars.length);

        paintSelections(g2, line, y, tabbedPrefixWidth, textWidth, fontHeight);

        paintHighlights(g2, featureChars, tabbedPrefixWidth, y, fontHeight);

        g2.setColor(component.getForeground());
        g2.setFont(prefixFont);
        g2.drawString(prefixString, 0, y + ascent);

        g2.setFont(textFont);

        g2.drawString(featureString, tabbedPrefixWidth, y + ascent);
    }

    private void paintSelections(Graphics g2, int line, int y, int prefixWidth, int textWidth, int fontHeight) {
        Color selectionBg = UIManager.getColor("List.selectionBackground");
        if (line >= minSelectionIndex && line <= maxSelectionIndex) {
            g2.setColor(selectionBg);
            g2.fillRect(0, y, prefixWidth + textWidth, fontHeight);
        }
        if (line == selectedLine) {
            g2.setColor(selectionBg.darker());
            g2.fillRect(0, y, prefixWidth, fontHeight);
        }
    }

    /**
     * float arc = Math.min(UIScale.scale(getArc()), Math.min(width, height));
     *             g2.fill(new RoundRectangle2D.Float(0, 0, width, height, arc, arc));
     */
    private void paintHighlights(Graphics g2, char[] text, int prefixWidth, int y, int fontHeight) {
        g2.setColor(highlightColorSelectionModel.getSelectedColor());
        List<FeatureHighlightIndex> highlightIndexes = featureHighlightIndexService.getHighlightIndexes(text);
        for (FeatureHighlightIndex hi : highlightIndexes) {
            int start = textMetrics.charsWidth(text, 0, hi.beginIndex());
            int width = textMetrics.charsWidth(text, hi.beginIndex(), hi.length());
            g2.fillRect(prefixWidth + start, y, width, fontHeight);
        }
    }

    private void setKeyboardListener() {
        featuresComponent.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (selectedLine < 0) {
                    return;
                }

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        selectLine(selectedLine - 1);
                        break;
                    case KeyEvent.VK_DOWN:
                        selectLine(selectedLine + 1);
                        break;
                }
            }

            // actions for keystrokes
            @Override
            public void keyTyped(KeyEvent e) {
                // escape
                if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
                    // for ESCAPE deselect range if range is selected
                    if (rangeSelectionManager.getProvider() == featuresModel) {
                        rangeSelectionManager.clear();
                        // clear range else clear selection
                        return;
                    }
                    // also, if a histogram selection is in effect, deselect it and clear its filter
                    if (selectedLine != -1 && featuresModel.getModelRole() == FeaturesModel.ModelRole.HISTOGRAM_ROLE) {
                        selectedLine = -1;
                        referencedFeaturesModel.setFilterBytes(new byte[0]);
                        featuresComponent.repaint();
                    }
                }
            }
        });
    }

    // select the specified line
    private void selectLine(int line) {

        // ignore request if out of range
        if (line < 0 || line >= featuresModel.getTotalLines()) {
            return;
        }

        // select the requested line
        selectedLine = line;

        // action depends on feature model type: FEATURES or HISTOGRAM
        FeaturesModel.ModelRole modelRole = featuresModel.getModelRole();
        if (modelRole == FeaturesModel.ModelRole.FEATURES_ROLE) {

            // set the feature selection
            featureLineSelectionManager.setFeatureLineSelection(featuresModel.getFeatureLine(selectedLine));

        } else if (modelRole == FeaturesModel.ModelRole.HISTOGRAM_ROLE) {

            // filter the histogram features model by the feature field
            byte[] matchableFeature = featuresModel.getFeatureLine(selectedLine).featureField();
            referencedFeaturesModel.setFilterBytes(matchableFeature);
        } else {
            throw new RuntimeException("Invalid type");
        }

        // repaint to show view of changed model
        featuresComponent.repaint();
    }

    // The view changes when the features model changes
    private class FeaturesModelChangedListener implements GenericPropertyChangeListener<FeaturesModel.ChangeType> {

        @Override
        public void propertyChange(PropertyChangeEvent evt, FeaturesModel.ChangeType oldValue, FeaturesModel.ChangeType newValue) {

            if (oldValue.equals(FeaturesModel.ChangeType.FEATURES_CHANGED)) {
                logger.info("Got Property Change [{}] old [{}], new [{}]", evt.getPropertyName(), oldValue, newValue);

                // the feature content changed so clear any selections and resize
                // if a histogram selection is in effect then clear the referenced feature
                // NOTE this may be obscure here but the deselection action is what clears the
                // referenced features model.
                if (selectedLine >= 0 && featuresModel.getModelRole() == FeaturesModel.ModelRole.HISTOGRAM_ROLE) {
                    referencedFeaturesModel.setFilterBytes(new byte[0]);
                }
                // undo the selected feature line, if any
                selectedLine = -1;
                // if a range selection is active then clear it
                if (rangeSelectionManager.getProvider() == featuresModel) {
                    rangeSelectionManager.clear();
                }
                // resize since the lines changed
                featuresComponent.setComponentSize(getViewDimension());
                featuresComponent.revalidate();
            } else if (oldValue.equals(FeaturesModel.ChangeType.FORMAT_CHANGED)) {
                // resize since the width changed
                featuresComponent.setComponentSize(getViewDimension());
                featuresComponent.revalidate();
            } else {
                throw new IllegalStateException("Unexpected value: " + oldValue);
            }
            // repaint to show view of changed model
            logger.info("Calling component repaint()");
            featuresComponent.repaint();
        }
    }

    // the view is redrawn when highlighting changes
    private class UserHighlightModelChangedListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            // redraw to render highlight changes
            featuresComponent.repaint();
        }
    }

    // this class listens for a feature line selection change to turn off the selected feature line
    // when the user navigates elsewhere
    private class FeatureLineSelectionManagerChangedListener implements GenericPropertyChangeListener<FeatureLine> {

        @Override
        public void propertyChange(PropertyChangeEvent evt, FeatureLine oldValue, FeatureLine newValue) {
            // deselect the selected line if the selection changes
            if (selectedLine >= 0 && featuresModel.getModelRole() == FeaturesModel.ModelRole.FEATURES_ROLE) {
                if (!newValue.equals(featureLineSelectionManager.getFeatureLineSelection())) {
                    // deselect the selected feature line if it is not a histogram line
                    // and it no longer matches the selection in the image model
                    selectedLine = -1;
                }
            }
            // redraw to render highlight change and also any selection change
            featuresComponent.repaint();
        }
    }



    private final class FeaturesUIMouseAdapter extends MouseAdapter implements MouseMotionListener {
        @Override
        public void mousePressed(MouseEvent e) {
            // give the window focus
            featuresComponent.requestFocusInWindow();
            // set the line index of mouse down
            int line = e.getY() / textMetrics.getHeight();
            if (line >= 0 && line < featuresModel.getTotalLines()) {
                mouseDownLine = line;
            } else {
                mouseDownLine = -1;
            }
            // indicate that a range selection is not in progress
            makingRangeSelection = false;
        }

        /**
         * Selects the feature line if the cursor is hovering over a valid forensic path.
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            // for mouse clicked deselect range if range is selected
            if (rangeSelectionManager.getProvider() == featuresModel) {
                rangeSelectionManager.clear();
            }
            // also perform a feature line selection operation if hovering over a selectable feature line
            if (mouseDownLine >= 0) {
                selectLine(mouseDownLine);
            }
        }

        /**
         * Track any range selection.
         */
        @Override
        public void mouseDragged(MouseEvent e) {
            // range selection happens when mouse drags
            if (mouseDownLine != -1) {

                // indicate that a range selection is in progress
                if (!makingRangeSelection) {
                    makingRangeSelection = true;
                    // clear out any other selection when this selection starts
                    rangeSelectionManager.setRange(featuresModel, mouseDownLine, mouseDownLine);
                }

                // get the line that the mouse is on, but keep the line within the valid line range
                int line = e.getY() / textMetrics.getHeight();
                if (line < 0) {
                    line = 0;
                }
                if (line >= featuresModel.getTotalLines()) {
                    line = featuresModel.getTotalLines() - 1;
                }

                // find the new range selection
                int newMinSelectionIndex;
                int newMaxSelectionIndex;
                if (mouseDownLine <= line) {
                    newMinSelectionIndex = mouseDownLine;
                    newMaxSelectionIndex = line;
                } else {
                    newMinSelectionIndex = line;
                    newMaxSelectionIndex = mouseDownLine;
                }

                // if the new range selection is different then take it and repaint
                if (newMinSelectionIndex != minSelectionIndex || newMaxSelectionIndex != maxSelectionIndex) {
                    minSelectionIndex = newMinSelectionIndex;
                    maxSelectionIndex = newMaxSelectionIndex;
                    featuresComponent.repaint();
                }
            }
        }

        /**
         * Take the range selection
         *
         * @param e the mouse event
         */
        @Override
        public void mouseReleased(MouseEvent e) {
            if (makingRangeSelection) {
                rangeSelectionManager.setRange(featuresModel, minSelectionIndex, maxSelectionIndex);
            }
        }
    }
}

