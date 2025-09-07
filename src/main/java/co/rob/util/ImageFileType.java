package co.rob.util;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides image file type constants and code for determining image file types.
 */

public record ImageFileType(String name, String[] rawSuffixArray) {
    // the suffix arrays
    public static final String[] RAW_SUFFIX_ARRAY = {".raw", ".img"};
    public static final String[] MULTIPART_SUFFIX_ARRAY = {".000", ".001", "001.vmdk"};
    public static final String[] E01_SUFFIX_ARRAY = {".E01"};
    public static final String[] AFF_SUFFIX_ARRAY = {".aff"};

    // the image file types
    public static final ImageFileType RAW = new ImageFileType("Raw image", RAW_SUFFIX_ARRAY);
    public static final ImageFileType MULTIPART = new ImageFileType("Multipart image", MULTIPART_SUFFIX_ARRAY);
    public static final ImageFileType E01 = new ImageFileType("E01 image", E01_SUFFIX_ARRAY);
    public static final ImageFileType AFF = new ImageFileType("AFF image", AFF_SUFFIX_ARRAY);


    /**
     * Returns all supported file types, separated by a space
     */
    public static String getSupportedImageFileTypes() {
        // Combine all suffix arrays into a single stream of strings
        String result = Stream.of(RAW_SUFFIX_ARRAY, MULTIPART_SUFFIX_ARRAY, E01_SUFFIX_ARRAY, AFF_SUFFIX_ARRAY)
                .flatMap(Stream::of)
                .collect(Collectors.joining(" "));

        // Append "/dev" if not on Windows
        if (!System.getProperty("os.name").startsWith("Windows")) {
            result += " /dev";
        }

        return result.trim();
    }

    /**
     * Returns the string name of the image file type
     */
    @NotNull
    public String toString() {
        return name;
    }

    /**
     * see if file suffix is in the given suffix array
     */
    public static boolean hasValidSuffix(File file) {
        return Stream.of(RAW_SUFFIX_ARRAY, E01_SUFFIX_ARRAY, AFF_SUFFIX_ARRAY)
                .flatMap(Stream::of)
                .anyMatch(suffix -> file.getName().toLowerCase().endsWith("." + suffix));
    }

    /**
     * Returns the image file type
     */
    public static ImageFileType getImageFileType(File file) {
        return Stream.of(
                        // Use a map to associate suffixes with their corresponding types
                        Map.entry(MULTIPART_SUFFIX_ARRAY, MULTIPART),
                        Map.entry(E01_SUFFIX_ARRAY, E01),
                        Map.entry(AFF_SUFFIX_ARRAY, AFF)
                )
                .filter(e -> hasValidSuffixType(e.getKey(), file))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(RAW);
    }

    private static boolean hasValidSuffixType(String[] suffixes, File file) {
        if (file == null || suffixes == null) {
            return false;
        }

        String fileExtension = FilenameUtils.getExtension(file.getName());

        return Arrays.stream(suffixes)
                .anyMatch(suffix -> suffix.equalsIgnoreCase(fileExtension));
    }
}

