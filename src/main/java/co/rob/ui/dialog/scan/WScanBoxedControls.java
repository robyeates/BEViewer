package co.rob.ui.dialog.scan;

import co.rob.ui.components.FileChooserButton;
import co.rob.pojo.scan.ScanSettings;
import co.rob.util.I18n;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static co.rob.ui.components.FileChooserButton.READ_DIRECTORY;


/**
 * Manage Scanner controls.
 */

public class WScanBoxedControls {

    private final Component component;

    private static final JCheckBox usePluginDirectoriesCB = new JCheckBox(I18n.textFor("usePluginDirectoriesCB.label"));
    private static final JTextField pluginDirectoriesTF = new JTextField();
    private final FileChooserButton pluginDirectoriesChooserB = new FileChooserButton(WScan.getWScanWindow(),
            I18n.textFor("pluginDirectoriesChooserB.tooltip"), READ_DIRECTORY, pluginDirectoriesTF);

    private final JCheckBox useSettableOptionsCB = new JCheckBox(I18n.textFor("useSettableOptionsCB.label"));
    private final JTextField settableOptionsTF = new JTextField();

    public WScanBoxedControls() {
        component = buildContainer();
        wireActions();
    }

    public static boolean isUsePluginDirectory() {
        return usePluginDirectoriesCB.isSelected();
    }

    public static String getPluginDirectoriesTextFieldText() {
        return pluginDirectoriesTF.getText();
    }

    private Component buildContainer() {
        // container using GridBagLayout with GridBagConstraints
        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createTitledBorder(I18n.textFor("controls.label")));
        container.setLayout(new GridBagLayout());
        int y = 0;
        WScan.addOptionalFileLine(container, y++, usePluginDirectoriesCB, pluginDirectoriesTF, pluginDirectoriesChooserB);
        WScan.addOptionalTextLine(container, y++, useSettableOptionsCB, settableOptionsTF, WScan.EXTRA_WIDE_FIELD_WIDTH);

        // tool tip text
        usePluginDirectoriesCB.setToolTipText(I18n.textFor("usePluginDirectoriesCB.tooltip"));
        useSettableOptionsCB.setToolTipText(I18n.textFor("useSettableOptionsCB.tooltip"));

        return container;
    }

    public void setScanSettings(ScanSettings scanSettings) {
        // controls
        usePluginDirectoriesCB.setSelected(scanSettings.usePluginDirectories);
        pluginDirectoriesTF.setEnabled(scanSettings.usePluginDirectories);
        pluginDirectoriesTF.setText(scanSettings.pluginDirectories);
        pluginDirectoriesChooserB.setEnabled(scanSettings.usePluginDirectories);

        useSettableOptionsCB.setSelected(scanSettings.useSettableOptions);
        settableOptionsTF.setEnabled(scanSettings.useSettableOptions);
        settableOptionsTF.setText(scanSettings.settableOptions);
    }

    public void getScanSettings(ScanSettings scanSettings) {
        // controls
        scanSettings.usePluginDirectories = usePluginDirectoriesCB.isSelected();
        scanSettings.pluginDirectories = pluginDirectoriesTF.getText();

        scanSettings.useSettableOptions = useSettableOptionsCB.isSelected();
        scanSettings.settableOptions = settableOptionsTF.getText();
    }

    public Component getComponent() {
        return component;
    }

    // this listener keeps UI widget visibility up to date
    private class GetUIValuesActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            ScanSettings scanSettings = new ScanSettings();
            getScanSettings(scanSettings);
            setScanSettings(scanSettings);
        }
    }

    private void wireActions() {
        // controls
        GetUIValuesActionListener getUIValuesActionListener = new GetUIValuesActionListener();
        usePluginDirectoriesCB.addActionListener(getUIValuesActionListener);
        useSettableOptionsCB.addActionListener(getUIValuesActionListener);
    }
}

