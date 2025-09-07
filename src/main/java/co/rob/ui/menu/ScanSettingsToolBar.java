package co.rob.ui.menu;

import co.rob.consumer.ScanSettingsConsumer;
import co.rob.pojo.scan.ScanSettings;
import co.rob.state.ScanSettingsListModel;
import co.rob.ui.dialog.scan.WScan;
import co.rob.ui.icons.BEIcons;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;

import static co.rob.util.BEConstants.BUTTON_SIZE;

/**
 * The toolbar for the run queue scheduler.
 */

public class ScanSettingsToolBar extends JToolBar {

    // the list that this toolbar works with
    private final JList<ScanSettings> runQueueL;
    private final ScanSettingsListModel scanSettingsListModel;

    // the toolbar's buttons and checkbox
    private final JButton runB;
    private final JButton deleteB;
    private final JButton editRedoB;
    private final JButton upB;
    private final JButton downB;
    private final JCheckBox pauseCB;

    public ScanSettingsToolBar(JList<ScanSettings> runQueueL, ScanSettingsListModel scanSettingsListModel) {
        super("Scan Settings Toolbar", JToolBar.HORIZONTAL);
        setFloatable(false);
        this.runQueueL = runQueueL;
        this.scanSettingsListModel = scanSettingsListModel;

        // run button
        runB = new FormedJButton(BEIcons.RUN_BULK_EXTRACTOR_24_LIGHT, "Generate a new report");
        this.add(runB);

        // delete button
        deleteB = new FormedJButton(BEIcons.DELETE_24_LIGHT, "Delete selection from queue");
        this.add(deleteB);

        // editRedo button
        editRedoB = new FormedJButton(BEIcons.EDIT_24_LIGHT, "Edit selection");
        this.add(editRedoB);

        // up button
        upB = new FormedJButton(BEIcons.UP_24_LIGHT, "Move selection up queue to be run sooner");
        this.add(upB);

        // down button
        downB = new FormedJButton(BEIcons.DOWN_24_LIGHT, "Move selection down queue to be run later");
        this.add(downB);

        // separator
        addSeparator(new Dimension(20, 0));

        // pause checkbox
        pauseCB = new JCheckBox("Pause");
        pauseCB.setToolTipText("Delay starting the next bulk_extractor run");

        pauseCB.setFocusable(false);
        pauseCB.setOpaque(false); // this looks better with the ToolBar's gradient
        pauseCB.setRequestFocusEnabled(false);

        this.add(pauseCB);

        // set enabled states
        setEnabledStates();

        // wire listeners
        wireListeners();
    }

    private void setEnabledStates() {

        // set states for buttons
        deleteB.setEnabled(runQueueL.getSelectedIndex() >= 0);
        editRedoB.setEnabled(runQueueL.getSelectedIndex() >= 0);
        upB.setEnabled(runQueueL.getSelectedIndex() > 0);
        downB.setEnabled(runQueueL.getSelectedIndex() != -1 && runQueueL.getSelectedIndex() < scanSettingsListModel.getSize() - 1);
    }

    private void wireListeners() {
        // on JList selection change, set button states
        runQueueL.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                setEnabledStates();
            }
        });

        // on JList data change, change button states and, for add, change the list selection
        scanSettingsListModel.addListDataListener(new ListDataListener() {

            // contentsChanged
            public void contentsChanged(ListDataEvent e) {
                setEnabledStates();
            }

            // intervalAdded
            public void intervalAdded(ListDataEvent e) {
                setEnabledStates();
            }

            // intervalRemoved
            public void intervalRemoved(ListDataEvent e) {
                setEnabledStates();
            }
        });

        // clicking runB starts to define a new job
        runB.addActionListener(_ -> WScan.openWindow(new ScanSettings()));

        // clicking deleteB deletes the scan settings job
        deleteB.addActionListener(_ -> {
            ScanSettings scanSettings = runQueueL.getSelectedValue();
            scanSettingsListModel.remove(scanSettings);
        });

        // pause or un-pause the consumer
        pauseCB.addActionListener(_ -> ScanSettingsConsumer.pauseConsumer(pauseCB.isSelected()));

        // clicking editRedoB
        editRedoB.addActionListener(_ -> {
            ScanSettings scanSettings = runQueueL.getSelectedValue();
            scanSettingsListModel.remove(scanSettings);
            WScan.openWindow(scanSettings);
        });

        // clicking upB moves job up
        upB.addActionListener(_ -> {
            ScanSettings scanSettings = runQueueL.getSelectedValue();
            scanSettingsListModel.moveUp(scanSettings);
        });

        // clicking downB moves job down
        downB.addActionListener(_ -> {
            ScanSettings scanSettings = runQueueL.getSelectedValue();
            scanSettingsListModel.moveDown(scanSettings);
        });
    }

    private static class FormedJButton extends JButton {
        FormedJButton(Icon icon, String tooltip) {
            setIcon(icon);
            setToolTipText(tooltip);
            setMinimumSize(BUTTON_SIZE);
            setPreferredSize(BUTTON_SIZE);
            setFocusable(false);
            setOpaque(false);
            setBorderPainted(false);
        }
    }
}

