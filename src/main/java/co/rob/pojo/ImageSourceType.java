package co.rob.pojo;

import org.jetbrains.annotations.NotNull;

/**
 * The <code>ImageSourceType</code> class identifies where images are expected to come from.
 */
public record ImageSourceType(String name) {
    public static final ImageSourceType IMAGE_FILE = new ImageSourceType("Image file");
    public static final ImageSourceType RAW_DEVICE = new ImageSourceType("Raw device");
    public static final ImageSourceType DIRECTORY_OF_FILES = new ImageSourceType("Directory of files");

    private static final java.util.Map<String, ImageSourceType> VALUES =
            java.util.Map.of(
                    IMAGE_FILE.name, IMAGE_FILE,
                    RAW_DEVICE.name, RAW_DEVICE,
                    DIRECTORY_OF_FILES.name, DIRECTORY_OF_FILES
            );

    public static ImageSourceType valueOf(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        ImageSourceType type = VALUES.get(name);
        return type != null ? type : new ImageSourceType(name);
    }

    @NotNull
    public String toString() {
        return name;
    }
}

