package co.rob.ui.dialog.scan;

import co.rob.pojo.scan.ScanSettings;
import co.rob.util.I18n;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Manage Tuning Parameters.
 */

public class WScanBoxedParallelizing {

    private final Component component;

    private final JCheckBox useStartProcessingAtCB = new JCheckBox(I18n.textFor("useStartProcessingAtCB.label"));
    private final JCheckBox useProcessRangeCB = new JCheckBox(I18n.textFor("useProcessRangeCB.label"));
    private final JCheckBox useAddOffsetCB = new JCheckBox(I18n.textFor("useAddOffsetCB.label"));

    private final JTextField startProcessingAtTF = new JTextField();
    private final JTextField processRangeTF = new JTextField();
    private final JTextField addOffsetTF = new JTextField();

    public WScanBoxedParallelizing() {
        component = buildContainer();
        wireActions();
    }

    private Component buildContainer() {
        // container using GridBagLayout with GridBagConstraints
        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createTitledBorder(I18n.textFor("parallel.label")));
        container.setLayout(new GridBagLayout());
        int y = 0;
        WScan.addOptionalTextLine(container, y++, useStartProcessingAtCB, startProcessingAtTF, WScan.NARROW_FIELD_WIDTH);
        WScan.addOptionalTextLine(container, y++, useProcessRangeCB, processRangeTF, WScan.WIDE_FIELD_WIDTH);
        //noinspection UnusedAssignment
        WScan.addOptionalTextLine(container, y++, useAddOffsetCB, addOffsetTF, WScan.NARROW_FIELD_WIDTH);

        // tooltip help
        useStartProcessingAtCB.setToolTipText(I18n.textFor("useStartProcessingAtCB.tooltip"));
        useProcessRangeCB.setToolTipText(I18n.textFor("useProcessRangeCB.tooltip"));
        useAddOffsetCB.setToolTipText(I18n.textFor("useAddOffsetCB.tooltip"));

        return container;
    }

    public void setScanSettings(ScanSettings scanSettings) {
        // tuning parameters
        useStartProcessingAtCB.setSelected(scanSettings.useStartProcessingAt);
        useProcessRangeCB.setSelected(scanSettings.useProcessRange);
        useAddOffsetCB.setSelected(scanSettings.useAddOffset);

        startProcessingAtTF.setEnabled(scanSettings.useStartProcessingAt);
        processRangeTF.setEnabled(scanSettings.useProcessRange);
        addOffsetTF.setEnabled(scanSettings.useAddOffset);

        startProcessingAtTF.setText(scanSettings.startProcessingAt);
        processRangeTF.setText(scanSettings.processRange);
        addOffsetTF.setText(scanSettings.addOffset);
    }

    public void getScanSettings(ScanSettings scanSettings) {
        // tuning parameters
        scanSettings.useStartProcessingAt = useStartProcessingAtCB.isSelected();
        scanSettings.useProcessRange = useProcessRangeCB.isSelected();
        scanSettings.useAddOffset = useAddOffsetCB.isSelected();

        scanSettings.startProcessingAt = startProcessingAtTF.getText();
        scanSettings.processRange = processRangeTF.getText();
        scanSettings.addOffset = addOffsetTF.getText();
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
        final ActionListener getUIValuesActionListener = new GetUIValuesActionListener();

        // tuning parameters
        // currently, bulk_extractor binds min and max together
        useStartProcessingAtCB.addActionListener(_ -> {
            if (useStartProcessingAtCB.isSelected()) {
                useProcessRangeCB.setSelected(false);
                getUIValuesActionListener.actionPerformed(null);
            }
        });
        // currently, bulk_extractor binds min and max together
        useProcessRangeCB.addActionListener(_ -> {
            if (useProcessRangeCB.isSelected()) {
                useStartProcessingAtCB.setSelected(false);
                getUIValuesActionListener.actionPerformed(null);
            }
        });
        useAddOffsetCB.addActionListener(getUIValuesActionListener);
    }
}

