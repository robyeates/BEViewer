package co.rob.ui.dialog;

import co.rob.BEViewer;
import co.rob.DaggerContext;
import co.rob.io.WorkSettingsWriter;
import co.rob.state.BookmarksModel;
import co.rob.state.ReportsModel;
import co.rob.ui.components.FileChooserButton;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.Serial;

/**
 * The dialog window for exporting work settings.
 */

public class WExportWorkSettings extends JDialog {
    @Serial
    private static final long serialVersionUID = 1;

    private static WExportWorkSettings wExportWorkSettings;

    private static WorkSettingsWriter workSettingsWriter;
    private static BookmarksModel bookmarksModel;
    private static ReportsModel reportsModel;
    private File workSettingsFile = null;
    private final JTextField filenameTF = new JTextField();
    private final JButton directoryChooserB = new FileChooserButton(BEViewer.getBEWindow(), "Export Work Settings to", FileChooserButton.WRITE_FILE, filenameTF);
    private final JButton exportB = new JButton("Export");
    private final JButton cancelB = new JButton("Cancel");

    /**
     * Opens this window.
     */
    public static void openWindow() {
        if (wExportWorkSettings == null) {
            // this is the first invocation
            // create the window
            wExportWorkSettings = new WExportWorkSettings();
        }
        workSettingsWriter = DaggerContext.get().workSettingsWriter();
        bookmarksModel = DaggerContext.get().bookmarksModel();
        reportsModel = DaggerContext.get().reportsModel();

        // show the dialog window
        wExportWorkSettings.setLocationRelativeTo(BEViewer.getBEWindow());
        wExportWorkSettings.setVisible(true);
    }

    private WExportWorkSettings() {
        // set parent window, title, and modality

        buildInterface();
        wireActions();
        getRootPane().setDefaultButton(exportB);
        pack();
    }

    private void buildInterface() {
        setTitle("Export Work Settings");
        setModal(true);
        setAlwaysOnTop(true);
        Container pane = getContentPane();

        // use GridBagLayout with GridBagConstraints
        GridBagConstraints c;
        pane.setLayout(new GridBagLayout());

        // (0,2) add the filename and directory input
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.LINE_START;
        pane.add(buildFilePath(), c);

        // (0,3) add the controls
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 3;
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
        filenameTF.setToolTipText("Export opened Report and Bookmark settings to this file");
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
        container.add(directoryChooserB, c);

        return container;
    }

    private Component buildControls() {
        GridBagConstraints c;
        Container container = new Container();
        container.setLayout(new GridBagLayout());

        // (0,0) exportB
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        container.add(exportB, c);

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

        // clicking directoryChooserB listener opens file chooser
        directoryChooserB.addActionListener(_ -> {
            // create chooser
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Export Work Settings As");
            chooser.setDialogType(JFileChooser.SAVE_DIALOG);
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

            // if the user selects APPROVE then take the text
            if (chooser.showOpenDialog(wExportWorkSettings) == JFileChooser.APPROVE_OPTION) {

                // put the text in the text field
                String filenameString = chooser.getSelectedFile().getAbsolutePath();
                filenameTF.setText(filenameString);
            }
        });

        // clicking exportB exports the work settings
        exportB.addActionListener(_ -> {
            workSettingsFile = new File(filenameTF.getText());

            boolean isSuccessful = workSettingsWriter.exportWorkSettings(workSettingsFile, reportsModel.elements(), bookmarksModel);
            if (isSuccessful) {
                setVisible(false);
            }
        });

        // clicking cancelB closes this window
        cancelB.addActionListener(_ -> setVisible(false));
    }

}

