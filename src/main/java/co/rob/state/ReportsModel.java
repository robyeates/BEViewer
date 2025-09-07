package co.rob.state;

import co.rob.ui.dialog.WError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.tree.*;
import java.io.File;
import java.io.FileFilter;
import java.util.*;
import java.util.function.Consumer;

/**
 * The <code>ReportsModel</code> class manages the Reports Tree
 * and provides the DefaultTreeModel and TreeSelectionModel
 * suitable for managing reports in a JTree view.
 * Unlike other models, signaling is sent to DefaultTreeModel treeModel.
 */
@Singleton
public class ReportsModel {

    private static final Logger logger = LoggerFactory.getLogger(ReportsModel.class);

    // Report Tree node root
    private final RootTreeNode rootTreeNode = new RootTreeNode();

    /**
     * Default Tree Model suitable for displaying the Reports Tree in a JTree view.
     */
    private final DefaultTreeModel treeModel = new DefaultTreeModel(rootTreeNode);

    /**
     * Default Tree Selection Model suitable for displaying the Reports Tree in a JTree view.
     */
    private final DefaultTreeSelectionModel treeSelectionModel = new DefaultTreeSelectionModel();

    private boolean includeStoplistFiles;
    private boolean includeEmptyFiles;

    // accept filenames for valid feature files
    // Features File filename filter
    private static final FileFilter featuresFileFilter = pathname -> {
        // reject unreadable files or paths that are directories
        if (!pathname.canRead() || pathname.isDirectory()) {
            return false;
        }

        // reject specific filenames
        String name = pathname.getName();
        if (!name.endsWith(".txt")) {
            // must end with .txt
            return false;
        }
        // exclude wordlist
        return !name.startsWith("wordlist_");
    };

    // filename comparator for sorting feature files
    private static final java.util.Comparator<File> fileComparator = File::compareTo;
    private Consumer<TreePath> scrollCallback;

    /**
     * Constructs a <code>ReportsModel</code> object.
     */
    @Inject
    public ReportsModel() {
        // configure the tree selection model to work with ReportsModel
        treeSelectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    }

    // a file equality checker that allows null
    private static boolean filesEqual(File f1, File f2) {
        // allow null when checking for equals
        if (f1 == null && f2 == null) {
            return true;
        } else if (f1 != null && f1.equals(f2)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Provides a Root TreeNode that can contain Report TreeNode children.
     */
    public static class RootTreeNode implements TreeNode {
        private final List<ReportTreeNode> reportTreeNodes = new ArrayList<>();

        public RootTreeNode() {
        }

        public int getIndex(File featuresDirectory, File reportImageFile) {

            // log if features directory does not contain report.xml
            File reportFile = new File(featuresDirectory, "report.xml");
            if (!reportFile.isFile()) {
                logger.warn("ReportsModel.getIndex: unexpected missing file {}", reportFile);
            }
            Optional<Integer> index = reportTreeNodes.stream()
                    .filter(node -> filesEqual(node.featuresDirectory, featuresDirectory) && filesEqual(node.reportImageFile, reportImageFile))
                    .map(reportTreeNodes::indexOf)
                    .findFirst();
            return index.orElse(-1);// a matching report was not found
        }

        // add unless present
        private void add(ReportTreeNode reportTreeNode) {
            if (getIndex(reportTreeNode) == -1) {
                // the report is new, so add it
                reportTreeNodes.add(reportTreeNode);
            } else {
                logger.info("ReportsModel.RootTreeNode.add: Node already present: {}", reportTreeNode);
            }
        }

        private void removeElementAt(int index) {
            if (index != -1) {
                // remove the report
                reportTreeNodes.remove(index);
            }
        }

        private int removeAllElements() {
            int size = reportTreeNodes.size();
            reportTreeNodes.clear();
            return size;
        }

        // the remaining functions implement TreeNode
        public Enumeration<ReportTreeNode> children() {
            return Collections.enumeration(reportTreeNodes);
        }

        public boolean getAllowsChildren() {
            return true;
        }

        public ReportTreeNode getChildAt(int childIndex) {
            return reportTreeNodes.get(childIndex);
        }

        public int getChildCount() {
            return reportTreeNodes.size();
        }

        public int getIndex(TreeNode treeNode) {
            return reportTreeNodes.indexOf(treeNode);
        }

        public TreeNode getParent() {
            return null;
        }

        public boolean isLeaf() {
            return false;
        }
    }

    /**
     * Provides a Report TreeNode that can contain Features File TreeNode children.
     * //TODO heavy IO on dir scan
     */
    public static class ReportTreeNode implements TreeNode {

        public final RootTreeNode parent;
        // these files define a Report
        public final File featuresDirectory;
        public final File reportImageFile;

        // a Report provides a vector of feature file nodes
        private final List<FeaturesFileTreeNode> featuresFileNodes = new ArrayList<>();

        public ReportTreeNode(RootTreeNode parent, File featuresDirectory, File reportImageFile) {
            this.parent = parent;
            this.featuresDirectory = featuresDirectory;
            this.reportImageFile = reportImageFile;
        }

        private void setFiles(boolean includeStoplistFiles, boolean includeEmptyFiles) {
            // get the initial array of feature files
            if (featuresDirectory == null || !featuresDirectory.exists()) {
                featuresFileNodes.clear();
                return;
            }
            var featuresFiles = featuresDirectory.listFiles(featuresFileFilter); //TODO - heavy I/O here
            if (featuresFiles == null) {
                logger.warn("ReportsModel.ReportTreeNode: unexpected invalid null featuresFiles.");
                featuresFiles = new File[0];
            }
            logger.info("Selected Report File [{}] found [{}] Feature files", featuresDirectory.getPath(), featuresFiles.length);
            // sort the array of features files
            Arrays.sort(featuresFiles, fileComparator);

            // add accepted files to featuresFileNodes
            featuresFileNodes.clear();
            for (File featuresFile : featuresFiles) {
                if (!includeStoplistFiles && featuresFile.getName().endsWith("_stoplist.txt")) {
                    // skip stoplists when not showing them
                    continue;
                }
                if (!includeEmptyFiles && featuresFile.length() == 0) {
                    // skip empty files when not showing them
                    continue;
                }
                featuresFileNodes.add(new FeaturesFileTreeNode(this, featuresFile));
            }
        }

        public String toString() {
            if (reportImageFile == null) {
                return "features directory: " + featuresDirectory + ", no image file, features files size: " + featuresFileNodes.size();
            } else {
                return "features directory: " + featuresDirectory + ", report image file: " + reportImageFile + ", features files size: " + featuresFileNodes.size();
            }
        }

        public Enumeration<FeaturesFileTreeNode> children() {
            return Collections.enumeration(featuresFileNodes);
        }

        public boolean getAllowsChildren() {
            return true;
        }

        public FeaturesFileTreeNode getChildAt(int childIndex) {
            return featuresFileNodes.get(childIndex);
        }

        public int getChildCount() {
            return featuresFileNodes.size();
        }

        public int getIndex(TreeNode treeNode) {
            FeaturesFileTreeNode featuresFileTreeNode = (FeaturesFileTreeNode) treeNode;
            if (featuresFileTreeNode == null) {
                return -1;
            }
            Optional<Integer> index = featuresFileNodes.stream()
                    .filter(node -> filesEqual(featuresFileTreeNode.featuresFile, node.featuresFile))
                    .map(featuresFileNodes::indexOf)
                    .findFirst();
            return index.orElse(-1);
        }

        public TreeNode getParent() {
            return parent;
        }

        public boolean isLeaf() {
            return false;
        }
    }

    /**
     * Provides a Features File TreeNode leaf.
     */
    public record FeaturesFileTreeNode(ReportTreeNode parent, File featuresFile) implements TreeNode {

        public Enumeration<FeaturesFileTreeNode> children() {
            return null;
        }

        public boolean getAllowsChildren() {
            return false;
        }

        public TreeNode getChildAt(int childIndex) {
            return null;
        }

        public int getChildCount() {
            return 0;
        }

        public int getIndex(TreeNode treeNode) {
            return -1;
        }

        public TreeNode getParent() {
            return parent;
        }

        public boolean isLeaf() {
            return true;
        }
    }

    /**
     * Obtain the tree model.
     */
    public TreeModel getTreeModel() {
        return treeModel;
    }

    /**
     * Obtain the tree selection model.
     */
    public TreeSelectionModel getTreeSelectionModel() {
        return treeSelectionModel;
    }

    /**
     * Set user preference to include stoplist files.
     */
    public void setIncludeStoplistFiles(boolean includeStoplistFiles) {
        this.includeStoplistFiles = includeStoplistFiles;
        setFiles();
    }

    /**
     * Return user preference to include stoplist files.
     */
    public boolean isIncludeStoplistFiles() {
        return includeStoplistFiles;
    }

    /**
     * Set user preference to include empty files.
     */
    public void setIncludeEmptyFiles(boolean includeEmptyFiles) {
        this.includeEmptyFiles = includeEmptyFiles;
        setFiles();
    }

    /**
     * Return user preference to include empty files.
     */
    public boolean isIncludeEmptyFiles() {
        return includeEmptyFiles;
    }

    /**
     * Refresh the list of files.
     */
    public void refreshFiles() {
        setFiles();
    }

    // reload FeaturesFileTreeNode and reload the treeModel
    private void setFiles() {
        // reload the FeaturesFileTreeNode leafs
        Enumeration<ReportsModel.ReportTreeNode> e = elements();
        while (e.hasMoreElements()) {
            ReportsModel.ReportTreeNode reportTreeNode = e.nextElement();
            reportTreeNode.setFiles(includeStoplistFiles, includeEmptyFiles);
        }

        // signal change to Tree Model
        treeModel.reload();
    }

    /**
     * Adds the report if it is new and updates the tree model.
     */
    public void addReport(File featuresDirectory, File reportImageFile) {
        // check to see if an equivalent report is already opened
        int index = rootTreeNode.getIndex(featuresDirectory, reportImageFile);

        if (index == -1) {
            // the report is new, so add it
            ReportTreeNode reportTreeNode = new ReportTreeNode(rootTreeNode, featuresDirectory, reportImageFile);
            rootTreeNode.add(reportTreeNode);

            // update the features file list
            reportTreeNode.setFiles(includeStoplistFiles, includeEmptyFiles);

            // signal change to Tree Model
            index = rootTreeNode.getIndex(reportTreeNode);
            int[] insertedIndex = {index};
            treeModel.nodesWereInserted(rootTreeNode, insertedIndex);
            logger.info("Selected Report File [{}] has been loaded", featuresDirectory.getPath());
        } else {
            // already there, so no action
            WError.showError("Report already opened for directory '"
                            + featuresDirectory + "'\nReport Image file '" + reportImageFile + "'.",
                    "Already Open", null);
        }
    }

    /**
     * Removes the report and updates the tree model.
     */
    public void remove(ReportTreeNode reportTreeNode) {
        int index = rootTreeNode.getIndex(reportTreeNode);
        if (index != -1) {

            // if the report removed is selected, deselect it
            TreePath selectedTreePath = treeSelectionModel.getSelectionPath();
            ReportTreeNode selectedReportTreeNode = getReportTreeNodeFromTreePath(selectedTreePath);
            if (reportTreeNode == selectedReportTreeNode) {
                treeSelectionModel.clearSelection();
                treeSelectionModel.setSelectionPath(null);
            }

            // remove the report tree node from the root tree node
            rootTreeNode.removeElementAt(index);

            // signal change to Tree Model
            int[] removedIndex = {index};
            TreeNode[] removedNodes = {reportTreeNode};
            treeModel.nodesWereRemoved(rootTreeNode, removedIndex, removedNodes);
        } else {
            // not there, so no action
            logger.info("ReportsModel.RootTreeNode.remove: Report not present: {}", reportTreeNode);
        }
    }

    /**
     * Clears all reports and updates the tree model.
     */
    public void clear() {
        int size = rootTreeNode.removeAllElements();
        if (size > 0) {
            // signal change to Tree Model
            treeModel.reload();

            // deselect any selection
            treeSelectionModel.clearSelection();
        } else {
            // already empty, so no action
            logger.info("ReportsModel.RootTreeNode.removeAllElements: Already empty");
        }
    }

    /**
     * Returns the report elements.
     */
    public Enumeration<ReportTreeNode> elements() {
        return rootTreeNode.children();
    }

    /**
     * Obtain the TreePath to a report else null.
     */
    public TreePath getTreePath(File featuresDirectory, File reportImageFile) {
        // find the index to a matching report
        int index = rootTreeNode.getIndex(featuresDirectory, reportImageFile);
        if (index != -1) {
            ReportTreeNode node = rootTreeNode.getChildAt(index);
            return new TreePath(new Object[]{node.parent, node});
        } else {
            return null;
        }
    }

    /**
     * Convenience function for returning the currently selected TreePath in the TreeSelectionModel.
     */
    public TreePath getSelectedTreePath() {
        return treeSelectionModel.getSelectionPath();
    }

    /**
     * Convenience function to get the ReportTreeNode from tree path
     */
    public static ReportTreeNode getReportTreeNodeFromTreePath(TreePath path) {
        if ((path != null) && path.getPathCount() >= 2) {
            return ((ReportTreeNode) (path.getPathComponent(1)));
        } else {
            return null;
        }
    }

    /**
     * Convenience function to get the FeaturesFileTreeNode from tree path
     */
    public static FeaturesFileTreeNode getFeaturesFileTreeNodeFromTreePath(TreePath path) {
        if ((path != null) && path.getPathCount() == 3) {
            return ((FeaturesFileTreeNode) (path.getPathComponent(2)));
        } else {
            return null;
        }
    }

    public void addScrollCallback(Consumer<TreePath> listener) {
        this.scrollCallback = listener;
    }

    public void requestScrollTo(TreePath treePath) {
        logger.info("Requesting scroll to [{}] for live callback [{}]", treePath, scrollCallback != null);
        scrollCallback.accept(treePath);
    }
}

