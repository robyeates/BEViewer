package co.rob.ui.dialog.scan;

import co.rob.io.BulkExtractorScanListReader;
import co.rob.pojo.scan.ScanSettings;
import co.rob.pojo.scan.Scanner;
import co.rob.ui.dialog.WError;
import co.rob.util.I18n;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manage the scanner selections (selected feature recorders).
 */

public class WScanBoxedScanners {

    /**
     * FeatureScanner contains UI and state for one scanner: its command name,
     * CheckBox, default value, and user's selected value.
     */
    private final JPanel container = new JPanel();
    private final WScanBoxedControls wScanBoxedControls;
    private Map<String, JCheckBox> scannerMap;

    public WScanBoxedScanners(WScanBoxedControls wScanBoxedControls) {
        this.wScanBoxedControls = wScanBoxedControls;
        buildContainer();
        wireActions();
    }

    public Component getComponent() {
        return container;
    }

    private void buildContainer() {
        // container using GridBagLayout with GridBagConstraints
        container.setBorder(BorderFactory.createTitledBorder(I18n.textFor("scanners.label")));
        container.setLayout(new GridBagLayout());
        setScannerList();
    }

    private void setScannerList() {
        // get the scanner list from bulk_extractor
        List<Scanner> scanners = getScanners();
        AtomicInteger count = new AtomicInteger(0);
        scannerMap = scanners.stream().map(scanner -> {
            // add a JCheckBox to the container for each scanner
            JCheckBox checkBox = new JCheckBox(scanner.getName());
            // add the checkbox to the container
            WScan.addOptionLine(container, count.incrementAndGet(), checkBox);
            return checkBox;
            // also add the checkbox to the scanner map
            // for future access to its selection value
        }).collect(Collectors.toMap(JCheckBox::getText, Function.identity()));
    }

    @NotNull
    private List<Scanner> getScanners() {
        List<Scanner> scanners;
        try {
            scanners = BulkExtractorScanListReader.readScanList(wScanBoxedControls.isUsePluginDirectory(), wScanBoxedControls.getPluginDirectoriesTextFieldText());
        } catch (IOException e) {
            WError.showError(I18n.textFor("scanners.read.error"),I18n.textFor("scanners.read.error.dialog.title"), e);
            scanners = new ArrayList<>();
        }
        return scanners;
    }

    public void setScanSettings(ScanSettings scanSettings) {
        // iterate over scanSettings.scanners and set the corresponding
        // selection state in the associated JCheckBox
        scanSettings.getScanners().forEach(scanner -> {
            JCheckBox checkbox = scannerMap.get(scanner.getName());
            checkbox.setSelected(scanner.isUseScanner());
        });
    }

    public void getScanSettings(ScanSettings scanSettings) {
        // iterate over scanSettings.scanners and get the selection state
        // from the associated JCheckBox
        scanSettings.getScanners().forEach(scanner -> {
            JCheckBox checkbox = scannerMap.get(scanner.getName());
            scanner.setUseScanner(checkbox.isSelected());
        });
    }

    private void wireActions() {
        // no action
    }
}
 
