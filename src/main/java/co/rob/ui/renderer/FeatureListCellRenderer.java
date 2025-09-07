package co.rob.ui.renderer;

import co.rob.pojo.FeatureLine;
import co.rob.util.PathFormat;
import co.rob.util.UserTextFormatSettings;

import javax.swing.*;
import java.awt.*;

import static co.rob.io.features.FeatureLineUtils.getSummaryString;

/**
 * The <code>FeatureListCellRenderer</code> class provides a ListCellRenderer interface
 * for FeatureLine objects, specifically,
 * the FeatureLine Selection ComboBox for NavigationPane
 * and the FeatureLine list for WBookmarks.
 */
public class FeatureListCellRenderer<T extends FeatureLine> extends JLabel implements ListCellRenderer<T> {
    /**
     * Creates a list cell renderer object for rendering feature lines in a ListCellRenderer.
     */
    public FeatureListCellRenderer() {
        setOpaque(true);
    }

    /**
     * Returns a Component containing the rendered feature selection text
     * derived from the FeatureLine.
     */
    public Component getListCellRendererComponent(
            JList list, // the list
            FeatureLine featureLine, // value to display
            int index,               // cell index
            boolean isSelected,      // is the cell selected
            boolean cellHasFocus) {  // does the cell have focus

        // set the text
        Font font = list.getFont();
        if (featureLine == null) {
            // this occurs when the list is empty.
            setEnabled(false);
            setFont(font.deriveFont(Font.ITALIC));
            setText("None");
        } else {
            // featureLine is available
            setEnabled(true);
            setFont(font.deriveFont(Font.PLAIN));
            setText(getSummaryString(featureLine, UserTextFormatSettings.getPathFormat() == PathFormat.HEX_FORMAT));
        }

        // set color
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        return this;
    }
}

