package co.rob.util;


import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import org.apache.commons.text.StringEscapeUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * The <code>EscapedStrings</code> class provides conversions between escaped and unescaped
 * String formats and between byte and String formats of various UTF encodings.
 * See UTF_* constants for available UTF encodings supported.
 */
public class UTF8Tools {
    public static final Charset UTF_8 = StandardCharsets.UTF_8;

    /**
     * Remove any escape codes from the byte array.
     */
    public static byte[] unescapeBytes(byte[] bytes) {
        String escapedString = new String(bytes, StandardCharsets.UTF_8);
        String unescapedString = StringEscapeUtils.unescapeJava(escapedString);
        return unescapedString.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Put the escape character back but leave all other escape characters.
     * Specifically, put back \x5C and \134 to \.
     */
    public static byte[] unescapeEscape(byte[] bytes) {
        String input = new String(bytes, StandardCharsets.UTF_8);
        String output = input.replaceAll("\\\\134|\\\\x5[Cc]", "\\\\");
        return output.getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] utf8To16Correct(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8)
                .getBytes(StandardCharsets.UTF_16);
    }

    public static byte[] utf16To8Correct(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_16)
                .getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Convert upper case bytes to lower case
     * // TODO Should this be only Ascii?
     */
    public static byte[] asciiToLower(byte[] bytes) {
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        for (byte b : bytes) {
            s.write(asciiToLower(b));
        }
        return s.toByteArray();
    }

    public static byte asciiToLower(byte b) {
        if (b >= 'A' && b <= 'Z') {
            return (byte) (b + ('a' - 'A'));
        }
        return b;
    }

    public static char asciiToLower(char b) {
        if (b >= 'A' && b <= 'Z') {
            return (char) (b + ('a' - 'A'));
        }
        return b;
    }

    /**
     * Check equality between two byte arrays
     */
    public static boolean bytesMatch(byte[] bytes1, byte[] bytes2) {
        return Arrays.equals(bytes1, bytes2);
    }

    /**
     * Determine if the bytes look like UTF16
     */
    public static boolean escapedLooksLikeUTF16(byte[] bytes) {
        byte[] unescapedBytes = UTF8Tools.unescapeBytes(bytes);
        CharsetDetector detector = new CharsetDetector();
        detector.setText(unescapedBytes);
        CharsetMatch match = detector.detect();
        return match != null && (match.getName().equalsIgnoreCase("UTF-16LE")
                || match.getName().equalsIgnoreCase("UTF-16BE")
                || match.getName().toUpperCase(Locale.ROOT).startsWith("UTF-16"));
    }

    /*
     * Remove nulls, only use this if escapedLooksLikeUTF16 is true.
     */
    public static byte[] stripNulls(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new byte[0];
        }
        // Convert to String, replace escaped nulls, then convert back to bytes.
        String temp = new String(bytes, StandardCharsets.UTF_8);
        // Regex matches \000 or \x00
        temp = temp.replaceAll("\\\\000|\\\\x00", "");
        return temp.getBytes(StandardCharsets.UTF_8);
    }
}

