package co.rob.io;

import co.rob.pojo.scan.Scanner;
import co.rob.util.thread.ThreadAborterTimer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * The <code>BulkExtractorScanListReader</code> class sets an array of scanners
 * indicating their name and whether they are enabled by default.
 */
public class BulkExtractorScanListReader {

    private static final Logger logger = LoggerFactory.getLogger(BulkExtractorScanListReader.class);

    private BulkExtractorScanListReader() {
    }

    /**
     * Read and set the scan list.
     */
    public static List<Scanner> readScanList(boolean usePluginDirectories, String pluginDirectories) throws IOException {
        // start the scan list reader process
        // cmd
        String[] cmd;
        if (usePluginDirectories) {
            // plugin directory, may not be supported by bulk_extractor yet
            String[] pluginDirectoriesArray = pluginDirectories.split("\\|");
            cmd = new String[2 + pluginDirectoriesArray.length * 2];
            cmd[0] = "bulk_extractor";
            cmd[1] = "-h";

            // put in plugin directory request for each plugin directory specified
            for (int i = 0; i < pluginDirectoriesArray.length; i++) {
                cmd[2 + i * 2] = "-P";
                cmd[3 + i * 2] = pluginDirectoriesArray[i];
            }

        } else {
            // without plugin directory
            cmd = new String[2];
            cmd[0] = "bulk_extractor";
            cmd[1] = "-h";
        }

        // run exec
        Process process = Runtime.getRuntime().exec(cmd);
        BufferedReader readFromProcess = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorFromProcess = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        // start the abort timer
        final int DELAY = 5000; // ms.  This should never happen, but if it does, it prevents a hang.
        ThreadAborterTimer aborter = new ThreadAborterTimer(process, DELAY);

        // read the scanners the old way from stderr
        ScanListReaderThread stderrThread = new ScanListReaderThread(errorFromProcess);
        stderrThread.start();

        // read the scanners the new way from stdout
        ScanListReaderThread stdoutThread = new ScanListReaderThread(readFromProcess);
        stdoutThread.start();

        // wait for the bulk_extractor process to terminate
        try {
            process.waitFor();
        } catch (InterruptedException ie1) {
            logger.error("BulkExtractorVersionReader process interrupted");
        }

        // wait for stderr Thread to finish
        try {
            stderrThread.join();
        } catch (InterruptedException ie2) {
            throw new RuntimeException("unexpected event");
        }

        // wait for stdout Thread to finish
        try {
            stdoutThread.join();
        } catch (InterruptedException ie3) {
            throw new RuntimeException("unexpected event");
        }

        // scanners to be returned
        List<Scanner> scanners = getScanners(stderrThread, stdoutThread);

        // cancel the aborter timer
        aborter.cancel();

        return scanners;
    }

    @NotNull
    private static List<Scanner> getScanners(ScanListReaderThread stderrThread, ScanListReaderThread stdoutThread) {
        List<Scanner> scanners;

        // set scanners based on which thread obtained them
        if (!stderrThread.scanners.isEmpty()) {
            scanners = stderrThread.scanners;

        } else if (!stdoutThread.scanners.isEmpty()) {
            scanners = stdoutThread.scanners;
        } else {
            // read effort failed
            scanners = new ArrayList<>();
        }
        // WLog.log("BulkExtractorScanListReader.readScanList Number of scanners (stderr for v1.2): " + scanners.size());
        return scanners;
    }

    private static final class ScanListReaderThread extends Thread {
        private final BufferedReader bufferedReader;
        public List<Scanner> scanners = new ArrayList<>();

        public ScanListReaderThread(BufferedReader bufferedReader) {
            this.bufferedReader = bufferedReader;
        }

        public void run() {
            while (true) {

                try {
                    // blocking wait until EOF
                    String input = bufferedReader.readLine();
                    if (input == null) {
                        break;
                    }

                    // break input into tokens
                    String[] tokens = input.split("\\s+");

                    // parse tokens for scanner names, recognizing if they are enabled or disabled by default
                    if (tokens.length >= 3) {

                        // do not accept instructions as valid scanner
                        if (tokens[2].equals("<scanner>")) {
                            continue;
                        }

                        // accept if "-e" or "-x"
                        if (tokens[0].isEmpty() && tokens[1].equals("-e")) {  // disabled by default
                            scanners.add(new Scanner(tokens[2], false));
                        } else if (tokens[0].isEmpty() && tokens[1].equals("-x")) {  // enabled by default
                            scanners.add(new Scanner(tokens[2], true));
                        }
                        // this input line does not define a scanner, so ignore it.
                    }
                } catch (IOException e) {
                    logger.error("BulkExtractorScanListReader.ScanListReaderThread {} aborting.", this);
                    break;
                }
            }
        }
    }
}

