package co.rob.util.file;

import java.io.File;
import java.util.Objects;

/**
 * The <code>FileTools</code> class provides static null-safe file operators.
 */
public final class FileTools {

    /**
     * Returns whether files are equivalent, accounting for null.
     */
    public static boolean filesAreEqual(File file1, File file2) {
        return Objects.equals(file1, file2);
    }

    /**
     * Returns value, accounting for null.
     */
    public static String getAbsolutePath(File file) {
        return file == null ? "" : file.getAbsolutePath();
    }
}

