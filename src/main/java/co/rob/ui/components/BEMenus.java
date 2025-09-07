package co.rob.ui.components;

import co.rob.BEViewer;
import co.rob.DaggerContext;
import co.rob.api.BulkExtractorVersionReader;
import co.rob.pojo.FeatureLine;
import co.rob.pojo.scan.ScanSettings;
import co.rob.state.*;
import co.rob.ui.dialog.*;
import co.rob.ui.dialog.scan.WScan;
import co.rob.ui.dialog.scan.WScanSettingsRunQueue;
import co.rob.ui.icons.BEIcons;
import co.rob.ui.selection.FeatureLineSelectionManager;
import co.rob.ui.selection.RangeSelectionManager;
import co.rob.ui.selection.ReportSelectionManager;
import co.rob.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.function.Consumer;

import static co.rob.io.features.FeatureLineUtils.isBlank;
import static co.rob.io.features.FeatureLineUtils.isFromReport;
import static co.rob.ui.icons.BEIcons.*;
import static co.rob.util.UserTextFormatSettings.LINE_FORMAT_PATH_PROPERTY_CHANGE;

/**
 * The <code>BEViewer</code> class provides the main entry
 * for the Bulk Extractor Viewer application.
 */
public class BEMenus extends JMenuBar {

    private static final Logger logger = LoggerFactory.getLogger(BEMenus.class);
    private final JMenuItem importWorkSettingsMenuItem;
    private final JMenuItem exportWorkSettingsMenuItem;
    private final JMenuItem quitMenuItem;
    private final JMenuItem zoomInMenuItem;
    private final JMenuItem zoomOutMenuItem;
    private final JMenuItem normalSizeMenuItem;
    private final JMenuItem imageFontZoomInMenuItem;
    private final JMenuItem imageFontZoomOutMenuItem;
    private final JMenuItem imageFontNormalSizeMenuItem;
    private final JMenuItem refreshMenuItem;
    private final JMenuItem runBulkExtractorMenuItem;
    private final JMenuItem openRunQueueMenuItem;
    private final JMenuItem clearLogMenuItem;
    private final JMenuItem aboutBulkExtractorViewerMenuItem;
    private final JMenuItem checkBulkExtractorVersionMenuItem;
    private final JMenuItem copyLogMenuItem;
    private final JMenuItem runTestsMenuItem;


    private final ReportsModel reportsModel;
    private final BookmarksModel bookmarksModel;
    private final FeatureLineSelectionManager featureLineSelectionManager;
    private final FeaturesModel featuresModel;
    private final UserHighlightModel userHighlightModel;


    private LogViewer logViewer;

    private JCheckBoxMenuItem cbShowToolbar;
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
  private JMenuItem printFeatureMenuItem;
  private JMenuItem clearBookmarksMenuItem;
  private JMenuItem exportBookmarksMenuItem;
  private JMenuItem copyMenuItem;
  private JMenuItem menuItemCloseOneReport;
  private JMenuItem menuItemCloseAllReports;
  private JMenuItem addBookmarkMenuItem;
  private JMenuItem manageBookmarksMenuItem;
  private JMenuItem panToStartMenuItem;
  private JMenuItem panToEndMenuItem;
  private JMenuItem showReportFileMenuItem;
  private JMenuItem showLogMenuItem;

  private final int KEY_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
  private final KeyStroke KEYSTROKE_O = KeyStroke.getKeyStroke(KeyEvent.VK_O, KEY_MASK);
  private final KeyStroke KEYSTROKE_Q = KeyStroke.getKeyStroke(KeyEvent.VK_Q, KEY_MASK);
  private final KeyStroke KEYSTROKE_C = KeyStroke.getKeyStroke(KeyEvent.VK_C, KEY_MASK);
  private final KeyStroke KEYSTROKE_R = KeyStroke.getKeyStroke(KeyEvent.VK_R, KEY_MASK);
  private final KeyStroke KEYSTROKE_A = KeyStroke.getKeyStroke(KeyEvent.VK_A, KEY_MASK);
  private final KeyStroke KEYSTROKE_M = KeyStroke.getKeyStroke(KeyEvent.VK_M, KEY_MASK);
  private final KeyStroke KEYSTROKE_B = KeyStroke.getKeyStroke(KeyEvent.VK_B, KEY_MASK);
  private final KeyStroke KEYSTROKE_L = KeyStroke.getKeyStroke(KeyEvent.VK_L, KEY_MASK);

  // ********************************************************************************
  // Create the menus
  // ********************************************************************************
  /**
   * Creates the menus for 
   */
  public BEMenus(BEToolbar toolbar) {

      reportsModel = DaggerContext.get().reportsModel();
      bookmarksModel = DaggerContext.get().bookmarksModel();
      featuresModel = DaggerContext.get().featuresModel();
      var referencedFeaturesModel = DaggerContext.get().referencedFeaturesModel();
      var imageViewww = DaggerContext.get().imageView();
      var imageModellll = DaggerContext.get().imageModel();


      var rangeSelectionManager = DaggerContext.get().rangeSelectionManager();
      var reportSelectionManager = DaggerContext.get().reportSelectionManager();
      featureLineSelectionManager = DaggerContext.get().featureLineSelectionManager();

      userHighlightModel = DaggerContext.get().userHighlightModel();

      // fileMenu
      JMenu fileMenu = new JMenu("File");
      add(fileMenu);

      // fileMenu Open
      JMenuItem openReportMenuItem = new JMenuItem("Open Report…", OPEN_REPORT_16_LIGHT);    // ...
      fileMenu.add(openReportMenuItem);
      openReportMenuItem.setAccelerator(KEYSTROKE_O);
      openReportMenuItem.addActionListener(_ -> WOpen.openWindow());

      // fileMenu Close
      menuItemCloseOneReport = new JMenuItem("Close Selected Report");
      fileMenu.add(menuItemCloseOneReport);
      menuItemCloseOneReport.addActionListener(_ -> {
          // get the currently selected report
          TreePath selectedTreePath = reportsModel.getSelectedTreePath();
          ReportsModel.ReportTreeNode reportTreeNode = ReportsModel.getReportTreeNodeFromTreePath(selectedTreePath);
          if (reportTreeNode != null) {
              closeReport(reportTreeNode);
          } else {
              logger.error("BEMenus Close Report failure");
          }
      });
      menuItemCloseOneReport.setEnabled(false);

      // wire listener to manage when miClose is enabled
      reportsModel.getTreeSelectionModel().addTreeSelectionListener(_ -> {
          // get the currently selected report
          TreePath selectedTreePath = reportsModel.getSelectedTreePath();
          ReportsModel.ReportTreeNode reportTreeNode = ReportsModel.getReportTreeNodeFromTreePath(selectedTreePath);
          menuItemCloseOneReport.setEnabled(reportTreeNode != null);
      });

      // fileMenu Close all
      menuItemCloseAllReports = new JMenuItem("Close all Reports");

      fileMenu.add(menuItemCloseAllReports);
      menuItemCloseAllReports.addActionListener(_ -> {
          if (showConfirmation(fileMenu) == 0) { //Popup confirmation since it deletes all state!
              logger.info("User confirmed Clear all Reports. Clearing data...");
          } else {
              logger.info("User cancelled. No action taken.");
              closeAllReports();
          }
      });
      menuItemCloseAllReports.setEnabled(false);

      // wire ReportsModel's report tree model listener to manage when miCloseAll is enabled
      reportsModel.getTreeModel().addTreeModelListener(new TreeModelListener() {
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
              Enumeration<ReportsModel.ReportTreeNode> e = reportsModel.elements();
              menuItemCloseAllReports.setEnabled(e.hasMoreElements());
          }
      });

      // fileMenu|<separator>
      fileMenu.addSeparator();

      // fileMenu|export bookmarks
      exportBookmarksMenuItem = new JMenuItem("Export Bookmarks…", EXPORT_BOOKMARKS_16_LIGHT);    // ...
      fileMenu.add(exportBookmarksMenuItem);
      exportBookmarksMenuItem.addActionListener(_ -> new WManageBookmarks().saveBookmarks());

      // wire listener to manage when bookmarks are available for export
      bookmarksModel.addListDataListener(new ListDataListener() {
          public void contentsChanged(ListDataEvent e) {
              exportBookmarksMenuItem.setEnabled(!bookmarksModel.isEmpty());
          }

          public void intervalAdded(ListDataEvent e) {
              exportBookmarksMenuItem.setEnabled(!bookmarksModel.isEmpty());
          }

          public void intervalRemoved(ListDataEvent e) {
              exportBookmarksMenuItem.setEnabled(!bookmarksModel.isEmpty());
          }
      });
      exportBookmarksMenuItem.setEnabled(false);

      // fileMenu|<separator>
      fileMenu.addSeparator();

      // fileMenu|Import Work Settings
      importWorkSettingsMenuItem = new JMenuItem("Import Work Settings…");    // ...
      fileMenu.add(importWorkSettingsMenuItem);
      Consumer<Void> closeAllCallback = _ -> closeAllReports();

      importWorkSettingsMenuItem.addActionListener(_ -> WImportWorkSettings.openWindow(closeAllCallback));

      // fileMenu|Export Work Settings
      exportWorkSettingsMenuItem = new JMenuItem("Export Work Settings…");    // ...
      fileMenu.add(exportWorkSettingsMenuItem);
      exportWorkSettingsMenuItem.addActionListener(_ -> WExportWorkSettings.openWindow());

      // fileMenu|<separator>
      fileMenu.addSeparator();

      // fileMenu|Print Feature
      printFeatureMenuItem = new JMenuItem("Print Range…", BEIcons.PRINT_FEATURE_16_LIGHT);    // ...
      printFeatureMenuItem.setEnabled(false);
      fileMenu.add(printFeatureMenuItem);
      printFeatureMenuItem.addActionListener(e -> getToolkit().getSystemClipboard());// TODOnew FeatureRangePrinter().printRange());
      // wire listener to know when a feature is available for printing
      rangeSelectionManager.addRangeSelectionManagerChangedListener(_ -> printFeatureMenuItem.setEnabled(rangeSelectionManager.hasSelection()));

      // fileMenu|<separator>
      fileMenu.addSeparator();

      // fileMenu Quit
      quitMenuItem = new JMenuItem("Quit", BEIcons.EXIT_16_LIGHT);
      fileMenu.add(quitMenuItem);
      quitMenuItem.setAccelerator(KEYSTROKE_Q);
      quitMenuItem.addActionListener(_ -> {
          // close down BEViewer
          // save preferences then exit
          new BEPreferences().savePreferences(toolbar.isVisible());
          BEViewer.doClose();
      });

      // edit
      JMenu edit = new JMenu("Edit");
      add(edit);

      // edit|copy
      copyMenuItem = new JMenuItem("Copy", BEIcons.COPY_16_LIGHT);
      copyMenuItem.setEnabled(false);
      edit.add(copyMenuItem);
      copyMenuItem.setAccelerator(KEYSTROKE_C);
      copyMenuItem.addActionListener(_ -> {
          // put text onto System Clipboard
          RangeSelectionManager.setSystemClipboard(rangeSelectionManager.getSelection());
      });

      // wire listener to manage when a buffer is available to copy
      rangeSelectionManager.addRangeSelectionManagerChangedListener(_ -> copyMenuItem.setEnabled(rangeSelectionManager.hasSelection()));

      // edit|<separator>
      edit.addSeparator();

      // edit|clear all bookmarks
      clearBookmarksMenuItem = new JMenuItem("Clear Bookmarks");
      edit.add(clearBookmarksMenuItem);
      clearBookmarksMenuItem.addActionListener(e -> bookmarksModel.clear());
      clearBookmarksMenuItem.setEnabled(false);

      // wire listener to know when bookmarks are empty
      bookmarksModel.addListDataListener(new ListDataListener() {
          public void contentsChanged(ListDataEvent e) {
              clearBookmarksMenuItem.setEnabled(!bookmarksModel.isEmpty());
          }

          public void intervalAdded(ListDataEvent e) {
              clearBookmarksMenuItem.setEnabled(!bookmarksModel.isEmpty());
          }

          public void intervalRemoved(ListDataEvent e) {
              clearBookmarksMenuItem.setEnabled(!bookmarksModel.isEmpty());
          }
      });

      // view
      JMenu view = new JMenu("View");
      add(view);
      ButtonGroup imageGroup = new ButtonGroup();
      ButtonGroup forensicPathNumericBaseGroup = new ButtonGroup();
      ButtonGroup referencedFeaturesGroup = new ButtonGroup();

      // view|Toolbar
      cbShowToolbar = new JCheckBoxMenuItem("Toolbar");
      cbShowToolbar.setSelected(true);
      view.add(cbShowToolbar);
      cbShowToolbar.addActionListener(_ -> {
          toolbar.setVisible(cbShowToolbar.isSelected());
      });
      // wire listener to know when the toolbar is visible
      toolbar.addToolbarChangedListener((_, _, _) -> cbShowToolbar.setSelected(toolbar.isVisible()));

      // view|Image Format
      JMenu imageFormatMenu = new JMenu("Image Format");
      view.add(imageFormatMenu);

      // view|Image Format|Text
      rbTextView = new JRadioButtonMenuItem("Text");
      imageGroup.add(rbTextView);
      imageFormatMenu.add(rbTextView);
      rbTextView.addActionListener(_ -> UserTextFormatSettings.setLineFormat(ImageLineFormat.TEXT_FORMAT));

      // view|Image Format|Hex
      rbHexView = new JRadioButtonMenuItem("Hex");
      imageGroup.add(rbHexView);
      imageFormatMenu.add(rbHexView);
      rbHexView.addActionListener(_ -> UserTextFormatSettings.setLineFormat(ImageLineFormat.HEX_FORMAT));

      // wire listener to manage which image view type button is selected
      UserTextFormatSettings.getInstance().addImageLineFormatPropertyChangeListener(evt -> {
          if (evt.getPropertyName().equals(LINE_FORMAT_PATH_PROPERTY_CHANGE)) {
              if (evt.getNewValue() == ImageLineFormat.TEXT_FORMAT) {
                  rbTextView.setSelected(true);
              } else if (evt.getNewValue() == ImageLineFormat.HEX_FORMAT) {
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
          public void actionPerformed(ActionEvent e) {
              UserTextFormatSettings.setPathFormat(PathFormat.TEXT_FORMAT);
              bookmarksModel.fireViewChanged();
          }
      });

      // view|Forensic Path Numeric Base|Hex
      rbHex = new JRadioButtonMenuItem("Hexadecimal");
      forensicPathNumericBaseGroup.add(rbHex);
      forensicPathNumericBaseMenu.add(rbHex);
      rbHex.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
              UserTextFormatSettings.setPathFormat(PathFormat.HEX_FORMAT);
              bookmarksModel.fireViewChanged();
          }
      });

      // wire listener to manage which forensic path numeric base is shown in the menu
      //Assumption is since this could change elsewhere
      UserTextFormatSettings.getInstance().addImageLineFormatPropertyChangeListener(new PropertyChangeListener() {
          @Override
          public void propertyChange(PropertyChangeEvent event) {
              if (event.getPropertyName().equals(LINE_FORMAT_PATH_PROPERTY_CHANGE)) {
                  if (event.getNewValue() == PathFormat.TEXT_FORMAT) {
                      rbDecimal.setSelected(true);
                  } else {
                      rbHex.setSelected(true);
                  }
              }
          }
      });

      // view|Feature Font Size
      JMenu featureFontSizeMenu = new JMenu("Feature Font Size");
      view.add(featureFontSizeMenu);

      // view|Feature Font Size|Zoom In
      zoomInMenuItem = new JMenuItem("Zoom In");
      featureFontSizeMenu.add(zoomInMenuItem);
      zoomInMenuItem.addActionListener(_ -> {
          featuresModel.setFontSize(featuresModel.getFontSize() + 1);
          referencedFeaturesModel.setFontSize(featuresModel.getFontSize());
      });

      // view|Feature Font Size|Zoom Out
      zoomOutMenuItem = new JMenuItem("Zoom Out");
      featureFontSizeMenu.add(zoomOutMenuItem);
      zoomOutMenuItem.addActionListener(_ -> {
          int oldFontSize = featuresModel.getFontSize();
          if (oldFontSize > 6) {
              featuresModel.setFontSize(oldFontSize - 1);
              referencedFeaturesModel.setFontSize(featuresModel.getFontSize());
          } else {
              WError.showError("Already at minimum font size of " + oldFontSize + ".",
                      "BEViewer Feature Font Size error", null);
          }
      });

      // view|Feature File View|Normal Size
      normalSizeMenuItem = new JMenuItem("Normal Size");
      featureFontSizeMenu.add(normalSizeMenuItem);
      normalSizeMenuItem.addActionListener(_ -> {
          featuresModel.setFontSize(BEPreferences.DEFAULT_FEATURE_FONT_SIZE);
          referencedFeaturesModel.setFontSize(featuresModel.getFontSize());
      });

      // view|Image Font Size
      JMenu imageFontSizeMenu = new JMenu("Image Font Size");
      view.add(imageFontSizeMenu);

      // view|Image Font Size|Zoom In
      imageFontZoomInMenuItem = new JMenuItem("Zoom In");
      imageFontSizeMenu.add(imageFontZoomInMenuItem);
      imageFontZoomInMenuItem.addActionListener(_ -> imageViewww.setFontSize(imageViewww.getFontSize() + 1));

      // view|Image Font Size|Zoom Out
      imageFontZoomOutMenuItem = new JMenuItem("Zoom Out");
      imageFontSizeMenu.add(imageFontZoomOutMenuItem);
      imageFontZoomOutMenuItem.addActionListener(_ -> {
          int oldFontSize = imageViewww.getFontSize();
          if (oldFontSize > 6) {
              imageViewww.setFontSize(oldFontSize - 1);
          } else {
              WError.showError("Already at minimum font size of " + oldFontSize + ".",
                      "BEViewer Image Font Size Size error", null);
          }
      });

      // view|Image Font Size|Normal Size
      imageFontNormalSizeMenuItem = new JMenuItem("Normal Size");
      imageFontSizeMenu.add(imageFontNormalSizeMenuItem);
      imageFontNormalSizeMenuItem.addActionListener(_ -> imageViewww.setFontSize(BEPreferences.DEFAULT_IMAGE_FONT_SIZE));

      // view|Reports
      JMenu reportsMenu = new JMenu("Reports");
      view.add(reportsMenu);
      // view|Reports|Show Stoplist Files
      cbShowStoplistFiles = new JCheckBoxMenuItem("Show Stoplist Files");
      reportsMenu.add(cbShowStoplistFiles);
      cbShowStoplistFiles.addActionListener(_ -> reportsModel.setIncludeStoplistFiles(cbShowStoplistFiles.getState()));

      // wire ReportsModel's report tree model listener to manage when show stoplist files is enabled
      reportsModel.getTreeModel().addTreeModelListener(new TreeModelListener() {
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
              Enumeration<ReportsModel.ReportTreeNode> e = reportsModel.elements();
              cbShowStoplistFiles.setSelected(reportsModel.isIncludeStoplistFiles());
          }
      });

      // view|Reports|Show Empty Files
      cbShowEmptyFiles = new JCheckBoxMenuItem("Show Empty Files");
      reportsMenu.add(cbShowEmptyFiles);
      cbShowEmptyFiles.addActionListener(_ -> reportsModel.setIncludeEmptyFiles(cbShowEmptyFiles.getState()));

      // wire ReportsModel's report tree model listener to manage when show empty files is enabled
      reportsModel.getTreeModel().addTreeModelListener(new TreeModelListener() {
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
              Enumeration<ReportsModel.ReportTreeNode> e = reportsModel.elements();
              cbShowEmptyFiles.setSelected(reportsModel.isIncludeEmptyFiles());
          }
      });

      // view|Reports|Refresh
      refreshMenuItem = new JMenuItem("Refresh");
      reportsMenu.add(refreshMenuItem);
      refreshMenuItem.addActionListener(_ -> reportsModel.refreshFiles());

      // wire ReportsModel's report tree model listener to manage when show empty files is enabled
      reportsModel.getTreeModel().addTreeModelListener(new TreeModelListener() {
          public void treeNodesChanged(TreeModelEvent e) {
              doRefresh();
          }

          public void treeNodesInserted(TreeModelEvent e) {
              doRefresh();
          }

          public void treeNodesRemoved(TreeModelEvent e) {
              doRefresh();
          }

          public void treeStructureChanged(TreeModelEvent e) {
              doRefresh();
          }

          private void doRefresh() {
              //TODO looks like a bug - reportsModel.doRefresh to reload?
              Enumeration<ReportsModel.ReportTreeNode> e = reportsModel.elements();
          }
      });

      // view|Referenced Features
      JMenu referencedFeaturesMenu = new JMenu("Referenced Features");
      view.add(referencedFeaturesMenu);

      // view|Referenced Features|always visible
      rbReferencedFeaturesVisible = new JRadioButtonMenuItem("Always Visible");
      referencedFeaturesGroup.add(rbReferencedFeaturesVisible);
      referencedFeaturesMenu.add(rbReferencedFeaturesVisible);
      rbReferencedFeaturesVisible.setSelected(!reportSelectionManager.isRequestHideReferencedFeatureView());
      rbReferencedFeaturesVisible.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
              reportSelectionManager.setRequestHideReferencedFeatureView(false);
          }
      });

      // view|Referenced Features|collapsible
      rbReferencedFeaturesCollapsible = new JRadioButtonMenuItem("Collapsed when not Referenced");
      referencedFeaturesGroup.add(rbReferencedFeaturesCollapsible);
      referencedFeaturesMenu.add(rbReferencedFeaturesCollapsible);
      rbReferencedFeaturesCollapsible.setSelected(reportSelectionManager.isRequestHideReferencedFeatureView());
      rbReferencedFeaturesCollapsible.addActionListener(_ -> reportSelectionManager.setRequestHideReferencedFeatureView(true));

      // wire listener to manage which visibility mode is shown in the menu
      reportSelectionManager.addReportSelectionManagerChangedListener(event -> {
          if (event.getPropertyName().equals(ReportSelectionManager.ChangeType.PREFERENCE_CHANGED.name())) {
              if ((boolean) event.getNewValue()) {
                  rbReferencedFeaturesCollapsible.setSelected(true);
              } else {
                  rbReferencedFeaturesVisible.setSelected(true);
              }
          }
      });

      // view|<separator>
      view.addSeparator();

      // view|Selected Feature
      JMenu selectedFeature = new JMenu("Selected Feature");
      view.add(selectedFeature);

      // view|SelectedFeature|Pan to Start of Path
      panToStartMenuItem = new JMenuItem("Pan to Start of Path");
      selectedFeature.add(panToStartMenuItem);
      panToStartMenuItem.addActionListener(_ -> {
          // move to start of feature currently in the image model
          imageModellll.setImageSelection(ForensicPath.getAdjustedPath(
                  imageViewww.getImagePage().pageForensicPath(), 0));
      });
      panToStartMenuItem.setEnabled(false);

      // view|SelectedFeature|Pan to  End of Path
      panToEndMenuItem = new JMenuItem("Pan to End of Path");
      selectedFeature.add(panToEndMenuItem);
      panToEndMenuItem.addActionListener(_ -> {
          // move to end of feature currently in the image model
          ImageModel.ImagePage imagePage = imageViewww.getImagePage();
          String pageForensicPath = imagePage.pageForensicPath();
          long imageSize = imagePage.imageSize();
          long imageEndOffset = (imageSize > 0) ? imageSize - 1 : 0;
          imageModellll.setImageSelection(ForensicPath.getAdjustedPath(
                  pageForensicPath, imageEndOffset));
      });
      panToEndMenuItem.setEnabled(false);

      // view|Selected Feature|<separator>
      selectedFeature.addSeparator();

      // view|Selected Feature|report.xml File
      showReportFileMenuItem = new JMenuItem("Show report.xml File");
      selectedFeature.add(showReportFileMenuItem);
      showReportFileMenuItem.addActionListener(_ -> {
          // get the currently selected feature line
          FeatureLine featureLine = featureLineSelectionManager.getFeatureLineSelection();
          if (featureLine.featuresFile() == null) {
              WError.showError("A Feature must be selected before viewing the Report fileMenu.",
                      "BEViewer Selected Feature error", null);
          } else {
              try {
                  File reportFile = new File(featureLine.featuresFile().getParentFile(), "report.xml");
                  URL url = reportFile.toURI().toURL();
                  new WURL("Bulk Extractor Viewer Report fileMenu " + reportFile, url);
              } catch (Exception exc) {
                  WError.showError("Unable to read report.xml fileMenu.",
                          "BEViewer Read error", exc);
              }
          }
      });
      showReportFileMenuItem.setEnabled(false);

      // wire action for all view|selected feature items
      featureLineSelectionManager.addFeatureLineSelectionManagerChangedListener((_, _, newSelectedFeatureLine) -> {
          boolean hasSelection = !isBlank(newSelectedFeatureLine);
          panToStartMenuItem.setEnabled(hasSelection);
          panToEndMenuItem.setEnabled(hasSelection);
          showReportFileMenuItem.setEnabled(hasSelection);
      });

      // bookmarks
      JMenu bookmarks = new JMenu("Bookmarks");
      add(bookmarks);

      // bookmarks|Bookmark selected Feature
      addBookmarkMenuItem = new JMenuItem("Bookmark selected Feature", ADD_BOOKMARK_16_LIGHT);    // ...
      bookmarks.add(addBookmarkMenuItem);
      addBookmarkMenuItem.setAccelerator(KEYSTROKE_B);
      addBookmarkMenuItem.addActionListener(_ -> {
          FeatureLine selectedFeatureLine = featureLineSelectionManager.getFeatureLineSelection();
          bookmarksModel.addElement(selectedFeatureLine);
      });

      // a feature line has been selected and may be added as a bookmark
      featureLineSelectionManager.addFeatureLineSelectionManagerChangedListener((evt, oldValue, newSelectedFeatureLine) -> {
          // same as in BEToolbar
          // enabled if feature line is not blank and is not already bookmarked
          addBookmarkMenuItem.setEnabled(!isBlank(newSelectedFeatureLine) && !bookmarksModel.contains(newSelectedFeatureLine));
      });
      FeatureLine selectedFeatureLine = featureLineSelectionManager.getFeatureLineSelection();
      addBookmarkMenuItem.setEnabled(!isBlank(selectedFeatureLine)
              && !bookmarksModel.contains(selectedFeatureLine));

      // bookmarks|Manage Bookmarks
      manageBookmarksMenuItem = new JMenuItem("Manage Bookmarks…", MANAGE_BOOKMARKS_16_LIGHT);    // ...
      bookmarks.add(manageBookmarksMenuItem);
      manageBookmarksMenuItem.setAccelerator(KEYSTROKE_M);
      manageBookmarksMenuItem.addActionListener(_ -> WManageBookmarks.openWindow());
      // wire listener to manage when bookmarks are available for management
      bookmarksModel.addListDataListener(new ListDataListener() {
          public void contentsChanged(ListDataEvent e) {
              manageBookmarksMenuItem.setEnabled(!bookmarksModel.isEmpty());
          }

          public void intervalAdded(ListDataEvent e) {
              manageBookmarksMenuItem.setEnabled(!bookmarksModel.isEmpty());
          }

          public void intervalRemoved(ListDataEvent e) {
              manageBookmarksMenuItem.setEnabled(!bookmarksModel.isEmpty());
          }
      });
      manageBookmarksMenuItem.setEnabled(!bookmarksModel.isEmpty());

      // tools
      JMenu tools = new JMenu("Tools");
      add(tools);

      // tools|Run bulk_extractor
      runBulkExtractorMenuItem = new JMenuItem("Run bulk_extractor…", RUN_BULK_EXTRACTOR_16_LIGHT);    // ...
      tools.add(runBulkExtractorMenuItem);
      runBulkExtractorMenuItem.setAccelerator(KEYSTROKE_R);
      runBulkExtractorMenuItem.addActionListener(_ -> WScan.openWindow(new ScanSettings()));

      // tools|bulk_extractor Run Queue...
      openRunQueueMenuItem = new JMenuItem("bulk_extractor Run Queue…");
      tools.add(openRunQueueMenuItem);
      openRunQueueMenuItem.addActionListener(_ -> WScanSettingsRunQueue.openWindow());

      // help
      JMenu help = new JMenu("Help");
      add(help);

      // help|About
      aboutBulkExtractorViewerMenuItem = new JMenuItem("About Bulk Extractor Viewer " + VersionInformation.getVersion(), BEIcons.HELP_ABOUT_16_LIGHT);
      help.add(aboutBulkExtractorViewerMenuItem);
      aboutBulkExtractorViewerMenuItem.setAccelerator(KEYSTROKE_A);
      aboutBulkExtractorViewerMenuItem.addActionListener(_ -> WAbout.openWindow());

      // help|Check Versions
      checkBulkExtractorVersionMenuItem = new JMenuItem("Check Bulk Extractor Version ");
      help.add(checkBulkExtractorVersionMenuItem);
      checkBulkExtractorVersionMenuItem.addActionListener(_ -> {
          try (var bulkExtractorVersionReader = new BulkExtractorVersionReader()) {
              bulkExtractorVersionReader.displayVersion();
          } catch (Exception ex) {
              WError.showError("Unable to display version information",
                      "BEViewer Read error", ex);
          }
      });

      // help|<separator>
      help.addSeparator();

      // help|diagnostics
      JMenu diagnostics = new JMenu("Diagnostics");
      help.add(diagnostics);

      // help|diagnostics|Show Log
      showLogMenuItem = new JMenuItem("Show Log");
      diagnostics.add(showLogMenuItem);
      showLogMenuItem.setAccelerator(KEYSTROKE_L);
      showLogMenuItem.addActionListener(_ -> logViewer = new LogViewer());

    // help|diagnostics|Clear Log
      clearLogMenuItem = new JMenuItem("Clear Log");
      diagnostics.add(clearLogMenuItem);
      clearLogMenuItem.addActionListener(e -> {
          if(logViewer != null) {
              logViewer.clearWindow();
          }
      });

      // help|diagnostics|Copy Log to System Clipboard
      copyLogMenuItem = new JMenuItem("Copy Log to System Clipboard");
      diagnostics.add(copyLogMenuItem);
      copyLogMenuItem.addActionListener(_ -> {
          // clear the selection manager
          rangeSelectionManager.clear();

          // get the Transferable log
          Transferable log = new StringSelection(logViewer.getTextAreaContent());

          // copy the log to the system clipboard and, if available, to the selection clipboard
          RangeSelectionManager.setSystemClipboard(log);
          RangeSelectionManager.setSelectionClipboard(log);
      });

      // help|diagnostics|<separator>
      diagnostics.addSeparator();

      // help|diagnostics|run tests
      runTestsMenuItem = new JMenuItem("Run Tests…");    // ...
      runTestsMenuItem.setEnabled(false);
      diagnostics.add(runTestsMenuItem);
      openReportMenuItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
              //new WTest();
              //NO-OP for now, enabled via system prop?
          }
      });
  }

    /**
     * Close a Report.
     */
    public void closeReport(ReportsModel.ReportTreeNode reportTreeNode) {
        // remove the Report from the reports model
        // note that this will notify tree selection listener,
        // resulting in reportSelectionManager being notified,
        // which will set the features models and views to null
        reportsModel.remove(reportTreeNode);

        // clear features in this report from the navigation history
        bookmarksModel.removeAssociatedFeatureLines(reportTreeNode);

        // clear the image selection if it is from this report
        FeatureLine featureLine = featureLineSelectionManager.getFeatureLineSelection();
        if (isFromReport(featureLine, reportTreeNode)) {
            featureLineSelectionManager.setFeatureLineSelection(null); //instead of a mutable FeatureLine new FeatureLine()
        }
    }

    /**
     * Close all Reports.
     */
    public void closeAllReports() {
        // clear all bookmarks
        bookmarksModel.clear();

        // close all Reports in the reports model
        reportsModel.clear();

        // clear any image selection
        featureLineSelectionManager.setFeatureLineSelection(null); //instead of a mutable FeatureLine new FeatureLine()

        // clear user highlighting
        userHighlightModel.setHighlightBytes(new byte[0]);
        userHighlightModel.setHighlightMatchCase(true);

        // clear feature filter text
        featuresModel.setFilterBytes(new byte[0]);
        featuresModel.setFilterMatchCase(true);
    }

    public int showConfirmation(Component parentComponent) {
        // Define the message and dialog title
        String message = "Are you sure? This will clear all report information and saved state.";
        String title = "Confirm Action";

        return JOptionPane.showConfirmDialog(
                parentComponent,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, // Use QUESTION_MESSAGE to show a standard icon placeholder
                BEIcons.WARNING_16_LIGHT // Pass the custom ImageIcon here
        );

    }
}

