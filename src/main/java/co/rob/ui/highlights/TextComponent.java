package co.rob.ui.highlights;

import co.rob.DaggerContext;
import co.rob.ui.CopyableLineProvider;
import co.rob.ui.selection.RangeSelectionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;

/**
 * The <code>TextComponent</code> class provides an uneditable text view of a text string.
 * //TODO fix the first commit where the use of this was lost
 * //TODO is this in  the right package?
 */
public class TextComponent extends JLabel implements CopyableLineProvider {
    private static final String NULL_TEXT = "None";
    private final RangeSelectionManager rangeSelectionManager;

    /**
     * Creates a text component object.
     */
    public TextComponent() {
        setText(" "); // required during initialization for getPreferredSize()
        setComponentText(null);
        setMinimumSize(new Dimension(0, getPreferredSize().height));

        //TODO set high alpha here
        setBackground(UIManager.getColor("List.selectionBackground")); // for when opaque
        rangeSelectionManager = DaggerContext.get().rangeSelectionManager();//selectionManagerComponents.rangeSelectionManager();
        // react to mouse clicks
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                activateSelection();
            }
        });
        // clear highlight if another provider took over
        PropertyChangeListener rangeSelectionListener = evt -> {
            if (rangeSelectionManager.getProvider() != this) {
                // clear highlight if another provider took over - ?!?!
                if (evt.getNewValue() != TextComponent.this) {
                    setOpaque(false);
                    repaint();
                }
            }
        };

        rangeSelectionManager.addRangeSelectionManagerChangedListener(rangeSelectionListener);
    }

    /**
     * Sets the text to display.
     */
    public void setComponentText(String text) {
        Font font = getFont();
        if (text == null || text.isEmpty()) {
            setEnabled(false);
            setFont(font.deriveFont(Font.ITALIC));
            setText(NULL_TEXT);
        } else {
            setEnabled(true);
            setFont(font.deriveFont(Font.PLAIN));
            setText(text);
        }
    }

    /**
     * Engage this component as the current range provider.
     */
    private void activateSelection() {
        setOpaque(true);
        repaint();
        rangeSelectionManager.setRange(this, 0, 0);
    }

    // CopyableLineInterface
    public String getCopyableLine(int line) {
        return getText();
    }

}

