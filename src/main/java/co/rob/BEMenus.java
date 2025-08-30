package co.rob;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;

/**
 * The <code>BEViewer</code> class provides the main entry
 * for the Bulk Extractor Viewer application.
 */
public class BEMenus extends JMenuBar {

  private final long serialVersionUID = 1;
  private JCheckBoxMenuItem cbShowShortcutsToolbar;
  private JCheckBoxMenuItem cbShowHighlightToolbar;
  private JRadioButtonMenuItem rbFeatureText;
  private JRadioButtonMenuItem rbTypedText;
  private JRadioButtonMenuItem rbTextView;
  private JRadioButtonMenuItem rbHexView;
  private JRadioButtonMenuItem rbDecimal;
  private JRadioButtonMenuItem rbHex;
  private JRadioButtonMenuItem rbReferencedFeaturesVisible;
  private JRadioButtonMenuItem rbReferencedFeaturesCollapsible;
  private JCheckBoxMenuItem cbShowStoplistFiles;
  private JCheckBoxMenuItem cbShowEmptyFiles;
  private JMenuItem miPrintFeature;
  private JMenuItem miBookmark;
  private JMenuItem miCopy;
  private JMenuItem miClose;
  private JMenuItem miCloseAll;

  private final int KEY_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
  private final KeyStroke KEYSTROKE_O = KeyStroke.getKeyStroke(KeyEvent.VK_O, KEY_MASK);
  private final KeyStroke KEYSTROKE_Q = KeyStroke.getKeyStroke(KeyEvent.VK_Q, KEY_MASK);
  private final KeyStroke KEYSTROKE_C = KeyStroke.getKeyStroke(KeyEvent.VK_C, KEY_MASK);
  private final KeyStroke KEYSTROKE_R = KeyStroke.getKeyStroke(KeyEvent.VK_R, KEY_MASK);
  private final KeyStroke KEYSTROKE_A = KeyStroke.getKeyStroke(KeyEvent.VK_A, KEY_MASK);

  // ********************************************************************************
  // Create the menus
  // ********************************************************************************
  /**
   * Creates the menus for BEViewer.
   */
  public BEMenus() {

    JMenuItem mi;

    // file
    JMenu file = new JMenu("File");
    add(file);

    // file Open
    mi = new JMenuItem("Open Report\u2026", BEIcons.OPEN_REPORT_16);	// ...
    file.add(mi);
    mi.setAccelerator(KEYSTROKE_O);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        WOpen.openWindow();
      }
    });

    // file Close 
    miClose = new JMenuItem("Close Selected Report");
    file.add(miClose);
    miClose.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        // get the currently selected report
        TreePath selectedTreePath = BEViewer.reportsModel.getSelectedTreePath();
        ReportsModel.ReportTreeNode reportTreeNode = ReportsModel.getReportTreeNodeFromTreePath(selectedTreePath);
        if (reportTreeNode != null) {
          BEViewer.closeReport(reportTreeNode);
        } else {
          WLog.log("BEMenus Close Report failure");
        }
      }
    });
    miClose.setEnabled(false);

    // wire listener to manage when miClose is enabled
    BEViewer.reportsModel.getTreeSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        // get the currently selected report
        TreePath selectedTreePath = BEViewer.reportsModel.getSelectedTreePath();
        ReportsModel.ReportTreeNode reportTreeNode = ReportsModel.getReportTreeNodeFromTreePath(selectedTreePath);
        if (reportTreeNode != null) {
          miClose.setEnabled(true);
        } else {
          miClose.setEnabled(false);
        }
      }
    });

    // file Close all
    miCloseAll = new JMenuItem("Close all Reports");
    file.add(miCloseAll);
    miCloseAll.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.closeAllReports();
      }
    });
    miCloseAll.setEnabled(false);

    // wire ReportsModel's report tree model listener to manage when miCloseAll is enabled
    BEViewer.reportsModel.getTreeModel().addTreeModelListener(new TreeModelListener() {
      public void treeNodesChanged(TreeModelEvent e) {
        setCloseAllVisibility();
      }
      public void treeNodesInserted(TreeModelEvent e) {
        setCloseAllVisibility();
      }
      public void treeNodesRemoved(TreeModelEvent e) {
        setCloseAllVisibility();
      }
      public void treeStructureChanged(TreeModelEvent e) {
        setCloseAllVisibility();
      }
      private void setCloseAllVisibility() {
        Enumeration<ReportsModel.ReportTreeNode> e = BEViewer.reportsModel.elements();
        miCloseAll.setEnabled(e.hasMoreElements());
      }
    });

    // file|<separator>
    file.addSeparator();

    // file|export bookmarks
    miBookmark = new JMenuItem("Export Bookmarks\u2026", BEIcons.EXPORT_BOOKMARKS_16);	// ...
    file.add(miBookmark);
    miBookmark.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        WBookmarks.openWindow();
      }
    });

    // wire listener to manage when bookmarks are available for export
    BEViewer.featureBookmarksModel.addBookmarksModelChangedListener(new Observer() {
      public void update(Observable o, Object arg) {
        miBookmark.setEnabled(BEViewer.featureBookmarksModel.size() > 0);
      }
    });

    // file|<separator>
    file.addSeparator();

    // file|Import Work Settings
    mi = new JMenuItem("Import Work Settings\u2026");	// ...
    file.add(mi);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        WImportWorkSettings.openWindow();
      }
    });

    // file|Export Work Settings
    mi = new JMenuItem("Export Work Settings\u2026");	// ...
    file.add(mi);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        WExportWorkSettings.openWindow();
      }
    });

    // file|<separator>
    file.addSeparator();

    // file|Print Feature
    miPrintFeature = new JMenuItem("Print Range\u2026", BEIcons.PRINT_FEATURE_16);	// ...
    miPrintFeature.setEnabled(false);
    file.add(miPrintFeature);
    miPrintFeature.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.printRange();
      }
    });
    // wire listener to know when a feature is available for printing
    BEViewer.rangeSelectionManager.addRangeSelectionManagerChangedListener(new Observer() {
      public void update(Observable o, Object arg) {
        miPrintFeature.setEnabled(BEViewer.rangeSelectionManager.hasSelection());
      }
    });
 
    // file|<separator>
    file.addSeparator();

    // file Quit
    mi = new JMenuItem("Quit", BEIcons.EXIT_16);
    file.add(mi);
    mi.setAccelerator(KEYSTROKE_Q);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        // close down BEViewer
        BEViewer.doClose();
      }
    });

    // edit
    JMenu edit = new JMenu("Edit");
    add(edit);

    // edit|copy
    miCopy = new JMenuItem("Copy", BEIcons.COPY_16);
    miCopy.setEnabled(false);
    edit.add(miCopy);
    miCopy.setAccelerator(KEYSTROKE_C);
    miCopy.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        // put text onto System Clipboard
        RangeSelectionManager.setSystemClipboard(BEViewer.rangeSelectionManager.getSelection());
      }
    });

    // wire listener to manage when a buffer is available to copy
    BEViewer.rangeSelectionManager.addRangeSelectionManagerChangedListener(new Observer() {
      public void update(Observable o, Object arg) {
        miCopy.setEnabled(BEViewer.rangeSelectionManager.hasSelection());
      }
    });

    // edit|<separator>
    edit.addSeparator();

    // edit|clear navigation history
    mi = new JMenuItem("Clear Navigation History");
    edit.add(mi);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.featureNavigationComboBoxModel.removeAllFeatures();
      }
    });

    // view
    JMenu view = new JMenu("View");
    add(view);
    ButtonGroup imageGroup = new ButtonGroup();
    ButtonGroup forensicPathNumericBaseGroup = new ButtonGroup();
    ButtonGroup referencedFeaturesGroup = new ButtonGroup();

    // view|Toolbars
    JMenu toolbarMenu = new JMenu("Toolbars");
    view.add(toolbarMenu);

    // view|Toolbars|Shortcuts Toolbar
    cbShowShortcutsToolbar = new JCheckBoxMenuItem("Shortcuts Toolbar");
    cbShowShortcutsToolbar.setSelected(true);
    toolbarMenu.add(cbShowShortcutsToolbar);
    cbShowShortcutsToolbar.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.shortcutsToolbar.setVisible(cbShowShortcutsToolbar.isSelected());
      }
    });
    // wire listener to know when the shortcuts toolbar is visible
    BEViewer.shortcutsToolbar.addShortcutsToolbarChangedListener(new Observer() {
      public void update(Observable o, Object arg) {
        cbShowShortcutsToolbar.setSelected(BEViewer.shortcutsToolbar.isVisible());
      }
    });

    // view|Toolbars|Highlight Toolbar
    cbShowHighlightToolbar = new JCheckBoxMenuItem("Highlight Toolbar");
    cbShowHighlightToolbar.setSelected(true);
    toolbarMenu.add(cbShowHighlightToolbar);
    cbShowHighlightToolbar.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.highlightToolbar.setVisible(cbShowHighlightToolbar.isSelected());
      }
    });
    // wire listener to know when the highlight toolbar is visible
    BEViewer.highlightToolbar.addHighlightToolbarChangedListener(new Observer() {
      public void update(Observable o, Object arg) {
        cbShowHighlightToolbar.setSelected(BEViewer.highlightToolbar.isVisible());
      }
    });

    // view|Image Format
    JMenu imageFormatMenu = new JMenu("Image Format");
    view.add(imageFormatMenu);

    // view|Image Format|Text
    rbTextView = new JRadioButtonMenuItem("Text");
    imageGroup.add(rbTextView);
    imageFormatMenu.add(rbTextView);
    rbTextView.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.imageView.setLineFormat(ImageLine.LineFormat.TEXT_FORMAT);
      }
    });

    // view|Image Format|Hex
    rbHexView = new JRadioButtonMenuItem("Hex");
    imageGroup.add(rbHexView);
    imageFormatMenu.add(rbHexView);
    rbHexView.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.imageView.setLineFormat(ImageLine.LineFormat.HEX_FORMAT);
      }
    });

    // wire listener to manage which image view type button is selected
    BEViewer.imageView.addImageViewChangedListener(new Observer() {
      public void update(Observable o, Object arg) {
        // this could be changed to act only on ImageView.ChangeType.FORENSIC_PATH_NUMERIC_BASE_CHANGED
        ImageLine.LineFormat lineFormat = BEViewer.imageView.getLineFormat();
        if (lineFormat == ImageLine.LineFormat.TEXT_FORMAT) {
          rbTextView.setSelected(true);
        } else if (lineFormat == ImageLine.LineFormat.HEX_FORMAT) {
          rbHexView.setSelected(true);
        } else {
          throw new RuntimeException("invalid image format source");
        }
      }
    });
    
    // view|Forensic Path Numeric Base
    JMenu forensicPathNumericBaseMenu = new JMenu("Forensic Path Numeric Base");
    view.add(forensicPathNumericBaseMenu);

    // view|Forensic Path Numeric Base|Decimal
    rbDecimal = new JRadioButtonMenuItem("Decimal");
    forensicPathNumericBaseGroup.add(rbDecimal);
    forensicPathNumericBaseMenu.add(rbDecimal);
    rbDecimal.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.featuresModel.setUseHexPath(false);
        BEViewer.referencedFeaturesModel.setUseHexPath(false);
        BEViewer.imageView.setUseHexPath(false);
      }
    });

    // view|Forensic Path Numeric Base|Hex
    rbHex = new JRadioButtonMenuItem("Hexadecimal");
    forensicPathNumericBaseGroup.add(rbHex);
    forensicPathNumericBaseMenu.add(rbHex);
    rbHex.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.featuresModel.setUseHexPath(true);
        BEViewer.referencedFeaturesModel.setUseHexPath(true);
        BEViewer.imageView.setUseHexPath(true);
      }
    });
    
    // wire listener to manage which forensic path numeric base is shown in the menu
    BEViewer.imageView.addImageViewChangedListener(new Observer() {
      public void update(Observable o, Object arg) {
        if (BEViewer.imageView.getChangeType() == ImageView.ChangeType.FORENSIC_PATH_NUMERIC_BASE_CHANGED) {
          boolean isHex = BEViewer.imageView.getUseHexPath();
          if (!isHex) {
            rbDecimal.setSelected(true);
          } else {
            rbHex.setSelected(true);
          }
        }
      }
    });
    
    // view|Feature File Font
    JMenu featureFileFontMenu = new JMenu("Feature File Font");
    view.add(featureFileFontMenu);

    // view|Feature File Font|Zoom In
    mi = new JMenuItem("Zoom In");
    featureFileFontMenu.add(mi);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.featuresModel.setFontSize(BEViewer.featuresModel.getFontSize() + 1);
        BEViewer.referencedFeaturesModel.setFontSize(BEViewer.featuresModel.getFontSize());
      }
    });

    // view|Feature File Font|Zoom Out
    mi = new JMenuItem("Zoom Out");
    featureFileFontMenu.add(mi);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        int oldFontSize = BEViewer.featuresModel.getFontSize();
        if (oldFontSize > 6) {
          BEViewer.featuresModel.setFontSize(oldFontSize - 1);
          BEViewer.referencedFeaturesModel.setFontSize(BEViewer.featuresModel.getFontSize());
        } else {
          WError.showError("Already at minimum font size of " + oldFontSize + ".",
                           "BEViewer Feature Font Size error", null);
        }
      }
    });

    // view|Feature File View|Normal Size
    mi = new JMenuItem("Normal Size");
    featureFileFontMenu.add(mi);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.featuresModel.setFontSize(BEPreferences.DEFAULT_FEATURE_FONT_SIZE);
        BEViewer.referencedFeaturesModel.setFontSize(BEViewer.featuresModel.getFontSize());
      }
    });

    // view|Image File Font
    JMenu imageFileFontMenu = new JMenu("Image File Font");
    view.add(imageFileFontMenu);

    // view|Image File Font|Zoom In
    mi = new JMenuItem("Zoom In");
    imageFileFontMenu.add(mi);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.imageView.setFontSize(BEViewer.imageView.getFontSize() + 1);
      }
    });

    // view|Image File Font|Zoom Out
    mi = new JMenuItem("Zoom Out");
    imageFileFontMenu.add(mi);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        int oldFontSize = BEViewer.imageView.getFontSize();
        if (oldFontSize > 6) {
          BEViewer.imageView.setFontSize(oldFontSize - 1);
        } else {
          WError.showError("Already at minimum font size of " + oldFontSize + ".",
                           "BEViewer Image File Font Size error", null);
        }
      }
    });

    // view|Image File Font|Normal Size
    mi = new JMenuItem("Normal Size");
    imageFileFontMenu.add(mi);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.imageView.setFontSize(BEPreferences.DEFAULT_IMAGE_FONT_SIZE);
      }
    });

    // view|Referenced Features
    JMenu referencedFeaturesMenu = new JMenu("Referenced Features");
    view.add(referencedFeaturesMenu);

    // view|Referenced Features|always visible
    rbReferencedFeaturesVisible = new JRadioButtonMenuItem("Always Visible");
    referencedFeaturesGroup.add(rbReferencedFeaturesVisible);
    referencedFeaturesMenu.add(rbReferencedFeaturesVisible);
    rbReferencedFeaturesVisible.setSelected(!BEViewer.reportSelectionManager.isRequestHideReferencedFeatureView());
    rbReferencedFeaturesVisible.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.reportSelectionManager.setRequestHideReferencedFeatureView(false);
      }
    });

    // view|Referenced Features|collapsible
    rbReferencedFeaturesCollapsible = new JRadioButtonMenuItem("Collapsed when not Referenced");
    referencedFeaturesGroup.add(rbReferencedFeaturesCollapsible);
    referencedFeaturesMenu.add(rbReferencedFeaturesCollapsible);
    rbReferencedFeaturesCollapsible.setSelected(BEViewer.reportSelectionManager.isRequestHideReferencedFeatureView());
    rbReferencedFeaturesCollapsible.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.reportSelectionManager.setRequestHideReferencedFeatureView(true);
      }
    });

    // wire listener to manage which visibility mode is shown in the menu
    BEViewer.reportSelectionManager.addReportSelectionManagerChangedListener(new Observer() {
      public void update(Observable o, Object arg) {
        boolean requestHide = BEViewer.reportSelectionManager.isRequestHideReferencedFeatureView();
        if (requestHide) {
          rbReferencedFeaturesCollapsible.setSelected(true);
        } else {
          rbReferencedFeaturesVisible.setSelected(true);
        }
      }
    });
    
    // view|Feature Files
    JMenu featureFilesMenu = new JMenu("Feature Files");
    view.add(featureFilesMenu);

    // view|Feature Files|Show Stoplist Files
    cbShowStoplistFiles = new JCheckBoxMenuItem("Show Stoplist Files");
    featureFilesMenu.add(cbShowStoplistFiles);
    cbShowStoplistFiles.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.reportsModel.setIncludeStoplistFiles(cbShowStoplistFiles.getState());
      }
    });

    // wire ReportsModel's report tree model listener to manage when show stoplist files is enabled
    BEViewer.reportsModel.getTreeModel().addTreeModelListener(new TreeModelListener() {
      public void treeNodesChanged(TreeModelEvent e) {
        setShowStoplistVisibility();
      }
      public void treeNodesInserted(TreeModelEvent e) {
        setShowStoplistVisibility();
      }
      public void treeNodesRemoved(TreeModelEvent e) {
        setShowStoplistVisibility();
      }
      public void treeStructureChanged(TreeModelEvent e) {
        setShowStoplistVisibility();
      }
      private void setShowStoplistVisibility() {
        Enumeration<ReportsModel.ReportTreeNode> e = BEViewer.reportsModel.elements();
        cbShowStoplistFiles.setSelected(BEViewer.reportsModel.isIncludeStoplistFiles());
      }
    });

    // view|Feature Files|Show Empty Files
    cbShowEmptyFiles = new JCheckBoxMenuItem("Show Empty Files");
    featureFilesMenu.add(cbShowEmptyFiles);
    cbShowEmptyFiles.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.reportsModel.setIncludeEmptyFiles(cbShowEmptyFiles.getState());
      }
    });

    // wire ReportsModel's report tree model listener to manage when show empty files is enabled
    BEViewer.reportsModel.getTreeModel().addTreeModelListener(new TreeModelListener() {
      public void treeNodesChanged(TreeModelEvent e) {
        setShowEmptyFilesVisibility();
      }
      public void treeNodesInserted(TreeModelEvent e) {
        setShowEmptyFilesVisibility();
      }
      public void treeNodesRemoved(TreeModelEvent e) {
        setShowEmptyFilesVisibility();
      }
      public void treeStructureChanged(TreeModelEvent e) {
        setShowEmptyFilesVisibility();
      }
      private void setShowEmptyFilesVisibility() {
        Enumeration<ReportsModel.ReportTreeNode> e = BEViewer.reportsModel.elements();
        cbShowEmptyFiles.setSelected(BEViewer.reportsModel.isIncludeEmptyFiles());
      }
    });

    // view|<separator>
    view.addSeparator();

    // view|propertes
    JMenu properties = new JMenu("Properties");
    view.add(properties);

    // view|Properties|report.xml File
    mi = new JMenuItem("report.xml File");
    properties.add(mi);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        // get the currently selected feature line
        FeatureLine featureLine = BEViewer.featureLineSelectionManager.getFeatureLineSelection();
        File featureFile = featureLine.featuresFile;
        if (featureFile == null || featureLine.featuresFile == null) {
          WError.showError("A Feature must be selected before viewing the Report file.", 
                           "BEViewer Properties error", null);
        } else {
          try {
            File reportFile = new File(featureLine.featuresFile.getParentFile(), "report.xml");
            URL url = reportFile.toURI().toURL();
            new WURL("Bulk Extractor Viewer Report file " + reportFile.toString(), url);
          } catch (Exception exc) {
            WError.showError("Unable to read report.xml file.", 
                             "BEViewer Read error", exc);
          }
        }
      }
    });

    // tools
    JMenu tools = new JMenu("Tools");
    add(tools);

    // tools|Run bulk_extractor
    mi = new JMenuItem("Run bulk_extractor\u2026", BEIcons.RUN_BULK_EXTRACTOR_16);	// ...
    tools.add(mi);
    mi.setAccelerator(KEYSTROKE_R);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        WScan.openWindow();
      }
    });

    // help
    JMenu help = new JMenu("Help");
    add(help);

//    // help|BEViewer Help
//    mi = new JMenuItem("BEViewer Help", BEIcons.HELP_16);
//    help.add(mi);
//    mi.setAccelerator(KEYSTROKE_H);
//    mi.addActionListener(new ActionListener() {
//      public void actionPerformed (ActionEvent e) {
//        if (Desktop.isDesktopSupported()) {
//          try {
//////        WHelp.openWindow();
////        URL helpURL = this.getClass().getClassLoader().getResource("doc/help.html");
////        new WURL("Bulk Extractor Viewer Help", helpURL);
//      }
//    });

    // help|About
    mi = new JMenuItem("About BEViewer 0.0.1-Alpha" , BEIcons.HELP_ABOUT_16);
    help.add(mi);
    mi.setAccelerator(KEYSTROKE_A);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        WAbout.openWindow();
      }
    });

    // help|Check Versions
    mi = new JMenuItem("Check Versions");
    help.add(mi);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BulkExtractorVersionReader.showVersions();
      }
    });

    // help|<separator>
    help.addSeparator();

    // help|Show Log
    mi = new JMenuItem("Show Log");
    help.add(mi);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        WLog.setVisible();
      }
    });

    // help|Clear Log
    mi = new JMenuItem("Clear Log");
    help.add(mi);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        WLog.clearLog();
      }
    });

    // help|Copy Log to System Clipboard
    mi = new JMenuItem("Copy Log to System Clipboard");
    help.add(mi);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        // clear the selection manager
        BEViewer.rangeSelectionManager.clear();

        // get the Transferable log
        Transferable log = new StringSelection(WLog.getLog());

        // copy the log to the system clipboard and, if available, to the selection clipboard
        RangeSelectionManager.setSystemClipboard(log);
        RangeSelectionManager.setSelectionClipboard(log);
      }
    });

    // help|<separator>
    help.addSeparator();

    // help|diagnostics
    JMenu diagnostics = new JMenu("Diagnostics");
    help.add(diagnostics);

    // help|diagnostics|Close all Image Readers
    mi = new JMenuItem("Close All Image Readers");
    diagnostics.add(mi);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        BEViewer.imageModel.closeAllImageReaders();
      }
    });

    // help|diagnostics|Image End Page
    mi = new JMenuItem("Show Image End Page");
    diagnostics.add(mi);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        // move to end of feature currently in the image model
        ImageModel.ImagePage imagePage = BEViewer.imageView.getImagePage();
        long offset = ForensicPath.getOffset(imagePage.pageForensicPath);
        long imageEndOffset = (offset > 0) ? offset - 1 : 0;
        BEViewer.imageModel.setImageSelection(ForensicPath.getAdjustedPath(imagePage.pageForensicPath, imageEndOffset));
      }
    });

    // help|diagnostics|run tests
    mi = new JMenuItem("Run Tests\u2026");	// ...
    diagnostics.add(mi);
    mi.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        new WTest();
      }
    });
  }
}

