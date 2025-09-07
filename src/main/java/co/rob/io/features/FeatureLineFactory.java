package co.rob.io.features;

import co.rob.ui.renderer.FeatureFieldFormatter;
import co.rob.pojo.FeatureLine;
import co.rob.ui.dialog.WError;
import co.rob.util.ForensicPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FeatureLineFactory {

    private static final Logger logger = LoggerFactory.getLogger(FeatureLineFactory.class);

    private FeatureLineFactory() {}

    public static FeatureLine create(File reportImageFile, File featuresFile, long startByte, int numBytes) {
        byte[] lineBytes;
        try {
            lineBytes = readBytesFromFile(featuresFile, startByte, numBytes);
        }catch (Exception e) {
            handleError(e, featuresFile);
            return new FeatureLine(reportImageFile, featuresFile, startByte, numBytes, new byte[0], new byte[0], new byte[0], null, "", "", true);
        }
        // The logic from the original constructor is broken out into helpers
        int correctedNumBytes = stripCarriageReturn(lineBytes, numBytes);
        FeatureLineData data = parseLine(lineBytes, correctedNumBytes);

        File actualImageFile = determineActualImageFile(reportImageFile, data.firstField());
        String formattedFeature = FeatureFieldFormatter.getFormattedFeatureText(featuresFile, data.featureField(), data.contextField());
        String forensicPath = ForensicPath.getPathWithoutFilename(data.firstField());

        return new FeatureLine(
                reportImageFile,
                featuresFile,
                startByte,
                numBytes,
                data.firstField(),
                data.featureField(),
                data.contextField(),
                actualImageFile,
                forensicPath,
                formattedFeature,
                false
        );


    }

    private static byte[] readBytesFromFile(File featuresFile, long startByte, int numBytes) throws IOException {
        try (FileInputStream fis = new FileInputStream(featuresFile);
             FileChannel channel = fis.getChannel()) {

            long fileSize = channel.size();
            if (startByte + numBytes > fileSize) {
                throw new IOException("Invalid line request: outside of file bounds.");
            }

            MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, startByte, numBytes);
            mappedByteBuffer.load();
            byte[] bytes = new byte[numBytes];
            mappedByteBuffer.get(bytes, 0, numBytes);
            return bytes;
        }
    }

    /**
     * the line should end in \n and never in \r\n, but check for and strip out \r anyway.
     * It is possible for transport tools such as zip to turn \n into \r\n.
     * If deemed unnecessary, this check may be removed.
     * The \n is stripped out already.  Strip out the \r now if it is there.
     */
    private static int stripCarriageReturn(byte[] lineBytes, int numBytes) {
        if (numBytes > 0 && lineBytes[numBytes - 1] == '\r') {
            return numBytes - 1;
        }
        return numBytes;
    }

    private static FeatureLineData parseLine(byte[] lineBytes, int numBytes) {
        int firstTabIndex = findTabIndex(lineBytes, 0, numBytes);
        int secondTabIndex = (firstTabIndex != -1 && firstTabIndex + 1 < numBytes) ? findTabIndex(lineBytes, firstTabIndex + 1, numBytes) : -1;

        byte[] firstField = extractField(lineBytes, 0, firstTabIndex);
        byte[] featureField = extractField(lineBytes, firstTabIndex + 1, secondTabIndex);
        byte[] contextField = (secondTabIndex != -1) ? extractField(lineBytes, secondTabIndex + 1, numBytes) : new byte[0];

        if (firstField.length == 0 && featureField.length == 0 && contextField.length == 0) {
            // there was no first tab, so set feature field as whole line
            featureField = lineBytes;
        }

        return new FeatureLineData(firstField, featureField, contextField);
    }

    // define firstField as text before first tab else all text
    private static int findTabIndex(byte[] bytes, int start, int end) {
        for (int i = start; i < end; i++) {
            if (bytes[i] == '\t') {
                return i;
            }
        }
        return -1;
    }

    private static byte[] extractField(byte[] source, int start, int end) {
        // Handle invalid input ranges.
        if (start < 0 || end < start) {
            return new byte[0];
        }

        // Write the bytes to a stream and return the array.
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(source, start, end - start);
        return stream.toByteArray();
    }

    private static File determineActualImageFile(File reportImageFile, byte[] firstField) {
        if (ForensicPath.hasFilename(firstField)) {
            return new File(ForensicPath.getFilename(firstField));
        } else {
            return reportImageFile;
        }
    }

    private static void handleError(Exception e, File featuresFile) {
        if (featuresFile != null && !featuresFile.exists()) {
            WError.showErrorLater("Features file " + featuresFile + " does not exist.", "BEViewer file error", null);
        } else {
            logger.error("Unable to open features file {}.", featuresFile, e);
            WError.showErrorLater("Unable to open features file " + featuresFile + ".", "BEViewer file error", e);
        }
    }

    /** A record to hold the parsed data fields for clean return from a method. */
    private record FeatureLineData(byte[] firstField, byte[] featureField, byte[] contextField) {}
}
