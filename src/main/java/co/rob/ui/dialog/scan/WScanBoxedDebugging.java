package co.rob.ui.dialog.scan;

import co.rob.pojo.scan.ScanSettings;
import co.rob.util.I18n;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Manage the debugging values.
 */
public class WScanBoxedDebugging {

    private final Component component;
    private final JCheckBox useStartOnPageNumberCB = new JCheckBox(I18n.textFor("useStartOnPageNumberCB.label"));
    private final JTextField startOnPageNumberTF = new JTextField();
    private final JCheckBox useDebugNumberCB = new JCheckBox(I18n.textFor("useDebugNumberCB.label"));
    private final JTextField debugNumberTF = new JTextField();
    private final JCheckBox useEraseOutputDirectoryCB = new JCheckBox(I18n.textFor("useEraseOutputDirectoryCB.label"));

    public WScanBoxedDebugging() {
        component = buildContainer();
        wireActions();
    }

    private Component buildContainer() {
        // container using GridBagLayout with GridBagConstraints
        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createTitledBorder(I18n.textFor("debugging.label")));
        container.setLayout(new GridBagLayout());
        int y = 0;
        WScan.addOptionalTextLine(container, y++, useStartOnPageNumberCB, startOnPageNumberTF, WScan.WIDE_FIELD_WIDTH);
        WScan.addOptionalTextLine(container, y++, useDebugNumberCB, debugNumberTF, WScan.NARROW_FIELD_WIDTH);
        //noinspection UnusedAssignment
        WScan.addOptionLine(container, y++, useEraseOutputDirectoryCB);
        return container;
    }

    public void setScanSettings(ScanSettings scanSettings) {
        // Debugging
        useStartOnPageNumberCB.setSelected(scanSettings.useStartOnPageNumber);
        startOnPageNumberTF.setEnabled(scanSettings.useStartOnPageNumber);
        startOnPageNumberTF.setText(scanSettings.startOnPageNumber);
        useDebugNumberCB.setSelected(scanSettings.useDebugNumber);
        debugNumberTF.setEnabled(scanSettings.useDebugNumber);
        debugNumberTF.setText(scanSettings.debugNumber);
        useEraseOutputDirectoryCB.setSelected(scanSettings.useEraseOutputDirectory);
    }

    public void getScanSettings(ScanSettings scanSettings) {
        // Debugging
        scanSettings.useStartOnPageNumber = useStartOnPageNumberCB.isSelected();
        scanSettings.startOnPageNumber = startOnPageNumberTF.getText();
        scanSettings.useDebugNumber = useDebugNumberCB.isSelected();
        scanSettings.debugNumber = debugNumberTF.getText();
        scanSettings.useEraseOutputDirectory = useEraseOutputDirectoryCB.isSelected();
    }

    public Component getComponent() {
        return component;
    }

    // the sole purpose of this listener is to keep UI widget visibility up to date
    private class GetUIValuesActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            ScanSettings scanSettings = new ScanSettings();
            getScanSettings(scanSettings);
            setScanSettings(scanSettings);
        }
    }

    private void wireActions() {
        // Debugging
        GetUIValuesActionListener getUIValuesActionListener = new GetUIValuesActionListener();
        useStartOnPageNumberCB.addActionListener(getUIValuesActionListener);
        useDebugNumberCB.addActionListener(getUIValuesActionListener);
        useEraseOutputDirectoryCB.addActionListener(getUIValuesActionListener);
    }
}

