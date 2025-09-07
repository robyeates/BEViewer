package co.rob.ui.components;

import co.rob.state.ImageView;
import co.rob.ui.selection.RangeSelectionManager;

import javax.swing.*;
import java.awt.*;

/**
 * A display area for the image page view.
 * //TODO Somewhere around here is where we'd want to do theme switching on background
 */
public class ImageComponent extends JComponent implements Scrollable {

  private final ImageView imageView;
  private final RangeSelectionManager rangeSelectionManager;
  private int rowHeight = 0;	// cache

    /**
     * Constructs a new <code>ImageComponent</code> with the given image model.
     * @param imageView the image view model to use
     * @param rangeSelectionManager the selection manager used by this component.
     */
    public ImageComponent(ImageView imageView, RangeSelectionManager rangeSelectionManager) {
        this.imageView = imageView;
        this.rangeSelectionManager = rangeSelectionManager;

        // initialize UI
        updateUI();

        // set background to opaque with List background color
        //setOpaque(true);
        //setBackground(UIManager.getColor("TextPane.background"));
    }

    /**
     * Sets the L&F object that renders this component.
     * @param imageUI  the <code>ImageUI</code> L&F object
     */
    public void setUI(ImageUI imageUI) {
        super.setUI(imageUI);
    }

    /**
     * Notification from the <code>UIFactory</code> that the L&F has changed.
     * Called to replace the UI with the latest version from the
     * <code>UIFactory</code>.
     */
    @Override
    public void updateUI() {
        setUI((ImageUI) UIManager.getUI(this));
        invalidate();
    }

    /**
     * Returns the class ID for the UI.
     * @return the ID ("ImageUI")
     */
    @Override
    public String getUIClassID() {
        return ImageUI.UI_CLASS_ID;
    }

    /**
     * Returns the {@code ImageView} that is providing the data, used by BasicImageUI.
     */
    public ImageView getImageView() {
        return imageView;
    }

    /**
     * Returns the range selection monitor, controlled by BasicImageUI.
     */
    public RangeSelectionManager getRangeSelectionManager() {
        return rangeSelectionManager;
    }

    /**
     * Do not use this method. Font is managed by BasicImageUI and fontSize from ImageView.
     */
    @Override
    public void setFont(Font f) {
        throw new UnsupportedOperationException("This component's font is managed internally.");
    }

    /**
     * Sets the component size. Note that the component will still need to be revalidated and painted.
     */
    public void setComponentSize(Dimension viewDimension) {
        setPreferredSize(viewDimension);
    }

    // Scrollable, adapted from javax.swing.text.JTextArea and JTextComponent

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (rowHeight == 0) {
            rowHeight = getFontMetrics(getBasicImageUI().getImageFont()).getHeight();
        }
        return rowHeight;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return visibleRect.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getParent() instanceof JViewport viewport && viewport.getWidth() > getPreferredSize().width;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport viewport && viewport.getHeight() > getPreferredSize().height;
    }

    private BasicImageUI getBasicImageUI() {
        if (ui instanceof BasicImageUI basicImageUI) {
            return basicImageUI;
        }
        throw new IllegalStateException("The UI delegate is not a BasicImageUI. Found: " + ui.getClass().getName());
    }
}

