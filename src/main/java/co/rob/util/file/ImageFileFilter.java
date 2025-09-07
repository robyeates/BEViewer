package co.rob.util.file;

import org.apache.commons.io.FilenameUtils;

import javax.swing.filechooser.FileFilter;
import java.io.File;

import static co.rob.util.ImageFileType.*;

public class ImageFileFilter extends FileFilter {

    /**
     * Whether the given file is accepted by this filter.
     *
     * @param file The file to check
     * @return true if accepted, false if not
     */
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        }

        boolean isLinuxDevice = file.getAbsolutePath().startsWith("/dev/");

        return hasValidSuffix(file) || isLinuxDevice || isValidFirstMultipartFile(file);
    }

    /**
     * Indicates whether the specified file is a valid first multipart file.
     * Note that .001 is not valid if .000 exists in the same directory.
     *
     * @param firstFile the first file in the multipart file sequence
     */
    private static boolean isValidFirstMultipartFile(File firstFile) {
        String extension = FilenameUtils.getExtension(firstFile.getName());

        return switch (extension.toLowerCase()) {
            case "000" -> true;
            case "001" -> {
                // Check if a .000 file exists to determine if this is the first file
                String baseName = FilenameUtils.getBaseName(firstFile.getName());
                String possible000FileName = baseName.substring(0, baseName.length() - 1) + "0";
                File possible000File = new File(firstFile.getParent(), possible000FileName + "." + "000");

                yield !possible000File.isFile();
            }
            case "vmdk" -> firstFile.getName().toLowerCase().endsWith("001.vmdk");
            default -> false;
        };
    }

    /**
     * The description of this filter.
     *
     * @return the description of this filter
     */
    public String getDescription() {
        return "Image Files (" + getSupportedImageFileTypes() + ")";
    }

}