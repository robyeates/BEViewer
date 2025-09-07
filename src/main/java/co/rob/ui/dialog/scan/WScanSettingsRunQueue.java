package co.rob.ui.dialog.scan;

import co.rob.BEViewer;
import co.rob.DaggerContext;
import co.rob.pojo.scan.ScanSettings;
import co.rob.state.ScanSettingsListModel;
import co.rob.ui.menu.ScanSettingsToolBar;
import co.rob.ui.renderer.ScanSettingsListCellRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The dialog window for managing the bulk_extractor run queue scheduler
 */
public class WScanSettingsRunQueue extends JDialog {

    private static ScanSettingsListModel scanSettingsListModel;

    private static WScanSettingsRunQueue wScanSettingsRunQueue;
    private final ScanSettingsToolBar toolbar;
    private final JButton closeB;

    /**
     * Opens this window.
     */
    public static void openWindow() {
        if (wScanSettingsRunQueue == null) {
            // this is the first invocation
            // create the window
            wScanSettingsRunQueue = new WScanSettingsRunQueue();
        }
        scanSettingsListModel = DaggerContext.get().scanSettingsListModel();
        // show the dialog window
        wScanSettingsRunQueue.setLocation(BEViewer.getBEWindowLocation());
        wScanSettingsRunQueue.setVisible(true);
    }

    private static void closeWindow() {
        WScanSettingsRunQueue.wScanSettingsRunQueue.setVisible(false);
    }

    private WScanSettingsRunQueue() {
        // set parent window, title, and modality
        JList<ScanSettings> runQueueL = new JList<>(scanSettingsListModel);

        toolbar = new ScanSettingsToolBar(runQueueL, scanSettingsListModel); //shouldn't be, check OG code
        closeB = new JButton("Close");

        runQueueL.setSelectionModel(scanSettingsListModel.getSelectionModel());
        runQueueL.setCellRenderer(new ScanSettingsListCellRenderer<>());

        buildInterface(runQueueL);
        wireActions();
        getRootPane().setDefaultButton(closeB);
        pack();
    }

    private void buildInterface(JList<ScanSettings> runQueueL) {
        setTitle("bulk_extractor Run Queue");
        Container pane = getContentPane();

        // use GridBagLayout with GridBagConstraints
        GridBagConstraints c;
        pane.setLayout(new GridBagLayout());

        int y = 0;
        // toolbar
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.BOTH;
        pane.add(toolbar, c);

        // "Run Queue" label
        y++;
        c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 0, 10);
        c.gridx = 0;
        c.gridy = y;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.BOTH;
        pane.add(new JLabel("Run Queue"), c);

        // scroll pane containing the Scan Settings jobs
        y++;
        c = new GridBagConstraints();
        c.insets = new Insets(0, 10, 10, 10);
        c.gridx = 0;
        c.gridy = y;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;

        // put the Scan Settings job list in the scroll pane
        JScrollPane scrollPane = new JScrollPane(runQueueL);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        // add the scroll pane containing the Scan Settings job list
        pane.add(scrollPane, c);

        // add controls
        y++;
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = y;
        pane.add(buildControls(), c);
    }

    private Component buildControls() {
        GridBagConstraints c;
        Container container = new Container();
        container.setLayout(new GridBagLayout());

        int x = 0;
        int y = 0;

        // closeB
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = x++;
        c.gridy = y;
        container.add(closeB, c);

        return container;
    }

    private void wireActions() {

        // clicking closeB closes this window
        closeB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
    }
}

