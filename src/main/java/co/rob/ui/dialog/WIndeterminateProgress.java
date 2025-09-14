package co.rob.ui.dialog;

import co.rob.BEViewer;
import co.rob.ui.WarmUpCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A pop-up window for showing indeterminate progress.
 * This window has no user controls.
 * Visibility is enabled after a delay.
 * Interfaces are thread-safe.
 */
@WarmUpCandidate
public class WIndeterminateProgress {

    private static final Logger logger = LoggerFactory.getLogger(WIndeterminateProgress.class);

    private static final int DELAY = 300; // ms
    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "WIndeterminateProgress-Scheduler");
                t.setDaemon(true);
                return t;
            });
    private final AtomicBoolean active = new AtomicBoolean(false);    // synchronized

    private final JDialog window;
    private final JLabel label = new JLabel();
    private ScheduledFuture<?> scheduledShow;

    /**
     * Creates a pop-up window for showing image reader progress.
     *
     * @param title the window title
     */
    public WIndeterminateProgress(String title) {
        // initialize the progress window
        window = new JDialog(BEViewer.getBEWindow(), title, false);
        // NOTE: Removed setAlwaysOnTop because Error windows get covered.
        // It would be nice to have a way to keep this progress window near the top.
        //    window.setAlwaysOnTop(true);
        window.setFocusableWindowState(false);    // don't take focus when visible
        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        Container pane = window.getContentPane();
        GridBagConstraints c;

        // use GridBagLayout with GridBagConstraints
        pane.setLayout(new GridBagLayout());

        // (0,0) feature line label
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        label.setMinimumSize(new Dimension(400, 16));
        label.setPreferredSize(new Dimension(400, 16));
        pane.add(label, c);

        // (0,1) JProgressBar
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.FIRST_LINE_START;

        // create the progress bar component
        JProgressBar bar = new JProgressBar();
        bar.setStringPainted(true);
        bar.setString("Reading");
        bar.setIndeterminate(true);  // animated
        bar.setMinimumSize(new Dimension(100, 16));
        bar.setPreferredSize(new Dimension(100, 16));

        // add the progress bar component
        pane.add(bar, c);

        // pack the window
        window.pack();
    }

    /**
     * Starts the progress window.
     * The window appears after delay
     *
     * @param text text to show in the progress window
     */
    public synchronized void startProgress(String text) {
        // set label text to indicate current progress action
        label.setText(text);

        // either update existing progress or start new progress
        if (active.get()) {
            window.toFront();
            // throw new RuntimeException("invalid state");
        } else {
            // set active and set thread to show progress after delay
            active.set(true);
            // schedule showing the window after DELAY
            scheduledShow = scheduler.schedule(() -> {
                if (active.get()) {
                    SwingUtilities.invokeLater(() -> {
                        window.setLocation(BEViewer.getBEWindowLocation());
                        window.setVisible(true);
                    });
                }
            }, DELAY, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Closes the progress window.
     */
    public synchronized void stopProgress() {
        if (!active.get()) {
            throw new IllegalStateException("Progress not active");
        }

        if (scheduledShow != null) {
            scheduledShow.cancel(false);
            scheduledShow = null;
        }

        SwingUtilities.invokeLater(() -> window.setVisible(false));
        active.set(false);
    }

    /** Shutdown the executor globally if needed */
    public static void shutdownScheduler() {
        scheduler.shutdown();
    }
}

