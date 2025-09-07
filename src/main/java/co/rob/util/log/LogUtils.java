package co.rob.util.log;

public class LogUtils {

    /**
     * Preserved from the outgoing WLog
     */
    public static String formatLogBytes(String text, byte[] bytes) {
        StringBuilder builder = new StringBuilder();

        // generate text
        builder.append("\n");
        builder.append(text);
        builder.append(", size: ").append(bytes.length);
        builder.append("\n");

        int i;

        // add bytes as text
        for (i=0; i<bytes.length; i++) {
            byte b = bytes[i];
            if (b > 31 && b < 127) {
                builder.append((char)b);	// printable
            } else {
                builder.append(".");	// not printable
            }
        }
        builder.append("\n");

        // add bytes as hex
        for (i=0; i<bytes.length; i++) {
            builder.append(String.format("%1$02x", bytes[i]));
            builder.append(" ");
        }
        builder.append("\n");

        // generate log string

        return builder.toString();
    }
}
