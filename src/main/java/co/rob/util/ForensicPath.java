package co.rob.util;

import co.rob.state.ImageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The <code>ForensicPath</code> class provides accessors to forensic path
 * strings.
 */
public final class ForensicPath {

    private static final Logger logger = LoggerFactory.getLogger(ForensicPath.class);

    /**
     * The UTF-8 byte sequence for the forensic filename marker (U+10001C)
     * followed by a hyphen delimiter.
     */
    private static final byte[] FILENAME_MARKER = {(byte) 0xf4, (byte) 0x80, (byte) 0x80, (byte) 0x9c};

    /**
     * Obtain adjusted path from existing path and specified offset.
     */
    public static String getAdjustedPath(String forensicPath, long offset) {
        if (forensicPath == null || forensicPath.isBlank()) {
            return String.valueOf(offset);
        }
        int offsetIndex = getOffsetIndex(forensicPath);
        if (offsetIndex == 0) { // no hyphen
            return String.valueOf(offset);
        } else {
            // the forensic path has embedded offsets
            String frontString = forensicPath.substring(0, offsetIndex);
            return frontString + offset;
        }
    }

    /**
     * Obtains a path aligned on a page boundary.
     *
     * @param forensicPath The original forensic path string.
     * @return The aligned forensic path.
     */
    public static String getAlignedPath(String forensicPath) {
        long offset = getOffset(forensicPath);
        long alignedOffset = offset - (offset % ImageModel.PAGE_SIZE);
        return getAdjustedPath(forensicPath, alignedOffset);
    }

    /**
     * Obtain the offset from the last part of the forensic path.
     */
    public static long getOffset(String forensicPath) {
        // allow ""
        if (forensicPath == null || forensicPath.isBlank()) {
            return 0;
        }

        int offsetIndex = getOffsetIndex(forensicPath);
        String offsetString = forensicPath.substring(offsetIndex);
        try {
            return Long.parseLong(offsetString);
        } catch (Exception e) {
            logger.error("malformed forensic path: '{}'", forensicPath);
            return 0;
        }
    }

    /**
     * See if firstField includes filename.
     */
    public static boolean hasFilename(byte[] firstField) {
        return (getForensicPathIndex(firstField) > 0);
    }

    /**
     * Return filename from firstField if available else "".
     */
    public static String getFilename(byte[] firstField) {
        int index = getForensicPathIndex(firstField);
        if (index == 0) {
            return "";
        } else {
            return new String(firstField, 0, index - FILENAME_MARKER.length - 1, StandardCharsets.UTF_8);
        }
    }


    /**
     * Return path from firstField without filename component,
     * returns same if there is no filename component
     */
    public static String getPathWithoutFilename(byte[] firstField) {
        int index = getForensicPathIndex(firstField); // either 0 or >=5
        return new String(firstField, index, firstField.length - index, StandardCharsets.UTF_8);
    }

    /**
     * Determines if the first field is a histogram entry.
     * A histogram entry is of the form "n=<number>\t".
     *
     * @param firstField The byte array to check.
     * @return {@code true} if it matches the histogram format, {@code false} otherwise.
     */
    public static boolean isHistogram(byte[] firstField) {
        if (firstField == null || firstField.length < 4) {
            return false;
        }

        // Use a more robust check with regular expressions
        String content = new String(firstField, StandardCharsets.UTF_8);
        return Pattern.matches("n=\\d+\t.*", content);
    }

    /**
     * Return printable path without any filename, in hex or decimal.
     */
    public static String getPrintablePath(String forensicPath, boolean useHex) {
        // derive the printable path
        if (!useHex) {
            return forensicPath;
        }
        return Arrays.stream(forensicPath.split("-")).map(ForensicPath::convertToHexIfNumeric).collect(Collectors.joining("-"));
    }

    private static String convertToHexIfNumeric(String part) {
        try {
            long offset = Long.parseLong(part);
            return Long.toHexString(offset);
        } catch (NumberFormatException e) {
            // Not a number, so return as is
            return part;
        }
    }

    /**
     * Finds the index of the first occurrence of the filename marker followed by a hyphen.
     * TODO UNIT TESTTT
     * @param firstField The byte array to search.
     * @return The start index of the path, or -1 if not found.
     */
    private static int getForensicPathIndex(byte[] firstField) {
        ByteBuffer buffer = ByteBuffer.wrap(firstField);
        while (buffer.hasRemaining()) {
            if (buffer.remaining() >= FILENAME_MARKER.length + 1) {
                byte[] temp = new byte[FILENAME_MARKER.length];
                buffer.mark(); // Mark the current position
                buffer.get(temp);
                if (Arrays.equals(temp, FILENAME_MARKER) && buffer.get() == '-') {
                    return buffer.position();
                }
                buffer.reset(); // Reset to the marked position
            }
            buffer.get(); // Move to the next byte
        }
        // not found
        return 0;
    }

    // the offset is after the last '-', if any, in the forensic path
    private static int getOffsetIndex(String forensicPath) {
        int delimiterPosition = forensicPath.lastIndexOf('-');
        if (delimiterPosition == -1) {
            // the forensic path is a simple offset
            return 0;
        } else {
            return delimiterPosition + 1;
        }
    }
}

