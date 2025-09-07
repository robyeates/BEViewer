package co.rob.util.file;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * Finds the report.xml report file type.
 */
public class ReportFileFilter extends FileFilter {
    // accept filenames for valid image files

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

        // valid report file
        return file.getName().equals("report.xml");
    }

    /**
     * The description of this filter.
     *
     * @return the description of this filter
     */
    public String getDescription() {
        return "Report Files (report.xml)";
    }
}

