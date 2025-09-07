package co.rob.util;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Decimal/Text or Hex
 */
public enum PathFormat {

    HEX_FORMAT("Hex"),
    TEXT_FORMAT("Text");

    private static final Map<String, PathFormat> nameToFormat = new HashMap<>();

    static {
        // Build a lookup map on startup
        Stream.of(PathFormat.values()).forEach(format -> nameToFormat.put(format.name, format));
    }

    private final String name;

    PathFormat(String name) {
        this.name = name;
    }

    /**
     * Returns the {@code LineFormat} object associated with the given name.
     *
     * @param name The name of the view format.
     * @return The corresponding {@code LineFormat} or {@code TEXT_FORMAT} if not recognized.
     */
    public static PathFormat lookup(String name) {
        return nameToFormat.getOrDefault(name, TEXT_FORMAT);
    }

    @Override
    public String toString() {
        return name;
    }
}