package co.rob;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The <code>BEViewer</code> class provides the main entry
 * for the Bulk Extractor Viewer application.
 */
public class BEViewer {
  public static final int GUI_EDGE_PADDING = 4;
  public static final int GUI_X_PADDING = 3;
  public static final int GUI_Y_PADDING = 8;
  public static final Dimension BUTTON_SIZE = new Dimension(18, 18);
  public static final int SPLIT_PANE_DIVIDER_SIZE = 4;

  // public handles
  public static final ReportsModel reportsModel = new ReportsModel();
  public static final ReportSelectionManager reportSelectionManager = new ReportSelectionManager();
  public static final FeaturesModel featuresModel = new FeaturesModel(
                           reportSelectionManager, FeaturesModel.ModelType.FEATURES_OR_HISTOGRAM);
  public static final FeaturesModel referencedFeaturesModel = new FeaturesModel(
                           reportSelectionManager, FeaturesModel.ModelType.REFERENCED_FEATURES);
  public static final FeatureBookmarksModel featureBookmarksModel
                    = new FeatureBookmarksModel();
  public static final FeatureLineSelectionManager featureLineSelectionManager
                    = new FeatureLineSelectionManager();
  public static final ImageModel imageModel = new ImageModel(featureLineSelectionManager);
  public static final UserHighlightModel userHighlightModel = new UserHighlightModel();
  public static final ImageView imageView = new ImageView(imageModel, userHighlightModel);
  public static final RangeSelectionManager rangeSelectionManager = new RangeSelectionManager();
  public static final FeatureNavigationComboBoxModel featureNavigationComboBoxModel
                    = new FeatureNavigationComboBoxModel(featureLineSelectionManager);
  public static final ClassificationManager classificationManager = new ClassificationManager();

  // toolbars
  public static final BEShortcutsToolbar shortcutsToolbar = new BEShortcutsToolbar();
  public static final BEHighlightToolbar highlightToolbar = new BEHighlightToolbar();

  // panes
  public static ReportsPane reportsPane;
  public static FeaturesPane featuresPane;
  public static NavigationPane navigationPane;

  // state
  public static final boolean isWindows;
  public static final boolean isMac;
  static {
    // identify the system BEViewer is running on: mac, windows, else likely linux
    String lcOSName = System.getProperty("os.name").toLowerCase();
    if (lcOSName.startsWith("windows")) {
      isWindows = true; isMac = false;
    } else if (lcOSName.startsWith("mac")) {
      isWindows = false; isMac = true;
    } else {
      isWindows = false; isMac = false;
    }
  }

  private static JFrame frame;

  /**
   * Returns the main window BEViewer runs from.
   * Popup windows may use this as their parent.
   */
  public static JFrame getBEWindow() {
    return frame;
  }

  private static final class Starter implements Runnable {
    private final String[] args;
    private Starter(String[] args) {
      this.args = args;
    }

    public void run() {
      // start BEViewer from Swing thread
      new BEViewer();

// hack in classifiction labels for demo
//classificationManager.addClassification("fun");

      // now that the GUI is initialized, set the initial state from the last run
      BEPreferences.loadPreferences();

      // now run a command, if provided
      if (args.length > 0) {
        BEArgsParser.parseArgs(args);
      }
    }
  }
 
  /**
   * Provides the main entry to the Bulk Extractor Viewer.
   * @param args not used
   */
  public static void main(String args[]) {
    // for thread safety, run from the Swing event dispatch queue
    SwingUtilities.invokeLater(new Starter(args));
  }

  // initialize data models, build the GUI, then load the user's preferences
  private BEViewer() {
    // make custom UI objects visible
    UIManager.put(FeaturesUI.UI_CLASS_ID, BasicFeaturesUI.class.getName());
    UIManager.put(ImageUI.UI_CLASS_ID, BasicImageUI.class.getName());

    // create the main JFrame
 // getVersion violates Macintosh minimal window guideline?
//    frame = new JFrame("Bulk Extractor Viewer V" + Config.VERSION);
    frame = new JFrame("Bulk Extractor Viewer");

    // set the logger
    WLog.log("Bulk Extractor Viewer Version  0.0.1-Alpha");
    WLog.setExceptionHandling();

    // set native Look and Feel
    // set for Mac OS X, see http://www.devdaily.com/apple/mac/java-mac-native-look/
    if (isMac) {
      // macintosh menubar goes in the Mac screen menubar
      System.setProperty("apple.laf.useScreenMenuBar", "true");

// sorry, this doesn't work:      // show application name instead of main class name
// sorry, this doesn't work:      System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Bulk Extractor Viewer");
    }

    try {
      // native look and feel
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch(Exception e) {
      WLog.log("Error setting native LAF: " + e);
    }

    // load the main JFrame contents
    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);	// close via handler
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        doClose();
      }
    });
    addComponents(frame.getContentPane());
    frame.setJMenuBar(new BEMenus()); // add the menubar to the frame
    KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
    frame.pack();
    frame.setVisible(true);
  }

  /**
   * Close a Report.
   */
  public static void closeReport(ReportsModel.ReportTreeNode reportTreeNode) {
    // remove the Report from the reports model
    // note that this will notify tree selection listener,
    // resulting in reportSelectionManager being notified,
    // which will set the features models and views to null
    reportsModel.remove(reportTreeNode);

    // clear features in this report from the navigation history
    featureNavigationComboBoxModel.removeAssociatedFeatures(reportTreeNode);

    // clear features in Bookmarks that are associated with this report
    featureBookmarksModel.removeAssociatedBookmarks(reportTreeNode);

    // close the associated opened image readers
    imageModel.closeImageReader(reportTreeNode.reportImageFile);
  }

  /**
   * Close all Reports.
   */
  public static void closeAllReports() {
    // clear the navigation history
    featureNavigationComboBoxModel.removeAllFeatures();

    // clear the Bookmark list
    featureBookmarksModel.removeAllBookmarks();

    // close all Reports in the reports model
    reportsModel.clear();

    // close any opened image readers
    imageModel.closeAllImageReaders();

    // clear user highlighting
    BEViewer.userHighlightModel.setHighlightBytes(new byte[0]);
    BEViewer.userHighlightModel.setHighlightMatchCase(true);

    // clear feature filter text
    BEViewer.featuresModel.setFilterBytes(new byte[0]);
    BEViewer.featuresModel.setFilterMatchCase(true);
  }

  /**
   * Print range
   */
  public static void printRange() {
    // this should only be called when valid
    if (!rangeSelectionManager.hasSelection()) {
      throw new RuntimeException("invalid request");
    }

    // forward print request to the appropriate range printer
    int minSelectionIndex = rangeSelectionManager.getMinSelectionIndex();
    int maxSelectionIndex = rangeSelectionManager.getMaxSelectionIndex();
    if (rangeSelectionManager.getProvider() == imageView) {
      new ImageRangePrinter().printImageRange(imageView, minSelectionIndex, maxSelectionIndex);
    } else if (rangeSelectionManager.getProvider() == featuresModel) {
      new FeatureRangePrinter().printFeatureRange(featuresModel, minSelectionIndex, maxSelectionIndex);
    } else if (rangeSelectionManager.getProvider() == referencedFeaturesModel) {
      new FeatureRangePrinter().printFeatureRange(referencedFeaturesModel, minSelectionIndex, maxSelectionIndex);
    } else {
      throw new RuntimeException("invalid request");
    }
  }

  /**
   * Close BEViewer.
   */
  public static void doClose() {
    // save preferences then exit
    BEPreferences.savePreferences();
    System.exit(0);
  }

  // add the DirectoryPane, FeaturesPane, and NavigationPane to split panes within the JFrame
  private void addComponents(Container pane) {
    pane.setLayout(new BorderLayout());
    // add the Tool bar to the top
    pane.add(getToolbars(), BorderLayout.NORTH);

    // add the splitpanes in the center
    JSplitPane innerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
                                               featuresPane = new FeaturesPane(),
                                               navigationPane = new NavigationPane());
    innerSplitPane.setBorder(null);	// per recommendation from SplitPaneDemo2
    innerSplitPane.setDividerSize(SPLIT_PANE_DIVIDER_SIZE);
    JSplitPane outerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
                                               reportsPane = new ReportsPane(),
                                               innerSplitPane);
    outerSplitPane.setDividerSize(SPLIT_PANE_DIVIDER_SIZE);
    pane.add(outerSplitPane, BorderLayout.CENTER);
  }

  private Container getToolbars() {
    Container c = new Container();
    c.setLayout(new BorderLayout());
    c.add(shortcutsToolbar, BorderLayout.NORTH);
    c.add(highlightToolbar, BorderLayout.SOUTH);
    return c;
  }
}

