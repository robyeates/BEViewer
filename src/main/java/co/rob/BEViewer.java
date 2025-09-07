package co.rob;

import co.rob.state.BEPreferences;
import co.rob.ui.components.*;
import co.rob.util.BEArgsParser;
import co.rob.util.VersionInformation;
import co.rob.util.log.UncaughtExceptionHandler;
import com.formdev.flatlaf.FlatIntelliJLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

import static co.rob.util.BEConstants.SPLIT_PANE_DIVIDER_SIZE;

/**
 * The <code>BEViewer</code> class provides the main entry
 * for the Bulk Extractor Viewer application.
 */
public class BEViewer {

    private static final Logger logger = LoggerFactory.getLogger(BEViewer.class);

    private static JFrame frame;
    private static BEToolbar toolbar;

    /**
     * Returns the main window BEViewer runs from.
     * Popup windows may use this as their parent.
     */
    public static Point getBEWindowLocation() {
        return frame.getLocation();
    }

    @Deprecated(since = "TO BE REMOVED TO AVOID LEAKS")
    public static Frame getBEWindow() {
        return frame;
    }

    /**
     * Provides the main entry to the Bulk Extractor Viewer.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        // for thread safety, run from the Swing event dispatch queue
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
        SwingUtilities.invokeLater(new Starter(args));
    }

    // initialize data models, build the GUI, then load the user's preferences
    private BEViewer() throws UnsupportedLookAndFeelException {
        // make custom UI objects visible
        //🛠️ Must have the qualified name now items are in packages
        UIManager.put(FeaturesUI.UI_CLASS_ID, BasicFeaturesUI.class.getCanonicalName());
        UIManager.put(ImageUI.UI_CLASS_ID, BasicImageUI.class.getCanonicalName());

        FlatIntelliJLaf.setup();
        UIManager.setLookAndFeel(new FlatIntelliJLaf());

        // create the main JFrame
        frame = new JFrame("Bulk Extractor Viewer v" + VersionInformation.getVersion());

        // start the logger
        logger.info("Bulk Extractor Viewer Version [{}]", VersionInformation.getVersion());

        // load the main JFrame contents
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);    // close via handler
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                doClose();
            }
        });

        toolbar = new BEToolbar(() -> SwingUtilities.updateComponentTreeUI(frame));
        addComponents(frame.getContentPane());//  ;
        frame.setJMenuBar(new BEMenus(toolbar)); // add the menubar to the frame
        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void doClose() {
        System.exit(0);
    }

    // add the DirectoryPane, FeaturesPane, and NavigationPane to split panes within the JFrame
    private void addComponents(Container pane) {
        pane.setLayout(new BorderLayout());
        // add the Tool bar to the top
        pane.add(setToolbar(toolbar), BorderLayout.NORTH);

        // add the split panes in the center
        JSplitPane innerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, DaggerContext.get().featuresPane(), DaggerContext.get().navigationPane());
        innerSplitPane.setBorder(null);    // per recommendation from SplitPaneDemo2
        innerSplitPane.setDividerSize(SPLIT_PANE_DIVIDER_SIZE);
        JSplitPane outerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, DaggerContext.get().reportsPane(), innerSplitPane);
        outerSplitPane.setDividerSize(SPLIT_PANE_DIVIDER_SIZE);
        pane.add(outerSplitPane, BorderLayout.CENTER);
    }

    private Container setToolbar(BEToolbar toolbar) {
        Container c = new Container();
        c.setLayout(new BorderLayout());
        c.add(new Container(), BorderLayout.NORTH); // force restart of shading
        c.add(toolbar, BorderLayout.CENTER);
        return c;
    }

    private static final class Starter implements Runnable {
        private final String[] args;

        private Starter(String[] args) {
            this.args = args;
        }

        public void run() {
            // start BEViewer from Swing thread
            try {
                new BEViewer();
            } catch (UnsupportedLookAndFeelException ignored) {
            }

            // now that the GUI is initialized, set the initial state from the last run
            Consumer<Boolean> toolbarConsumer = visible -> toolbar.setVisible(visible);
            BEPreferences.loadPreferences(toolbarConsumer);

            // now run a command, if provided
            if (args.length > 0) {
                BEArgsParser.parseArgs(args);
            }
        }
    }
}

