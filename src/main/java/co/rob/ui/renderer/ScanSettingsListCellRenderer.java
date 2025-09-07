package co.rob.ui.renderer;

import co.rob.pojo.scan.ScanSettings;

import javax.swing.*;
import java.awt.*;

/**
 * Implements ListCellRenderer to list a scan setting in a JList.
 */
public class ScanSettingsListCellRenderer<T extends ScanSettings> extends JLabel implements ListCellRenderer<T> {

    /**
     * Creates a list cell renderer object for rendering in a ListCellRenderer.
     */
    public ScanSettingsListCellRenderer() {
        setOpaque(true);
    }

    public Component getListCellRendererComponent(JList<? extends T> list, T scanSettingObject, int index, boolean isSelected, boolean cellHasFocus) {
        // set the text
        setFont(list.getFont().deriveFont(Font.PLAIN));
        setText(scanSettingObject.getCommandString());

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

