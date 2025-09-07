package co.rob.consumer;

import co.rob.DaggerContext;
import co.rob.pojo.scan.ScanSettings;
import co.rob.state.ScanSettingsListModel;
import co.rob.ui.dialog.WError;
import co.rob.ui.dialog.WScanProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.io.IOException;
import java.util.concurrent.locks.LockSupport;

/**
 * The <code>ScanSettingsConsumer</code> consumes Scan Settings jobs
 * one at a time as they become available in the scan settings run queue.
 * <p>
 * The consumer loops, consuming jobs, until it parks.
 * The provider provides unpark permits after enqueueing jobs.
 * This policy keeps the consumer going.
 * <p>
 * This object must be initialized after ScanSettingsListModel.
 */
//@SuppressWarnings("InfiniteLoopStatement")
public class ScanSettingsConsumer extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ScanSettingsConsumer.class);

    private static ScanSettingsConsumer scanSettingsConsumer;
    private static boolean isPaused = false;

    private final ScanSettingsListModel scanSettingsListModel;

    /**
     * Just loading the constructor starts the consumer.
     */
    public ScanSettingsConsumer() {
        scanSettingsListModel = DaggerContext.get().scanSettingsListModel();
        scanSettingsConsumer = this;
        start();
    }

    /**
     * Pause the consumer so that it does not start another buk_extractor run
     * or restart the consumer.
     */
    public synchronized static void pauseConsumer(boolean doPause) {
        if (doPause) {
            isPaused = true;
        } else {
            isPaused = false;
            LockSupport.unpark(ScanSettingsConsumer.scanSettingsConsumer);
        }
    }

    // this runs forever, once through per semaphore permit acquired
    public void run() {

        // register self to listen for added jobs
        scanSettingsListModel.addListDataListener(new ListDataListener() {
            public void contentsChanged(ListDataEvent e) {
                // not used;
            }

            public void intervalAdded(ListDataEvent e) {
                // consume the item by waking up self
                LockSupport.unpark(ScanSettingsConsumer.scanSettingsConsumer);
            }

            public void intervalRemoved(ListDataEvent e) {
                // not used;
            }
        });

        while (true) {
            // wait for signal that a job may be available
            LockSupport.park();

            if (isPaused) {
                // to pause, simply restart at top of loop.
                // Use pauseConsumer() to restart.
                continue;
            }

            // get a ScanSettings run job from the producer
            ScanSettings scanSettings = scanSettingsListModel.remove();
            if (scanSettings == null) {
                // the producer is not ready, so restart at top of loop.
                continue;
            }

            // log the scan command of the job that is about to be run
            logger.info("ScanSettingsConsumer starting bulk_extractor run: '{}'", scanSettings.getCommandString());

            // start the bulk_extractor process
            Process process;
            try {
                // NOTE: It would be nice to use commandString instead, but Runtime
                // internally uses array and its string tokenizer doesn't manage
                // quotes or spaces properly, so we use array.
                process = Runtime.getRuntime().exec(scanSettings.getCommandArray());

            } catch (IOException e) {
                // something went wrong starting bulk_extractor so alert and abort
                WError.showErrorLater("bulk_extractor Scanner failed to start command\n'" + scanSettings.getCommandString() + "'", "bulk_extractor failure", e);

                // despite the failure to start, continue to consume the queue
                LockSupport.unpark(ScanSettingsConsumer.scanSettingsConsumer);
                continue;
            }

            // open a dedicated instance of WScanProgress for showing progress and
            // status of the bulk_extractor process
            WScanProgress.openWindow(scanSettings, process);

            // wait for the bulk_extractor scan process to finish
            // The process terminates by itself or by calling process.destroy().
            try {
                process.waitFor();
            } catch (InterruptedException ie) {
                throw new RuntimeException("unexpected event");
            }

            // unpark to try another run if the producer has one available
            LockSupport.unpark(ScanSettingsConsumer.scanSettingsConsumer);
        }
    }
}

