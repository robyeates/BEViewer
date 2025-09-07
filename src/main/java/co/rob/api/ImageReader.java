package co.rob.api;

import co.rob.ui.dialog.WError;
import co.rob.util.ForensicPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * The <code>BulkExtractorFileReader</code> class provides path reading services
 * using the bulk_extractor utility, which is capable of extracting path buffers.
 * //
 * This is the BulkExtractor API
 */
public class ImageReader implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ImageReader.class);

    // process management
    private static final int DELAY = 60 * 1000; // 12 is good for testing
    private static final Pattern CONTENT_LENGTH_PATTERN = Pattern.compile("Content-Length: (\\d+)");
    private static final Pattern X_RANGE_AVAILABLE_PATTERN = Pattern.compile("X-Range-Available: bytes 0-(\\d+)");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final Process process;
    private final ExecutorService executor;

    private final HttpClient httpClient;
    private final URI baseUri;
    private final PrintWriter writeToProcess;
    private final InputStream readFromProcess;
    private boolean readerIsValid;
    private long totalSizeAtPath;
    private byte[] bytes = new byte[0];

    /**
     * Open an image reader and attach it to a bulk_extractor process
     * If the reader fails then an error dialog is displayed
     * and an empty reader is returned in its place.
     *
     * @param newFile the file
     */
    public ImageReader(File newFile) {
        this.executor = Executors.newVirtualThreadPerTaskExecutor(); // TODO SRP - HTTP & Process management in one class feels too big
        this.httpClient = HttpClient.newHttpClient();
        this.baseUri = URI.create("http://localhost:" + 80); // TODO or inject whatever config
        // open interactive http channel with bulk_extractor
        // use "bulk_extractor -p -http <image_file>"
        String[] cmd = null;
        cmd = new String[]{"bulk_extractor", "-p", "-http", newFile.getAbsolutePath()};

        logger.info("BulkExtractorFileReader starting bulk_extractor process");
        logger.info("BulkExtractorFileReader cmd: {} {} {} {}", cmd[0], cmd[1], cmd[2], cmd[3]);

        try {
            this.process = new ProcessBuilder(cmd).redirectErrorStream(false) // Keep stderr separate for logging
                    .start();
            writeToProcess = new PrintWriter(process.getOutputStream());
            readFromProcess = process.getInputStream();
            // Handle stderr in a dedicated virtual thread to surface errors in client logs
            executor.submit(() -> {
                try (var errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    errorReader.lines().forEach(line -> logger.error("BulkExtractorFileReader error from bulk_extractor process: {}", line));
                } catch (IOException e) {
                    logger.error("Error reading from stderr: {}", e.getMessage());
                }
            });
        } catch (IOException e) {
            WError.showErrorLater("Unable to start the bulk_extractor reader.", "Error reading Image", e);
            throw new IllegalStateException("Failed to start bulk_extractor process.", e);
        }
    }

    /**
     * indicates whether the reader is valid
     *
     * @return true unless state has become invalid
     */
    public boolean isValid() {
        return process.isAlive();
    }

    public ImageReaderResponse read(String forensicPath, long numBytes) {
        // wrap openAndRead because it throws exceptions
        try {
            return get(forensicPath, numBytes);
        } catch (Exception e) {
            WError.showErrorLater("Unable to read image data.", "Error reading Image", e);
            return new ImageReaderResponse(new byte[0], 0);
        }
    }

    private ImageReaderResponse get(String forensicPath, long numBytes) throws IOException, InterruptedException {
        long rangeStopValue = Math.max(0, numBytes - 1);

        String getString = "GET " + forensicPath + " HTTP/1.1";
        String rangeString = "Range: bytes=0-" + rangeStopValue;
        logger.info("GET [{}] and Range [{}]", getString, rangeString);
        writeToProcess.println(getString);
        writeToProcess.println(rangeString);
        writeToProcess.println();
        writeToProcess.flush();

        // read the response
        String contentLengthLine = readLine();
        logger.info("ImageReader length response 1: '{}'", contentLengthLine);
        String contentRangeLine = readLine();
        logger.info("ImageReader range response 2: '{}'", contentRangeLine);
        String xRangeAvailableLine = readLine();
        logger.info("ImageReader available response 3: '{}'", xRangeAvailableLine);
        String blankLine = readLine();
        logger.info("ImageReader blank line response 4: '{}'", blankLine);

        // Content-Length: provides the number of bytes returned
        int numBytesReturned;
        final String CONTENT_LENGTH = "Content-Length: ";
        logger.info("BulkExtractorFileReader bulk_extractor read response 1: Content-Length: '{}'", contentLengthLine);
        if (!contentLengthLine.startsWith(CONTENT_LENGTH)) {
            logger.info("Invalid content length line: '{}'", contentLengthLine);
            readerIsValid = false;
        }
        String contentLengthString = contentLengthLine.substring(CONTENT_LENGTH.length());
        try {
            // parse out bytes returned from the line read
            numBytesReturned = Math.toIntExact(Long.valueOf(contentLengthString).longValue());
        } catch (NumberFormatException e) {
            logger.info("Invalid numeric format in content length line: '" + contentLengthLine + "'");
            throw e;
        }
        // note if numBytesReturned is not numBytes requested, which is expected near EOF
        if (numBytesReturned != numBytes) {
            logger.info("BulkExtractorFileReader: note: bytes requested: " + numBytes
                    + ", bytes read: " + numBytesReturned);
        }

        // Content-Range: check for presence of the Content-Range header, but ignore range values
        // because numBytesReturned, above, is sufficient
        final String CONTENT_RANGE = "Content-Range: bytes ";
        if (!contentRangeLine.startsWith(CONTENT_RANGE)) {
            logger.info("Invalid content range line: '" + contentRangeLine + "'");
            readerIsValid = false;
        }

        // X-Range-Available: used to derive the total path size for this path
        final String X_RANGE_AVAILABLE = "X-Range-Available: bytes 0-";
        if (!xRangeAvailableLine.startsWith(X_RANGE_AVAILABLE)) {
            logger.info("Invalid X-Range-Available line: '" + xRangeAvailableLine + "'");
            readerIsValid = false;
        }
        // get the range end byte
        String rangeEndString = xRangeAvailableLine.substring(X_RANGE_AVAILABLE.length());
        int pathEndByte = 1844644073;///TODO HACKKKSSS
        try {
           // pathEndByte = Integer.valueOf(rangeEndString).intValue();
            // the total path size is path offset plus the range end value plus one
            totalSizeAtPath = ForensicPath.getOffset(forensicPath) + pathEndByte + 1;
        } catch (NumberFormatException e) {
            totalSizeAtPath = 0;
            logger.info("Invalid numeric format in path range line: '" + contentLengthLine + "'");
            readerIsValid = false;
            throw e;
        }

        // blank line
        if (!blankLine.equals("")) {
            logger.info("Invalid blank line: '" + blankLine + "'");
            readerIsValid = false;
        }

        // read the indicated number of bytes
        bytes = readBytes(numBytesReturned);

        // make sure stdin is now clear
        // this is not a guaranteed indicator of readiness of the bulk_extractor thread
        // because the bulk_extractor thread can issue multiple writes and flushes
        int leftoverBytes = readFromProcess.available();
        if (leftoverBytes != 0) {

            // clean up
            byte[] extraBytes = readBytes(leftoverBytes);

            // fail
           //WLog.logBytes("Unexpected extra bytes in bulk_extractor read response", extraBytes);
           //String extraBytesString = new String(extraBytes);
           //WLog.log("Unexpected bulk_extractor read response: " + leftoverBytes
           //        + " extra bytes: '" + extraBytesString + "'");
            readerIsValid = false;
        }

        // compose the image reader response
        if (readerIsValid) {
            ImageReaderResponse imageReaderResponse = new ImageReaderResponse(
                    bytes, totalSizeAtPath);
            return imageReaderResponse;
        } else {
            return new ImageReaderResponse(new byte[0], 0);
        }
    }

    // read line terminated by \r\n
    private String readLine() throws IOException {
        int bInt;
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        // start watchdog for this read
        //ThreadAborterTimer aborter = new ThreadAborterTimer(process, DELAY);
        // read bytes through required http \r\n line terminator
        while (true) {
            bInt = readFromProcess.read();
            if (bInt == -1) {
                logger.info("bulk_extractor stream terminated in readLine: " + bInt);
                readerIsValid = false;
                break;
            }
            if (bInt != '\r') {
                // add byte to line
                outStream.write(bInt);

            } else {
                // remove the \n following the \r
                bInt = readFromProcess.read();

                // verify \n
                if (bInt != '\n') {
                    logger.info("Invalid line terminator returned from bulk_extractor: " + bInt);
                    readerIsValid = false;
                }
                break;
            }
        }

        // stop watchdog for this read
       // aborter.cancel();

        // convert bytes to String
        String line = outStream.toString();

        return line;
    }

    private byte[] readBytes(int numBytes) throws IOException {
        byte[] bytes = new byte[numBytes];

        // read each byte
        for (int i=0; i<numBytes; i++) {
            int bInt = readFromProcess.read();
            if (bInt > 256) {
                logger.info("Invalid byte returned from bulk_extractor: " + bInt);
                readerIsValid = false;
                break;
            } else if (bInt < 0) {
                // the stream has terminated
                logger.info("bulk_extractor stream terminated by byte: " + bInt);
                readerIsValid = false;
                break;
            } else {
                bytes[i] = (byte)bInt;
            }
        }

        // stop watchdog
      // aborter.cancel();

        return bytes;
    }
    /**
     * Closes the reader, releasing resources.
     */
    @Override
    public void close() {
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

    public record ImageReaderResponse(byte[] bytes, long totalSizeAtPath) {}

    private String formatRequest(HttpRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.method())
                .append(" ")
                .append(request.uri())
                .append(" ")
                .append("HTTP/1.1\n");

        request.headers().map()
                .forEach((k, v) -> sb.append(k).append(": ").append(String.join(",", v)).append("\n"));

        return sb.toString();
    }

    private String formatResponse(HttpResponse<byte[]> response) {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ")
                .append(response.statusCode())
                .append("\n");

        response.headers().map().forEach((k, v) ->
                sb.append(k).append(": ").append(String.join(",", v)).append("\n"));

        if (response.body() instanceof byte[] bodyBytes) {
            sb.append("\n[Body: ").append(bodyBytes.length).append(" bytes]");
        } else {
            sb.append("\n[Body type: ").append(response.body().getClass().getSimpleName()).append("]");
        }

        return sb.toString();
    }
}

