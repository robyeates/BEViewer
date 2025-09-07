package co.rob.ui.components;

import co.rob.DaggerContext;
import co.rob.ui.CopyableLineProvider;
import co.rob.ui.selection.RangeSelectionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * The <code>FileComponent</code> class provides an uneditable text view of a file's filename.
 * The tooltip text name is the long file path.
 */
public class FileComponent extends JLabel implements CopyableLineProvider {

    private static final String NULL_FILE = "None";
    private static final String NULL_TOOLTIP = null;
    private final RangeSelectionManager rangeSelectionManager;

    /**
     * Creates a file component object.
     */
    public FileComponent() {
        setText(" "); // required during initialization for getPreferredSize()
        setFile(null);
        ToolTipManager.sharedInstance().registerComponent(this);  // tooltip is normally off on JLabel
        setMinimumSize(new Dimension(0, getPreferredSize().height));
        setBackground(UIManager.getColor("List.selectionBackground")); // for when opaque

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                activateSelectionManager();
            }
        });

        //var components = DaggerSelectionManagerComponents.create();
        this.rangeSelectionManager = DaggerContext.get().rangeSelectionManager();

        rangeSelectionManager.addRangeSelectionManagerChangedListener(_ -> {
            if (rangeSelectionManager.getProvider() != FileComponent.this) {
                setOpaque(false);
                repaint();
            }
        });
    }

    /**
     * Sets the file to display.
     *
     * @param file the file to display
     */
    public void setFile(File file) {
        Font baseFont = getFont();
        if (file == null) {
            setEnabled(false);
            setFont(baseFont.deriveFont(Font.ITALIC));
            setText(NULL_FILE);
            setToolTipText(NULL_TOOLTIP);
        } else {
            setEnabled(true);
            setFont(baseFont.deriveFont(Font.PLAIN));
            setText(file.getName());
            setToolTipText(file.getAbsolutePath());
        }
    }

    private void activateSelectionManager() {
        setOpaque(true);
        repaint();
        rangeSelectionManager.setRange(this, 0, 0);
    }

    // CopyableLineInterface
    public String getCopyableLine(int line) {
        return getText();
    }

}

