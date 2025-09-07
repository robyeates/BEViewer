package co.rob.ui.dialog.scan;

import co.rob.ui.components.FileChooserButton;
import co.rob.pojo.scan.ScanSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Manage general option values.
 */

public class WScanBoxedGeneral {

    private final Component component;

    private final JCheckBox useBannerFileCB = new JCheckBox("Use Banner File");
    private final JTextField bannerFileTF = new JTextField();
    private final JButton bannerFileChooserB = new FileChooserButton(WScan.getWScanWindow(), "Select Banner File", FileChooserButton.READ_FILE, bannerFileTF);
    private final JCheckBox useAlertListFileCB = new JCheckBox("Use Alert List File");
    private final JTextField alertListFileTF = new JTextField();
    private final JButton alertListFileChooserB = new FileChooserButton(WScan.getWScanWindow(), "Select Alert List File", FileChooserButton.READ_FILE, alertListFileTF);
    private final JCheckBox useStopListFileCB = new JCheckBox("Use Stop List File");
    private final JTextField stopListFileTF = new JTextField();
    private final JButton stopListFileChooserB = new FileChooserButton(WScan.getWScanWindow(), "Select Stop List File", FileChooserButton.READ_FILE, stopListFileTF);
    private final JCheckBox useFindRegexTextFileCB = new JCheckBox("Use Find Regex Text File");
    private final JTextField findRegexTextFileTF = new JTextField();
    private final JButton findRegexTextFileChooserB = new FileChooserButton(WScan.getWScanWindow(), "Select Find Regex Text File", FileChooserButton.READ_FILE, findRegexTextFileTF);
    private final JCheckBox useFindRegexTextCB = new JCheckBox("Use Find Regex Text");
    private final JTextField findRegexTextTF = new JTextField();
    private final JCheckBox useRandomSamplingCB = new JCheckBox("Use Random Sampling");
    private final JTextField randomSamplingTF = new JTextField();

    public WScanBoxedGeneral() {
        component = buildContainer();
        wireActions();
    }

    private Component buildContainer() {
        // container using GridBagLayout with GridBagConstraints
        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createTitledBorder("General Options"));
        container.setLayout(new GridBagLayout());
        int y = 0;
        WScan.addOptionalFileLine(container, y++, useBannerFileCB, bannerFileTF, bannerFileChooserB);
        WScan.addOptionalFileLine(container, y++, useAlertListFileCB, alertListFileTF, alertListFileChooserB);
        WScan.addOptionalFileLine(container, y++, useStopListFileCB, stopListFileTF, stopListFileChooserB);
        WScan.addOptionalFileLine(container, y++, useFindRegexTextFileCB, findRegexTextFileTF, findRegexTextFileChooserB);
        WScan.addOptionalTextLine(container, y++, useFindRegexTextCB, findRegexTextTF, WScan.EXTRA_WIDE_FIELD_WIDTH);
        //noinspection UnusedAssignment
        WScan.addOptionalTextLine(container, y++, useRandomSamplingCB, randomSamplingTF, WScan.WIDE_FIELD_WIDTH);

        // tool tip text
        useRandomSamplingCB.setToolTipText("Random Sampling in form frac[:passes]");

        return container;
    }

    public void setScanSettings(ScanSettings scanSettings) {
        // general options
        useBannerFileCB.setSelected(scanSettings.useBannerFile);
        bannerFileTF.setEnabled(scanSettings.useBannerFile);
        bannerFileTF.setText(scanSettings.bannerFile);
        bannerFileChooserB.setEnabled(scanSettings.useBannerFile);

        useAlertListFileCB.setSelected(scanSettings.useAlertlistFile);
        alertListFileTF.setEnabled(scanSettings.useAlertlistFile);
        alertListFileTF.setText(scanSettings.alertlistFile);
        alertListFileChooserB.setEnabled(scanSettings.useAlertlistFile);

        useStopListFileCB.setSelected(scanSettings.useStoplistFile);
        stopListFileTF.setEnabled(scanSettings.useStoplistFile);
        stopListFileTF.setText(scanSettings.stoplistFile);
        stopListFileChooserB.setEnabled(scanSettings.useStoplistFile);

        useFindRegexTextFileCB.setSelected(scanSettings.useFindRegexTextFile);
        findRegexTextFileTF.setEnabled(scanSettings.useFindRegexTextFile);
        findRegexTextFileTF.setText(scanSettings.findRegexTextFile);
        findRegexTextFileChooserB.setEnabled(scanSettings.useFindRegexTextFile);

        useFindRegexTextCB.setSelected(scanSettings.useFindRegexText);
        findRegexTextTF.setEnabled(scanSettings.useFindRegexText);
        findRegexTextTF.setText(scanSettings.findRegexText);

        useRandomSamplingCB.setSelected(scanSettings.useRandomSampling);
        randomSamplingTF.setEnabled(scanSettings.useRandomSampling);
        randomSamplingTF.setText(scanSettings.randomSampling);
    }

    public void getScanSettings(ScanSettings scanSettings) {
        // general options
        scanSettings.useBannerFile = useBannerFileCB.isSelected();
        scanSettings.bannerFile = bannerFileTF.getText();
        scanSettings.useAlertlistFile = useAlertListFileCB.isSelected();
        scanSettings.alertlistFile = alertListFileTF.getText();
        scanSettings.useStoplistFile = useStopListFileCB.isSelected();
        scanSettings.stoplistFile = stopListFileTF.getText();
        scanSettings.useFindRegexTextFile = useFindRegexTextFileCB.isSelected();
        scanSettings.findRegexTextFile = findRegexTextFileTF.getText();
        scanSettings.useFindRegexText = useFindRegexTextCB.isSelected();
        scanSettings.findRegexText = findRegexTextTF.getText();
        scanSettings.useRandomSampling = useRandomSamplingCB.isSelected();
        scanSettings.randomSampling = randomSamplingTF.getText();
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
        ActionListener getUIValuesActionListener = new GetUIValuesActionListener();

        useBannerFileCB.addActionListener(getUIValuesActionListener);
        useAlertListFileCB.addActionListener(getUIValuesActionListener);
        useStopListFileCB.addActionListener(getUIValuesActionListener);
        useFindRegexTextFileCB.addActionListener(getUIValuesActionListener);
        useFindRegexTextCB.addActionListener(getUIValuesActionListener);
        useRandomSamplingCB.addActionListener(getUIValuesActionListener);
    }
}

