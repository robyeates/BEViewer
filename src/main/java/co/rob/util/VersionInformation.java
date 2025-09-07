package co.rob.util;

import java.io.IOException;
import java.util.Properties;

public class VersionInformation {

    private static final Properties props = new Properties();

    static {
        try (var in = VersionInformation.class.getResourceAsStream("/version.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load version info", e);
        }
    }

    public static String getVersion() {
        return props.getProperty("beviewer.version", "unknown");
    }
}
