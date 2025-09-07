package co.rob.api;

import co.rob.ui.dialog.WError;
import co.rob.util.VersionInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The <code>BulkExtractorVersionChecker</code> class verifies the version of bulk_extractor
 * against the expected value.  The version must be in the format "#.#.#".
 */
public class BulkExtractorVersionReader implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(BulkExtractorVersionReader.class);

    private static final int DELAY = 60 * 1000; // 12 is good for testing
    private static final String VERSION_PREFIX = "bulk_extractor ";
    private final ExecutorService executor;
    private final Process process;

    public BulkExtractorVersionReader() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor(); // TODO SRP - HTTP & Process management in one class feels too big
        process = createBulkExtractorProcess();
    }

    private Process createBulkExtractorProcess() {
        final Process process;
        // open interactive http channel with bulk_extractor
        // use "bulk_extractor -p -http <image_file>"
        String[] cmd = {"bulk_extractor", "-V"};

        logger.info("BulkExtractorFileReader starting bulk_extractor process");
        logger.info("BulkExtractorFileReader cmd: {} {}", cmd[0], cmd[1]);
        try {
            process = new ProcessBuilder(cmd).redirectErrorStream(false) // Keep stderr separate for logging
                    .start();
        } catch (IOException e) {
            WError.showErrorLater("Unable to start the bulk_extractor reader.", "Error getting Image", e);
            throw new IllegalStateException("Failed to start bulk_extractor process.", e);
        }
        // Handle stderr in a dedicated virtual thread to surface errors in client logs
        executor.submit(() -> {
            try (var errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                errorReader.lines().forEach(line -> logger.error("BulkExtractorFileReader error from bulk_extractor process: {}", line));
            } catch (IOException e) {
                logger.error("Error reading from stderr: {}", e.getMessage());
            }
        });
        //TODO log startup outcome
        return process;
    }

    /**
     * Displays the bulk_extractor and BEViewer versions
     */
    public void displayVersion() {
        executor.submit(() -> {
            try (var inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String input = inputReader.readLine();
                if ((input != null) && input.startsWith(VERSION_PREFIX)) {
                    String bulk_extractorVersion = input.substring(VERSION_PREFIX.length()).trim();
                    logger.info("BulkExtractorVersionReader.readVersion: bulk_extractor version {}", bulk_extractorVersion);
                    WError.showMessageLater("Versions:\nBulk Extractor Viewer: " + VersionInformation.getVersion() +
                                    "\nbulk_extractor: " + bulk_extractorVersion,
                            "Version Information");
                } else {
                    logger.error("Error in reading bulk_extractor version: '{}'", input);
                }
            } catch (IOException e) {
                logger.error("Error reading from stdin: {}", e.getMessage());
            }
        });
    }

    @Override
    public void close() throws Exception {
        logger.info("BulkExtractorFileReader.close: closing process.");
        if (process.isAlive()) {
            process.destroy();
        }
        try {
            boolean terminated = process.waitFor(DELAY, TimeUnit.MILLISECONDS);
            if (!terminated) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Interrupted while waiting for process to terminate.");
        } finally {
            // Shut down the executor service
            executor.shutdown();
        }
        logger.info("BulkExtractorFileReader.close: process terminated.");
    }

}

