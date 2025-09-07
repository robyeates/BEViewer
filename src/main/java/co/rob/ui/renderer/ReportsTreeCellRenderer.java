package co.rob.ui.renderer;

import co.rob.state.ReportsModel;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.io.File;

/**
 * The <code>ReportsTreeCellRenderer</code> class provides a text list renderer view
 * for the tree nodes.
 */
public class ReportsTreeCellRenderer extends DefaultTreeCellRenderer {

    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        // configure the display based on the type of the value
        String text;
        String toolTipText;
        boolean isEnabled = true;

        switch (value) {
            case ReportsModel.RootTreeNode rootTreeNode -> {
                // RootTreeNode
                // only the root node can do this, but just prepare warning text
                text = "error";
                toolTipText = "";
            }
            case ReportsModel.ReportTreeNode reportTreeNode -> {
                // set text
                if (reportTreeNode.featuresDirectory != null) {
                    text = reportTreeNode.featuresDirectory.getName();
                } else {
                    text = "No features directory";
                }

                // set tool tip text
                if (reportTreeNode.featuresDirectory != null) {
                    toolTipText = reportTreeNode.featuresDirectory.toString();
                } else {
                    toolTipText = "No features directory";
                }
                if (reportTreeNode.reportImageFile != null) {
                    toolTipText += ", " + reportTreeNode.reportImageFile.toString();
                } else {
                    toolTipText += ", no image file";
                }
            }
            case ReportsModel.FeaturesFileTreeNode featuresFileTreeNode -> {
                // features file
                File featuresFile = featuresFileTreeNode.featuresFile();
                if (featuresFile != null) {
                    text = featuresFile.getName();
                    toolTipText = featuresFile.toString();
                } else {
                    text = "No features file";
                    toolTipText = "No features file";
                }

                // disable the cell unless the file size > 0
                try {
                    if (featuresFile.length() == 0) {
                        isEnabled = false;
                    }
                } catch (Exception e) {
                    isEnabled = false;
                }
            }
            case null, default -> throw new RuntimeException("invalid tree node: " + value);
        }

        // fill out and return this renderer
        super.getTreeCellRendererComponent(tree, text, selected, expanded, leaf, row, hasFocus);
        setToolTipText(toolTipText);
        setEnabled(isEnabled);
        return this;
    }
}

