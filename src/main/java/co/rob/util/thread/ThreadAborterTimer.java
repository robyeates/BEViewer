package co.rob.util.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Places a timer on a process and kills the process if the timer times out.
 */
@Deprecated
public class ThreadAborterTimer extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ThreadAborterTimer.class);

    private final Process process;
    private final int msDelay;
    private boolean isAborted = false;

    /**
     * Cancel the timeout thread.
     */
    public synchronized void cancel() {
        isAborted = true;
    }

    /**
     * Creates a timer that will kill the process after the delay unless aborted.
     */
    public ThreadAborterTimer(Process process, int msDelay) {
        this.process = process;
        this.msDelay = msDelay;
        this.start();
    }

    // this should be private.
    public void run() {
        try {
            sleep(msDelay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // if cancel() was not called, force kill
        if (!isAborted) {

            // timeout so kill process
            logger.info("ThreadAborterTimer process timeout on process '{}'", process);
            process.destroy();

            // verify that the process dies
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                logger.error("ThreadAborterTimer timeout failure", e);
            }
        }
    }
}

