package co.rob.ui.dialog;

import co.rob.BEViewer;
import co.rob.DaggerContext;
import co.rob.api.generated.CancelScanV1Api;
import co.rob.api.generated.GetScanStatusV1Api;
import co.rob.api.generated.invoker.ApiClient;
import co.rob.api.generated.invoker.ApiException;
import co.rob.api.generated.model.ScanStatusResponseV1;
import co.rob.ui.components.FileComponent;
import co.rob.pojo.scan.ScanSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The dialog window for showing progress on a started bulk_extractor scan process.
 * Multiple windows and scans may be active.  This window is not modal.
 */
public class WScanProgress extends JDialog {

    private static final Logger logger = LoggerFactory.getLogger(WScanProgress.class);

    private final FileComponent imageFileLabel = new FileComponent();
    private final FileComponent featureDirectoryLabel = new FileComponent();
    private final JTextField commandField = new JTextField();

    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel statusL = new JLabel();
    private final JTextArea outputArea = new JTextArea();
    private final JButton cancelB = new JButton("Cancel");
    private final JButton closeB = new JButton("Close");

    private final ScanSettings scanSettings;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final String scanId;
    private final ApiClient apiClient;
    private GetScanStatusV1Api getScanStatus;
    private CancelScanV1Api cancelScanV1Api;

    public static void openWindow(ScanSettings scanSettings, String scanId, ApiClient apiClient) {
        SwingUtilities.invokeLater(() -> new WScanProgress(scanSettings, scanId, apiClient));
    }

    // call openWindow to run this privately on the Swing thread
    private WScanProgress(ScanSettings scanSettings, String scanId, ApiClient apiClient) {
        this.scanSettings = scanSettings;
        this.scanId = scanId;
        this.apiClient = apiClient;

        setLocationRelativeTo(BEViewer.getBEWindow());
        setClosePolicy();
        buildInterface();
        getRootPane().setDefaultButton(cancelB);
        wireActions();
        setStartState();
        pack();
        setVisible(true);
    }

    private void setClosePolicy() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                doClose();
            }
        });
    }

    private void setStartState() {
        // populate UI from scanSettings
        imageFileLabel.setFile(new File(scanSettings.inputImage));
        featureDirectoryLabel.setFile(new File(scanSettings.outdir));

        String commandString = scanSettings.getCommandString();
        commandField.setText(commandString);
        commandField.setToolTipText(commandString);
        commandField.setCaretPosition(0);
        getScanStatus = new GetScanStatusV1Api(apiClient);
        cancelScanV1Api = new CancelScanV1Api(apiClient);
        // start polling backend
        scheduledExecutorService.scheduleAtFixedRate(this::pollScanStatus, 0, 2, TimeUnit.SECONDS);
    }

    private void pollScanStatus() {
        try {
            ScanStatusResponseV1 status = getScanStatus.getScanStatusV1(scanId);
            SwingUtilities.invokeLater(() -> updateUI(status));
        } catch (ApiException e) {
            logger.error("Failed to poll scan status for {}", scanId, e);
            scheduledExecutorService.shutdownNow();
            SwingUtilities.invokeLater(() -> {
                statusL.setText("Error contacting backend");
                progressBar.setString("Error");
                closeB.setEnabled(true);
                cancelB.setEnabled(false);
            });
        }
    }

    private void updateUI(ScanStatusResponseV1 status) {
        // Progress %
        if (status.getProgress() != null) {
            int pct = status.getProgress().intValue();
            progressBar.setValue(pct);
            progressBar.setString(pct + "%");
        }

        // Status message
        if (status.getMessage() != null) {
            statusL.setText(status.getMessage());
        }

        outputArea.append(status.getState().name() + "\n");
        outputArea.append("\n");

        switch (status.getState()) {
            case QUEUED, RUNNING -> {
                //noop
            }
            case COMPLETED -> {
                doneSuccess();
            }
            case FAILED -> {
                doneError(status.getMessage());
                break;
            }
            case CANCELED -> {
                doneCanceled();
            }
        }
    }

    private void doneSuccess() {
        scheduledExecutorService.shutdownNow();
        closeB.setEnabled(true);
        cancelB.setEnabled(false);

        statusL.setText("Scan completed. Report " + new File(scanSettings.outdir).getName() + " is ready.");
        progressBar.setValue(100);
        progressBar.setString("Done");

        DaggerContext.get().reportsModel().addReport(new File(scanSettings.outdir), new File(scanSettings.inputImage)); //TODO either this or call
        // TODO GetScanResultsV1Api
    }

    private void doneCanceled() {
        scheduledExecutorService.shutdownNow();
        closeB.setEnabled(true);
        cancelB.setEnabled(false);

        statusL.setText("Scan was canceled");
        progressBar.setString("Canceled");
        WError.showError("Scan canceled.\n" + scanSettings.getCommandString(), "Scan canceled", null);
    }

    private void doneError(String msg) {
        scheduledExecutorService.shutdownNow();
        closeB.setEnabled(true);
        cancelB.setEnabled(false);

        statusL.setText("Error: " + msg);
        progressBar.setString("Error");
        WError.showError("Scan terminated: " + msg + "\n" + scanSettings.getCommandString(), "Scan failed", null);
    }

    private void buildInterface() {
        // set the title to include the image filename
        setTitle("bulk_extractor Scan");

        // use GridBagLayout with GridBagConstraints
        GridBagConstraints c;
        Container pane = getContentPane();
        pane.setLayout(new GridBagLayout());

        // (0,0) File container
        c = new GridBagConstraints();
        c.insets = new Insets(15, 5, 0, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        pane.add(getFileContainer(), c);

        // (0,1) Command field container
        c = new GridBagConstraints();
        c.insets = new Insets(15, 5, 0, 5);
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
//    c.anchor = GridBagConstraints.LINE_START;
        pane.add(getCommandContainer(), c);

        // (0,2) Progress container
        c = new GridBagConstraints();
        c.insets = new Insets(15, 5, 0, 5);
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.LINE_START;
        pane.add(getProgressContainer(), c);

        // (0,3) bulk_extractor output area container
        c = new GridBagConstraints();
        c.insets = new Insets(15, 5, 0, 5);
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        pane.add(getOutputContainer(), c);

        // (0,4) controls
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 4;

        // add the cancel button
        pane.add(buildControls(), c);
    }

    private Component buildControls() {
        GridBagConstraints c;
        Container container = new Container();
        container.setLayout(new GridBagLayout());

        // cancel
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        container.add(cancelB, c);

        // close
        closeB.setEnabled(false);
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 1;
        c.gridy = 0;
//    c.anchor = GridBagConstraints.FIRST_LINE_START;
        container.add(closeB, c);

        return container;
    }

    private void wireActions() {
        // cancelB
        cancelB.addActionListener(_ -> doCancel());
        // closeB
        closeB.addActionListener(_ -> doClose());
    }

    // cancel
    private void doCancel() {
        try {
            cancelScanV1Api.cancelScanV1(scanId);
            logger.info("Requested cancellation of scan {}", scanId);
        } catch (ApiException e) {
            logger.error("Failed to cancel scan {}", scanId, e);
        }
    }

    // close
    private void doClose() {
        scheduledExecutorService.shutdownNow();
        dispose();
    }

    // File container
    private Container getFileContainer() {
        GridBagConstraints c;
        Container container = new Container();
        container.setLayout(new GridBagLayout());

        // (0,0) "Image File"
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 10);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        container.add(new JLabel("Image File"), c);

        // (1,0) <image file>
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        container.add(imageFileLabel, c);

        // (0,1) "Feature Directory"
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 10);
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.LINE_START;
        container.add(new JLabel("Feature Directory"), c);

        // (1,1) <feature directory>
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        container.add(featureDirectoryLabel, c);

        return container;
    }

    // Command container
    private Container getCommandContainer() {
        GridBagConstraints c;
        Container container = new Container();
        container.setLayout(new GridBagLayout());

        // (0,0) "command"
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        container.add(new JLabel("Command"), c);

        // (0,1) command text field
        commandField.setEditable(false);
        commandField.setMinimumSize(new Dimension(0, commandField.getPreferredSize().height));

        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        // add the command field
        container.add(commandField, c);

        return container;
    }

    // Progress container
    private Container getProgressContainer() {
        GridBagConstraints c;
        Container container = new Container();
        container.setLayout(new GridBagLayout());

        // (0,0) "progress"
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 10);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        container.add(new JLabel("Progress"), c);

        // (1,0) progress bar
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        progressBar.setPreferredSize(new Dimension(80, progressBar.getPreferredSize().height));
        progressBar.setMinimumSize(progressBar.getPreferredSize());
        progressBar.setStringPainted(true);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setValue(0);
        progressBar.setString("0%");
        container.add(progressBar, c);

        // (0,1) status text
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        container.add(statusL, c);

        return container;
    }

    // bulk_extractor output container
    private Container getOutputContainer() {
        GridBagConstraints c;
        Container container = new Container();
        container.setLayout(new GridBagLayout());

        // (0,0) "bulk_extractor output"
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        container.add(new JLabel("bulk_extractor output"), c);

        // (0,1) output scroll pane for containing output from bulk_extractor
        outputArea.setEditable(false);
        JScrollPane outputScrollPane = new JScrollPane(outputArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        outputScrollPane.setPreferredSize(new Dimension(600, 200));
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;

        // add the output scroll pane
        container.add(outputScrollPane, c);

        return container;
    }
}

