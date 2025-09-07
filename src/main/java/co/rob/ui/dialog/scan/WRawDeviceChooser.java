package co.rob.ui.dialog.scan;

import co.rob.BEViewer;
import co.rob.ui.dialog.WError;
import co.rob.util.I18n;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

/**
 * The dialog window for choosing a raw linux device under /dev
 */

public class WRawDeviceChooser extends JDialog {

    // resources
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final FileFilter fileFilter = new RawDeviceFileFilter();
    private final File DEV_FILE = new File("/dev");

    private static WRawDeviceChooser wRawDeviceChooser;
    private final JList<String> rawDeviceL = new JList<>(listModel);
    private final JButton selectB = new JButton(I18n.textFor("deviceChooser.selectB.label"));
    private final JButton cancelB = new JButton(I18n.textFor("deviceChooser.cancelB.label"));
    private final JButton refreshB = new JButton("deviceChooser.refreshB.label");

    private WRawDeviceChooser() {
        // set parent window, title, and modality
        buildInterface();
        wireActions();
        getRootPane().setDefaultButton(selectB);
        pack();
    }


    public String getSelection() {
        return rawDeviceL.getSelectedValue();
    }

    /**
     * Opens this window.
     */
    public static WRawDeviceChooser openWindow() {
        if (wRawDeviceChooser == null) {
            // this is the first invocation
            // create the window
            wRawDeviceChooser = new WRawDeviceChooser();
        }

        // show the dialog window
        wRawDeviceChooser.setLocation(BEViewer.getBEWindowLocation());
        wRawDeviceChooser.refreshList();
        wRawDeviceChooser.setVisible(true);
        return wRawDeviceChooser;
    }

    private void refreshList() {
        // reload list
        // NOTE: we may wish to replace this with more code in order to provide device size and SN.
        // For SN, exec "hd param -i /dev/sdb", repeating for each sd*.
        // For size, exec or use binary search of reading one byte as is done in RawFileReader.
        listModel.clear();
        File[] files = DEV_FILE.listFiles(fileFilter);
        if (files == null) {
            files = new File[0];
        }
        Arrays.stream(files).forEach(file -> listModel.addElement(file.getAbsolutePath()));

        if (files.length == 0) {
            // warn if no devices
            WError.showMessage(I18n.textFor("deviceChooser.error"), I18n.textFor("deviceChooser.error.dialog.title"));
        }
    }

    private static void closeWindow() {
        // the window is not instantiated in test mode
        if (wRawDeviceChooser != null) {
            WRawDeviceChooser.wRawDeviceChooser.setVisible(false);
        }
    }

    private void buildInterface() {
        setTitle(I18n.textFor("deviceChooser.label"));
        setModal(true);
        setAlwaysOnTop(true);
        Container pane = getContentPane();

        // use GridBagLayout with GridBagConstraints
        GridBagConstraints c;
        pane.setLayout(new GridBagLayout());

        // (0,0) "Raw Devices (/dev)"
        c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 0, 10);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.BOTH;
        pane.add(new JLabel(I18n.textFor("deviceChooser.pane.label")), c);

        // (0,1) scroll pane containing the raw device table
        c = new GridBagConstraints();
        c.insets = new Insets(0, 10, 10, 10);
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;

        // put the raw device list in a scroll pane
        JScrollPane scrollPane = new JScrollPane(rawDeviceL);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        // add the scroll pane containing the raw device list
        pane.add(scrollPane, c);

        // (0,2) add the controls
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.LINE_START;
        pane.add(buildControls(), c);
    }

    private Component buildControls() {
        GridBagConstraints c;
        Container container = new Container();
        container.setLayout(new GridBagLayout());

        // (0,1)selectB
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 1;
        selectB.setEnabled(false);
        container.add(selectB, c);

        // (1,1) cancelB
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 1;
        c.gridy = 1;
        container.add(cancelB, c);

        // (2,1) vertical separator
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 2;
        c.gridy = 1;
        container.add(new JSeparator(SwingConstants.VERTICAL), c);

        // (3,1) refreshB
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 3;
        c.gridy = 1;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        container.add(refreshB, c);

        return container;
    }

    private void wireActions() {
        // rawDeviceL selection change changes selectB enabled state
        rawDeviceL.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectB.setEnabled(rawDeviceL.getSelectedValue() != null);
            }
        });

        // select
        selectB.addActionListener(_ -> setVisible(false));

        // cancel
        cancelB.addActionListener(_ -> {
            rawDeviceL.setSelectedValue(null, false);
            setVisible(false);
        });

        // refresh
        refreshB.addActionListener(_ -> {
            rawDeviceL.setSelectedValue(null, false);
            refreshList();
        });
    }

    private static class RawDeviceFileFilter implements FileFilter {

        /**
         * Whether the given file is accepted by this filter.
         * @param file The file to check
         * @return true if accepted, false if not
         */
        public boolean accept(File file) {
            return (file.getAbsolutePath().startsWith("/dev/sd"));
        }
    }
}

