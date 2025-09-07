package co.rob.util;

import java.nio.charset.StandardCharsets;

public class UTF16Tools {
    public static byte[] getUTF16Bytes(byte[] utf8Bytes) {
        // Handle null or empty input gracefully
        if (utf8Bytes == null || utf8Bytes.length == 0) {
            return new byte[0];
        }

        return new String(utf8Bytes, StandardCharsets.UTF_8)
                .getBytes(StandardCharsets.UTF_16);
    }
}
