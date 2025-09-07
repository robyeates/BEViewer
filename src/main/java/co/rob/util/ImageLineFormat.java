package co.rob.util;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The <code>LineFormat</code> class identifies view format styles.
 */

public enum ImageLineFormat {

    HEX_FORMAT("Hex"),
    TEXT_FORMAT("Text");

    private static final Map<String, ImageLineFormat> nameToFormat = new HashMap<>();

    static {
        // Build a lookup map on startup
        Stream.of(ImageLineFormat.values()).forEach(format -> nameToFormat.put(format.name, format));
    }

    private final String name;

    ImageLineFormat(String name) {
        this.name = name;
    }

    /**
     * Returns the {@code LineFormat} object associated with the given name.
     *
     * @param name The name of the view format.
     * @return The corresponding {@code LineFormat} or {@code TEXT_FORMAT} if not recognized.
     */
    public static ImageLineFormat lookup(String name) {
        return nameToFormat.getOrDefault(name, TEXT_FORMAT);
    }

    @Override
    public String toString() {
        return name;
    }
}