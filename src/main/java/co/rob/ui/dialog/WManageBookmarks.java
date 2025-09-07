package co.rob.ui.dialog;

import co.rob.BEViewer;
import co.rob.DaggerContext;
import co.rob.pojo.FeatureLine;
import co.rob.ui.WarmUpCandidate;
import co.rob.ui.selection.FeatureLineSelectionManager;
import co.rob.ui.renderer.FeatureListCellRenderer;
import co.rob.io.BookmarksWriter;
import co.rob.state.BookmarksModel;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;

/**
 * The dialog window for exporting bookmarks.
 * //
 * Import of bookmarks seems to be handled with Import Work Settings
 */
@WarmUpCandidate
public class WManageBookmarks extends JDialog {

    private static WManageBookmarks wManageBookmarks;

    private final JList<FeatureLine> bookmarkL;
    private final JButton clearB = new JButton("Clear");
    private final JButton deleteB = new JButton("Delete");
    private final JButton navigateB = new JButton("Navigate To");
    private final JButton exportB = new JButton("Export");
    private final JButton closeB = new JButton("Close");

    private final FeatureLineSelectionManager featureLineSelectionManager;
    private final BookmarksModel bookmarksModel;
    private final BookmarksWriter bookmarksWriter;

    public WManageBookmarks() {

        bookmarksModel = DaggerContext.get().bookmarksModel();
        featureLineSelectionManager = DaggerContext.get().featureLineSelectionManager();
        bookmarksWriter = DaggerContext.get().bookmarksWriter();

        this.bookmarkL = bookmarksModel.createJList();
        this.bookmarkL.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.bookmarkL.setCellRenderer(new FeatureListCellRenderer<>());

        buildInterface();
        setButtonStates();
        wireActions();

        getRootPane().setDefaultButton(exportB);
        setTitle("Bookmarks");
        pack();

        wManageBookmarks=this;
    }

    /**
     * Use this static function to save bookmarks.
     */
    public void saveBookmarks() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Bookmarks as");
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        // if the user selects APPROVE then perform the export
        if (chooser.showOpenDialog(BEViewer.getBEWindow()) == JFileChooser.APPROVE_OPTION) {
            bookmarksWriter.exportBookmarks(chooser.getSelectedFile());
        }
    }

    /// TODO HMMMM,
    public static void openWindow() {
        wManageBookmarks.setLocation(BEViewer.getBEWindowLocation());
        wManageBookmarks.setVisible(true);
    }

    public static void closeWindow() {
        wManageBookmarks.setVisible(false);
    }

    private void buildInterface() {
        setTitle("Bookmarks");
        Container pane = getContentPane();

        // use GridBagLayout with GridBagConstraints
        GridBagConstraints c;
        pane.setLayout(new GridBagLayout());

        // (0,0) "Bookmarks List" label
        c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 0, 10);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.BOTH;
        pane.add(new JLabel("Bookmarks List"), c);

        // (0,1) scroll pane containing Bookmarks table
        c = new GridBagConstraints();
        c.insets = new Insets(0, 10, 10, 10);
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;

        // put the bookmarks list in a scroll pane
        JScrollPane scrollPane = new JScrollPane(bookmarkL);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        // add the scroll pane containing the bookmarks list
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

        // (0,1) clearB
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 1;
        container.add(clearB, c);

        // (1,1) deleteB
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 1;
        c.gridy = 1;
        container.add(deleteB, c);

        // (2,1) vertical separator
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 2;
        c.gridy = 1;
        container.add(new JSeparator(SwingConstants.VERTICAL), c);

        // (3,1) navigateB
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 3;
        c.gridy = 1;
        container.add(navigateB, c);

        // (4,1) exportB
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 4;
        c.gridy = 1;
        container.add(exportB, c);

        // (5,1) closeB
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 5;
        c.gridy = 1;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        container.add(closeB, c);

        return container;
    }

    private void setButtonStates() {

        // set states for buttons
        clearB.setEnabled(!bookmarksModel.isEmpty());
        deleteB.setEnabled(bookmarkL.getSelectedIndex() >= 0);
        navigateB.setEnabled(bookmarkL.getSelectedIndex() >= 0);
        exportB.setEnabled(!bookmarksModel.isEmpty());
    }

    private void wireActions() {
        // listen to bookmarks JList selection because selection state
        // changes button states
        bookmarkL.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                setButtonStates();
            }
        });

        // listen to bookmarks list changes
        bookmarksModel.addListDataListener(new ListDataListener() {
            public void contentsChanged(ListDataEvent e) {
                setButtonStates();
            }

            public void intervalAdded(ListDataEvent e) {
                setButtonStates();
            }

            public void intervalRemoved(ListDataEvent e) {
                setButtonStates();
            }
        });

        // clicking clearB deletes all bookmark entries
        clearB.addActionListener(_ -> bookmarksModel.clear());

        // clicking deleteB deletes the bookmark entry selected in bookmarkL
        deleteB.addActionListener(_ -> bookmarksModel.removeElement(bookmarkL.getSelectedValue()));

        // clicking navigateB navigates to the bookmark entry selected in bookmarkL
        navigateB.addActionListener(_ -> {
            FeatureLine featureLine = bookmarkL.getSelectedValue();
            if (featureLine == null) {
                throw new NullPointerException("Selected bookmark is null?");
            }
            featureLineSelectionManager.setFeatureLineSelection(featureLine);
        });

        // clicking exportB exports the bookmarked features
        exportB.addActionListener(_ -> saveBookmarks());

        // clicking closeB closes this window
        closeB.addActionListener(_ -> setVisible(false));
    }

}

