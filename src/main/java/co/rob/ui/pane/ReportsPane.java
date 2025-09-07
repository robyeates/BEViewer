package co.rob.ui.pane;

import co.rob.state.ReportsModel;
import co.rob.ui.renderer.ReportsTreeCellRenderer;
import co.rob.ui.selection.ReportSelectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.util.function.Consumer;

import static co.rob.util.BEConstants.GUI_EDGE_PADDING;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

/**
 * The <code>ReportsPane</code> class provides the reports view
 * and the interface for selecting a feature file.
 */
@Singleton
public final class ReportsPane extends Container {

    private static final Logger logger = LoggerFactory.getLogger(ReportsPane.class);

    private static final int EDGE_PADDING = GUI_EDGE_PADDING;
    //  private static final int Y_PADDING = BEViewer.GUI_Y_PADDING;
    private static final long serialVersionUID = 1;
    private final ReportsModel reportsModel;
    private final ReportSelectionManager reportSelectionManager;
    private JTree tree;

    /**
     * Scrolls to and opens the requested tree path.
     */
    public void scrollToTreePath(TreePath treePath) {
        logger.info("Scrolling to path for TreePath [{}]", treePath);
        tree.expandPath(treePath);
        tree.scrollPathToVisible(treePath);
    }

    /**
     * Constructs the interfaces for selecting a feature file.
     */
    public ReportsPane(ReportsModel reportsModelDAG, ReportSelectionManager reportSelectionManagerDAG) {
        this.reportsModel = reportsModelDAG;
        this.reportSelectionManager = reportSelectionManagerDAG;

        // wire reportsModel's tree selection model to notify reportSelectionManager of change
        //TreeSelectionListener

        // build the UI
        setComponents();

        reportsModel.getTreeSelectionModel().addTreeSelectionListener(e -> {
            ReportsModel.ReportTreeNode reportTreeNode = null;
            ReportsModel.FeaturesFileTreeNode featuresFileTreeNode = null;

            TreePath treePath = reportsModel.getSelectedTreePath();
            if (treePath != null) {
                reportTreeNode = ReportsModel.getReportTreeNodeFromTreePath(treePath);
                featuresFileTreeNode = ReportsModel.getFeaturesFileTreeNodeFromTreePath(treePath);
            }

            // get the image file and features file
            File reportImageFile = (reportTreeNode == null) ? null : reportTreeNode.reportImageFile;
            File featuresFile = (featuresFileTreeNode == null) ? null : featuresFileTreeNode.featuresFile();

            // forward the selection to the reportSelectionManager
            reportSelectionManager.setReportSelection(reportImageFile, featuresFile);
        });
        Consumer<TreePath> scrollCallback = this::scrollToTreePath;
        logger.info("Setting callback for TreePath [{}]", scrollCallback);
        reportsModel.addScrollCallback(scrollCallback);

    }

    /**
     * Used to establish tree as default selected component when BEViewer starts.
     */
    public void grabTreeFocus() {
        tree.grabFocus();
    }

    // ************************************************************
    // Constructs the ReportsPane interface
    // ************************************************************
    private void setComponents() {

        // use GridBagLayout with GridBagConstraints
        GridBagConstraints c;
        setLayout(new GridBagLayout());
        int y = 0;

        // (0,0) reports header containing "Reports" and delete button
        c = new GridBagConstraints();
        c.insets = new Insets(EDGE_PADDING, EDGE_PADDING, 0, EDGE_PADDING);
        c.gridx = 0;
        c.gridy = y++;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;

        // add the report pane's title
        add(getReportsHeader(), c);

        // (0,1) report tree inside JScrollPane
        c = new GridBagConstraints();
        c.insets = new Insets(0, EDGE_PADDING, EDGE_PADDING, EDGE_PADDING);
        c.gridx = 0;
        c.gridy = y++;
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;

        tree = new JTree();
        tree.setModel(reportsModel.getTreeModel());
        tree.setSelectionModel(reportsModel.getTreeSelectionModel());
        tree.setRootVisible(false);
        tree.setCellRenderer(new ReportsTreeCellRenderer());
        tree.setToggleClickCount(1);
        ToolTipManager.sharedInstance().registerComponent(tree);    // tooltip is normally off on JTree

        // create the scrollpane with the tree inside it
        JScrollPane reportsScrollPane = new JScrollPane(tree, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
        reportsScrollPane.setPreferredSize(new Dimension(250, 700));

        // add the scrollpane containing the features component
        add(reportsScrollPane, c);
    }

    // ************************************************************
    // reports pane header
    // ************************************************************
    private Container getReportsHeader() {
        Container container = new Container();
        container.setLayout(new GridBagLayout());
        GridBagConstraints c;

        // (0,0) "Reports" title
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        // add the report pane's title
        container.add(new JLabel("Reports"), c);

        return container;
    }
}

