package co.rob.pojo;

import org.jetbrains.annotations.NotNull;

/**
 * The <code>ImageSourceType</code> class identifies where images are expected to come from.
 */
public record ImageSourceType(String name) {
    public static final ImageSourceType IMAGE_FILE = new ImageSourceType("Image file");
    public static final ImageSourceType RAW_DEVICE = new ImageSourceType("Raw device");
    public static final ImageSourceType DIRECTORY_OF_FILES = new ImageSourceType("Directory of files");

    @NotNull
    public String toString() {
        return name;
    }
}

