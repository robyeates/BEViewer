package co.rob.ui.dialog;

import co.rob.BEViewer;
import co.rob.DaggerContext;
import co.rob.io.WorkSettingsReader;
import co.rob.pojo.FeatureLine;
import co.rob.pojo.WorkSettings;
import co.rob.state.BookmarksModel;
import co.rob.state.ReportsModel;
import co.rob.ui.components.FileChooserButton;
import co.rob.util.WorkSettingsFileReadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The dialog window for importing opened Reports and Bookmarks.
 */

public class WImportWorkSettings extends JDialog {

    private static final Logger logger = LoggerFactory.getLogger(WImportWorkSettings.class);

    private static WImportWorkSettings wImportWorkSettings;

    private static WorkSettingsReader workSettingsReader;
    private static BookmarksModel bookmarksModel;
    private static ReportsModel reportsModel;
    private static Consumer<Void> closeAllReportsCallback;
    private final JTextField filenameTF = new JTextField();
    private final JButton fileChooserB = new FileChooserButton(BEViewer.getBEWindow(), "Import Work Settings From", FileChooserButton.READ_FILE, filenameTF);
    private final JCheckBox keepExistingWorkSettingsCB = new JCheckBox("Keep Existing Work Settings");
    private final JButton importB = new JButton("Import");
    private final JButton cancelB = new JButton("Cancel");

    /**
     * Opens this window.
     */
    public static void openWindow(Consumer<Void> closeAllCallback) {
        if (wImportWorkSettings == null) {
            // this is the first invocation
            // create the window
            wImportWorkSettings = new WImportWorkSettings();
        }
        workSettingsReader = DaggerContext.get().workSettingsReader();
        bookmarksModel = DaggerContext.get().bookmarksModel();
        reportsModel = DaggerContext.get().reportsModel();

        // show the dialog window
        wImportWorkSettings.setLocationRelativeTo(BEViewer.getBEWindow());
        wImportWorkSettings.setVisible(true);
        closeAllReportsCallback = closeAllCallback;
    }

    private WImportWorkSettings() {
        // set parent window, title, and modality

        buildInterface();
        wireActions();
        getRootPane().setDefaultButton(importB);
        pack();
    }

    private void buildInterface() {
        setTitle("Import Work Settings");
        setModal(true);
        setAlwaysOnTop(true);
        Container pane = getContentPane();

        // use GridBagLayout with GridBagConstraints
        GridBagConstraints c;
        pane.setLayout(new GridBagLayout());

        // (0,0) add the filename input
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        pane.add(buildFilePath(), c);

        // (0,1) add option control
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.LINE_START;
        pane.add(buildOptionControl(), c);

        // (0,2) add the controls
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.LINE_START;
        pane.add(buildControls(), c);
    }

    private Component buildFilePath() {
        GridBagConstraints c;
        Container container = new Container();
        container.setLayout(new GridBagLayout());

        // filename label (0,0)
        c = new GridBagConstraints();
        c.insets = new Insets(0, 5, 0, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        container.add(new JLabel("Work Settings File"), c);

        // file filenameTF (1,0)
        filenameTF.setMinimumSize(new Dimension(250, filenameTF.getPreferredSize().height));
        filenameTF.setPreferredSize(new Dimension(250, filenameTF.getPreferredSize().height));
        filenameTF.setToolTipText("Import opened Report and Bookmark settings from this file");
        c = new GridBagConstraints();
        c.insets = new Insets(0, 5, 0, 5);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.anchor = GridBagConstraints.LINE_START;
        container.add(filenameTF, c);

        // file chooser (2,0)
        c = new GridBagConstraints();
        c.insets = new Insets(0, 5, 0, 5);
        c.gridx = 2;
        c.gridy = 0;
        container.add(fileChooserB, c);

        return container;
    }

    private Component buildOptionControl() {
        GridBagConstraints c;
        Container container = new Container();
        container.setLayout(new GridBagLayout());

        // (0,0) keep existing settings checkbox
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        keepExistingWorkSettingsCB.setToolTipText("Do not clear opened Reports and Bookmarks that are already active");
        keepExistingWorkSettingsCB.setSelected(true);
        container.add(keepExistingWorkSettingsCB, c);

        return container;
    }

    private Component buildControls() {
        GridBagConstraints c;
        Container container = new Container();
        container.setLayout(new GridBagLayout());

        // (0,0) importB
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        container.add(importB, c);

        // (1,0) cancelB
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        container.add(cancelB, c);

        return container;
    }

    private void wireActions() {

        // clicking importB imports the opened reports and the bookmarks settings
        importB.addActionListener(_ -> {
            File workSettingsFile = new File(filenameTF.getText());
            boolean keepExistingWorkSettings = keepExistingWorkSettingsCB.isSelected();
            WorkSettings workSettings;
            try {
                Optional<WorkSettings> optionalWorkSettings = workSettingsReader.importWorkSettings(workSettingsFile, keepExistingWorkSettings, closeAllReportsCallback);
                workSettings = optionalWorkSettings.orElseThrow(() -> new WorkSettingsFileReadException("Empty File Read"));
            } catch (WorkSettingsFileReadException e) {
                logger.error("Unable to restore work settings feature", e);
                return;
            }

            if (loadReportSettings(workSettings) && loadBookmarks(workSettings)) {
                setVisible(false);
            }
        });

        // clicking cancelB closes this window
        cancelB.addActionListener(_ -> setVisible(false));
    }

    private boolean loadBookmarks(WorkSettings workSettings) {
        // load the bookmark into the model
        for (FeatureLine featureLine : workSettings.getFeatureLines()) {
            try {
                bookmarksModel.addElement(featureLine);
            } catch (Exception e) {
                logger.error("""
                        Unable to restore work settings feature:\
                        Report Image file: {}\
                        Feature file: {}\
                        Feature index: {}""", featureLine.reportImageFile(), featureLine.featuresFile(), featureLine.startByte(), e);
                WError.showError("Unable to restore work settings feature:"
                                + "\nReport Image file: " + featureLine.reportImageFile()
                                + "\nFeature file: " + featureLine.featuresFile()
                                + "\nFeature index: " + featureLine.startByte(),
                        "BEViewer Preferences Error", e);
                return false;
            }
        }
        logger.info("Added [{}] FeatureLine elements to Bookmarks", workSettings.getFeatureLines().size());
        return true;
    }

    private boolean loadReportSettings(WorkSettings workSettings) {
        // load the report into the model
        workSettings.getReportSettings().forEach(reportSettings ->
                reportsModel.addReport(reportSettings.reportFeaturesDirectory(), reportSettings.reportImageFile()));
        logger.info("Added [{}] Reports", workSettings.getReportSettings().size());
        return true;
    }
}

