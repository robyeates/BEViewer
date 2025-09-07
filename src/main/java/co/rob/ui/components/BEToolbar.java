package co.rob.ui.components;

import co.rob.DaggerContext;
import co.rob.pojo.FeatureLine;
import co.rob.pojo.scan.ScanSettings;
import co.rob.state.BookmarksModel;
import co.rob.state.UserHighlightModel;
import co.rob.ui.dialog.WManageBookmarks;
import co.rob.ui.dialog.WOpen;
import co.rob.ui.dialog.scan.WScan;
import co.rob.ui.highlights.SimpleColorPicker;
import co.rob.ui.highlights.preview.SelectedColorPreview;
import co.rob.ui.icons.BEIcons;
import co.rob.ui.selection.FeatureLineSelectionManager;
import co.rob.ui.selection.HighlightColorSelectionModel;
import co.rob.ui.selection.RangeSelectionManager;
import co.rob.util.GenericPropertyChangeListener;
import co.rob.util.UTF8Tools;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeSupport;

import static co.rob.io.features.FeatureLineUtils.isBlank;
import static co.rob.ui.icons.BEIcons.THEME_PICKER_DARK_MODE;
import static co.rob.ui.icons.BEIcons.THEME_PICKER_LIGHT_MODE;
import static co.rob.util.BEConstants.GUI_X_PADDING;

public class BEToolbar extends JToolBar {

    private static boolean darkMode = false;

    // close
    private final JButton closeToolbarButtonLight = new JButton(BEIcons.CLOSE_16_LIGHT);
    private final JButton closeToolbarButtonDark = new JButton(BEIcons.CLOSE_16_DARK);

    // shortcut button controls
    private final JButton openReportButtonLight = new JButton(BEIcons.OPEN_REPORT_16_LIGHT);
    private final JButton openReportButtonDark = new JButton(BEIcons.OPEN_REPORT_16_DARK);

    private final JButton runButtonLight = new JButton(BEIcons.RUN_BULK_EXTRACTOR_16_LIGHT);
    private final JButton runButtonDark = new JButton(BEIcons.RUN_BULK_EXTRACTOR_16_DARK);

    private final JButton copyButtonLight = new JButton(BEIcons.COPY_16_LIGHT);
    private final JButton copyButtonDark = new JButton(BEIcons.COPY_16_DARK);

    private final JButton printButtonLight = new JButton(BEIcons.PRINT_FEATURE_16_LIGHT);
    private final JButton printButtonDark = new JButton(BEIcons.PRINT_FEATURE_16_DARK);

    private final JButton addBookmarkButtonLight = new JButton(BEIcons.ADD_BOOKMARK_16_LIGHT);
    private final JButton addBookmarkButtonDark = new JButton(BEIcons.ADD_BOOKMARK_16_DARK);

    private final JButton manageBookmarksButtonLight = new JButton(BEIcons.MANAGE_BOOKMARKS_16_LIGHT);
    private final JButton manageBookmarksButtonDark = new JButton(BEIcons.MANAGE_BOOKMARKS_16_DARK);

    private final JButton themePickerButton = new JButton(THEME_PICKER_LIGHT_MODE);

    // highlight
    private final JTextField highlightTF = new JTextField();
    private final JCheckBox matchCaseCB = new JCheckBox("Match case");

    private final BookmarksModel bookmarksModel;
    private final FeatureLineSelectionManager featureLineSelectionManager;
    private final RangeSelectionManager rangeSelectionManager;
    private final UserHighlightModel userHighlightModel;
    private final HighlightColorSelectionModel highlightColorSelectionModel;
    private final Runnable lookAndFeelCallback;

    private Color defaultHighlightTFBackgroundColor = null;

    // resources
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    //private final ModelChangedNotifier<Object> toolbarChangedNotifier = new ModelChangedNotifier<Object>();

    /**
     * This notifies when visibility changes.
     */
    public void setVisible(boolean visible) {
        var old = this.isVisible();
        super.setVisible(visible);
        propertyChangeSupport.firePropertyChange("ToolbarVisible", old, visible);
    }

    // this simply gets this interface to appear in javadocs
    public boolean isVisible() {
        return super.isVisible();
    }

    public BEToolbar(Runnable lookAndFeelCallback) {
        super("BEViewer Toolbar", JToolBar.HORIZONTAL);
        this.lookAndFeelCallback = lookAndFeelCallback;
        setFloatable(false); // disabled because it looks bad

        bookmarksModel = DaggerContext.get().bookmarksModel();
        featureLineSelectionManager = DaggerContext.get().featureLineSelectionManager();
        rangeSelectionManager = DaggerContext.get().rangeSelectionManager();
        highlightColorSelectionModel = DaggerContext.get().highlightColorSelectionModel();

        userHighlightModel = DaggerContext.get().userHighlightModel();


        // close Toolbar button
        closeToolbarButtonLight.setFocusable(false);
        closeToolbarButtonLight.setOpaque(false);
        closeToolbarButtonLight.setBorderPainted(false);
        closeToolbarButtonLight.setToolTipText("Close this toolbar");
        closeToolbarButtonLight.addActionListener(e -> setVisible(false));
        add(closeToolbarButtonLight);

        // separator
        addSeparator(new Dimension(20, 0));

        // shortcuts
        addShortcutsControl();

        // separator
        addSeparator(new Dimension(20, 0));

        // highlight control
        addHighlightControl();

        // set enabled states
        setEnabledStates();

        // wire listeners
        wireListeners();
    }

    /**
     * Adds an <code>Observer</code> to the listener list.
     *
     * @param toolbarChangedListener the <code>Observer</code> to be added
     */
    public void addToolbarChangedListener(GenericPropertyChangeListener<Boolean> toolbarChangedListener) {
        propertyChangeSupport.addPropertyChangeListener(toolbarChangedListener);
    }

    /**
     * Removes <code>Observer</code> from the listener list.
     *
     * @param toolbarChangedListener the <code>Observer</code> to be removed
     */
    public void removeToolbarChangedListener(GenericPropertyChangeListener<Boolean> toolbarChangedListener) {
        propertyChangeSupport.removePropertyChangeListener(toolbarChangedListener);
    }

    // ************************************************************
    // shortcuts
    // ************************************************************
    private void addShortcutsControl() {

        // open Report
        openReportButtonLight.setFocusable(false);
        openReportButtonLight.setOpaque(false);
        openReportButtonLight.setBorderPainted(false);
        openReportButtonLight.setToolTipText("Open a report for browsing");
        openReportButtonLight.addActionListener(_ -> WOpen.openWindow());
        add(openReportButtonLight);

        // run bulk_extractor
        runButtonLight.setFocusable(false);
        runButtonLight.setOpaque(false);
        runButtonLight.setBorderPainted(false);
        runButtonLight.setToolTipText("Generate a report using bulk_extractor");
        runButtonLight.addActionListener(_ -> WScan.openWindow(new ScanSettings()));
        add(runButtonLight);

        // add bookmark
        addBookmarkButtonLight.setFocusable(false);
        addBookmarkButtonLight.setOpaque(false);
        addBookmarkButtonLight.setBorderPainted(false);
        addBookmarkButtonLight.setToolTipText("Bookmark the selected feature");
        addBookmarkButtonLight.addActionListener(_ -> {
            FeatureLine selectedFeatureLine = featureLineSelectionManager.getFeatureLineSelection();
            bookmarksModel.addElement(selectedFeatureLine);
        });
        add(addBookmarkButtonLight);

        // manage bookmarks
        manageBookmarksButtonLight.setFocusable(false);
        manageBookmarksButtonLight.setOpaque(false);
        manageBookmarksButtonLight.setBorderPainted(false);
        manageBookmarksButtonLight.setToolTipText("Manage bookmarks");
        manageBookmarksButtonLight.addActionListener((ActionListener) _ -> WManageBookmarks.openWindow());
        add(manageBookmarksButtonLight);

        // Copy
        copyButtonLight.setFocusable(false);
        copyButtonLight.setOpaque(false);
        copyButtonLight.setBorderPainted(false);
        copyButtonLight.setToolTipText("Copy selection to the System Clipboard");
        copyButtonLight.addActionListener(_ -> RangeSelectionManager.setSystemClipboard(rangeSelectionManager.getSelection()));
        add(copyButtonLight);

        // print selected Feature
        printButtonLight.setFocusable(false);
        printButtonLight.setOpaque(false);
        printButtonLight.setBorderPainted(false);
        printButtonLight.setToolTipText("Print the selected feature range");
        printButtonLight.addActionListener(_ -> DaggerContext.get().featureRangePrinter().printRange());
        add(printButtonLight);

        runButtonLight.setFocusable(false);
        runButtonLight.setOpaque(false);
        runButtonLight.setBorderPainted(false);
        runButtonLight.setToolTipText("Generate a report using bulk_extractor");
        runButtonLight.addActionListener(_ -> WScan.openWindow(new ScanSettings()));
        add(runButtonLight);

    }

    // ************************************************************
    // highlight
    // ************************************************************
    private void addHighlightControl() {

        // ************************************************************
        // "Highlight:" label
        // ************************************************************
        add(new JLabel("Highlight"));

        // separator
        addSeparator(new Dimension(GUI_X_PADDING, 0));

        // ************************************************************
        // highlight input text field TODO switchable highlight color
        // ************************************************************
        defaultHighlightTFBackgroundColor = highlightTF.getBackground();

        // require a fixed size
        Dimension highlightDimension = new Dimension(220, highlightTF.getPreferredSize().height);
        highlightTF.setMinimumSize(highlightDimension);
        highlightTF.setPreferredSize(highlightDimension);
        highlightTF.setMaximumSize(highlightDimension);

        // create the JTextField highlightTF for containing the highlight text
        highlightTF.setToolTipText("Type text to highlight, type \"|\" between words to enter multiple Highlights");

        // change the user highlight model when highlightTF changes
        highlightTF.getDocument().addDocumentListener(new DocumentListener() {
            private void setHighlightString() {
                // and set the potentially escaped highlight text in the image model
                byte[] highlightBytes = highlightTF.getText().getBytes(UTF8Tools.UTF_8);
                userHighlightModel.setHighlightBytes(highlightBytes);

                // also make the background yellow while there is text in highlightTF
                if (highlightBytes.length > 0) {
                    highlightTF.setBackground(highlightColorSelectionModel.getSelectedColor());// TODO pull from selection manager
                } else {
                    highlightTF.setBackground(defaultHighlightTFBackgroundColor);
                }
            }

            // JTextField responds to Document change
            public void insertUpdate(DocumentEvent e) {
                setHighlightString();
            }

            public void removeUpdate(DocumentEvent e) {
                setHighlightString();
            }

            public void changedUpdate(DocumentEvent e) {
                setHighlightString();
            }
        });
        highlightColorSelectionModel.addPropertyChangeListener((evt, oldValue, newValue) -> {
            highlightTF.putClientProperty("JComponent.outline", newValue); //TODO new ACCENT COLOR FETCH
            highlightTF.setBackground(newValue);
        });


        // add the highlight text textfield
        add(highlightTF);

        // separator
        addSeparator(new Dimension(10, 0));

        // ************************************************************
        // Match Case checkbox
        // ************************************************************
        // set initial value
        matchCaseCB.setSelected(userHighlightModel.isHighlightMatchCase());

        // set up the highlight match case checkbox
        matchCaseCB.setFocusable(false);
        matchCaseCB.setOpaque(false); // this looks better with the ToolBar's gradient
        matchCaseCB.setRequestFocusEnabled(false);
        matchCaseCB.setToolTipText("Match case in highlight text");

        // match case action listener
        matchCaseCB.addActionListener(_ -> {
            // toggle the case setting
            userHighlightModel.setHighlightMatchCase(matchCaseCB.isSelected());
            // as a convenience, change focus to highlightTF
            highlightTF.requestFocusInWindow();
        });

        // wire listener to keep view in sync with the model
        userHighlightModel.addUserHighlightModelChangedListener((GenericPropertyChangeListener<Boolean>) (_, _, newValue) -> {
            matchCaseCB.setSelected(newValue);
            // text field, which can include user escape codes
            byte[] fieldBytes = highlightTF.getText().getBytes(UTF8Tools.UTF_8);
            byte[] modelBytes = userHighlightModel.getHighlightBytes();
            if (!UTF8Tools.bytesMatch(fieldBytes, modelBytes)) {
                highlightTF.setText(new String(modelBytes));
            }
        });

        // add the match case button
        add(matchCaseCB);

        // ************************************************************
        // Highlight Colour choice on a button
        // ************************************************************
        var selectedColorPreview = new SelectedColorPreview();
        highlightColorSelectionModel.addPropertyChangeListener((evt, oldValue, newValue) -> {
            selectedColorPreview.setColor(newValue);
        });

        selectedColorPreview.addActionListener(_-> {
            SimpleColorPicker.showDialog(this, "Pick Highlight Color", new SimpleColorPicker(highlightColorSelectionModel));
            //update everywhere from highlightColorSelectionModel
        });

        add(selectedColorPreview);
        // separator
        addSeparator(new Dimension(10, 0));

        // ************************************************************
        // Dark theme switch - icons in Downloads
        // ************************************************************
        //Moon vs Sun icon switch
        themePickerButton.setFocusPainted(false);
        themePickerButton.setIcon(THEME_PICKER_DARK_MODE); // light mode → show moon icon to switch
        themePickerButton.addActionListener(e -> toggleTheme(themePickerButton, lookAndFeelCallback));
        add(themePickerButton);
    }

    // wire listeners to set enabled states
    private void wireListeners() {

        // a feature line has been selected and may be added as a bookmark
        featureLineSelectionManager.addFeatureLineSelectionManagerChangedListener((_, _, selectedFeatureLine) -> {
            // enabled if feature line is not blank and is not already bookmarked
            addBookmarkButtonLight.setEnabled(!isBlank(selectedFeatureLine) && !bookmarksModel.contains(selectedFeatureLine));
        });
        /// TODO - do we always want to call all sets for any change event?
        // the bookmarks list has changed
        bookmarksModel.addListDataListener(new ListDataListener() {
            public void contentsChanged(ListDataEvent e) {
                setEnabledStates();
            }
            public void intervalAdded(ListDataEvent e) {
                setEnabledStates();
            }
            public void intervalRemoved(ListDataEvent e) {
                setEnabledStates();
            }
        });

        // the user's feature or image range selection has changed,
        // impacting copy and print buttons
      // rangeSelectionManager.addRangeSelectionManagerChangedListener(new Observer() {
      //     public void update(Observable o, Object arg) {///TODO
      //         setEnabledStates();//TODO
      //     }//TODO
      // });
    }


    private void setEnabledStates() {
        // add bookmark
        FeatureLine selectedFeatureLine = featureLineSelectionManager.getFeatureLineSelection();
        if (selectedFeatureLine != null) {
            addBookmarkButtonLight.setEnabled(!isBlank(selectedFeatureLine)
                    && !bookmarksModel.contains(selectedFeatureLine));
        }
        // manage bookmarks
        manageBookmarksButtonLight.setEnabled(!bookmarksModel.isEmpty());

        // copy
        copyButtonLight.setEnabled(rangeSelectionManager.hasSelection());

        // print
        printButtonLight.setEnabled(rangeSelectionManager.hasSelection());
    }

    private static void toggleTheme(JButton button, Runnable lookAndFeelCallback ) {
        try {
            if (darkMode) {
                // switch to light
                UIManager.setLookAndFeel(new FlatIntelliJLaf());
                button.setIcon(THEME_PICKER_DARK_MODE);
                darkMode = false;
            } else {
                // switch to dark
                UIManager.setLookAndFeel(new FlatDarculaLaf());
                button.setIcon(THEME_PICKER_LIGHT_MODE);
                darkMode = true;
            }
            lookAndFeelCallback.run();
        } catch (UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();//TODO
        }
    }
}

