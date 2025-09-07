package co.rob.ui.components;

import co.rob.util.file.ImageFileFilter;
import co.rob.util.file.ReportFileFilter;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * This class provides a button that displays "...", has a tooltip
 * and responds to selection by activating a file chooser
 * and loading the file chooser's selection to a destination text field.
 */

public class FileChooserButton extends JButton implements ActionListener {

    // read/write file/directory/image
    public static final int READ_FILE = 0;
    public static final int READ_DIRECTORY = 1;
    public static final int WRITE_FILE = 2;
    public static final int WRITE_DIRECTORY = 3;
    public static final int READ_IMAGE_FILE = 4;
    public static final int READ_REPORT_FILE = 5;

    private final Component parent;
    private final String toolTip;
    private final JTextField textField;

    private final int dialogType;
    private final int fileSelectionMode;
    private final FileFilter preferredFileFilter;

    private final JFileChooser chooser = new JFileChooser();

    public FileChooserButton(Component parent, String toolTip, int mode, JTextField destTextField) {
        super("\u2026"); // "…"
        setToolTipText(toolTip);
        addActionListener(this);

        this.parent = parent;
        this.toolTip = toolTip;
        this.textField = destTextField;

        switch (mode) {
            case READ_FILE -> {
                dialogType = JFileChooser.OPEN_DIALOG;
                fileSelectionMode = JFileChooser.FILES_ONLY;
                preferredFileFilter = null;
            }
            case READ_DIRECTORY -> {
                dialogType = JFileChooser.OPEN_DIALOG;
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY;
                preferredFileFilter = null;
            }
            case WRITE_FILE, WRITE_DIRECTORY -> {
                dialogType = JFileChooser.SAVE_DIALOG;
                fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES;
                preferredFileFilter = null;
            }
            case READ_IMAGE_FILE -> {
                dialogType = JFileChooser.OPEN_DIALOG;
                fileSelectionMode = JFileChooser.FILES_ONLY;
                preferredFileFilter = new ImageFileFilter();
            }
            case READ_REPORT_FILE -> {
                dialogType = JFileChooser.OPEN_DIALOG;
                fileSelectionMode = JFileChooser.FILES_ONLY;
                preferredFileFilter = new ReportFileFilter();
            }
            default -> throw new IllegalArgumentException("Invalid mode: " + mode);
        }
    }

    public void actionPerformed(ActionEvent e) {
        chooser.setDialogTitle(toolTip);
        chooser.setDialogType(dialogType);
        chooser.setFileSelectionMode(fileSelectionMode);

        String currentPath = textField.getText();
        if (currentPath != null && !currentPath.isBlank()) {
            chooser.setSelectedFile(new File(currentPath));
        }

        if (preferredFileFilter != null) {
            chooser.setFileFilter(preferredFileFilter);
        }

        // if the user selects APPROVE then take the text
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            // put the text in the text field
            textField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
}

