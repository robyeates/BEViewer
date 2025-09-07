package co.rob.io;

import co.rob.state.ReportsModel;
import co.rob.util.ReportFileReadException;
import co.rob.util.file.FileTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.prefs.Preferences;

@Singleton
public class ReportFileReader {

    private static final Logger logger = LoggerFactory.getLogger(ReportFileReader.class);
    private ReportsModel reportsModel;

    @Inject
    public ReportFileReader(ReportsModel reportsModelDAG) {
       reportsModel = reportsModelDAG;
    }

    public File validateReportFile(String reportFileTextField, String reportFileName) throws ReportFileReadException {
        File reportFile = new File(reportFileTextField);
        if (!reportFile.getName().equals(reportFileName)) {
            // not "report.xml"
            throw new ReportFileReadException("Report file " + reportFile.getPath() + "\ndoes not end in " + reportFileName + ".");
        } else if (reportFile.isDirectory()) {
            // directory
            throw new ReportFileReadException("Directory " + reportFile.getPath() + "\nis not a valid Report file.");
        } else if (!reportFile.canRead() && !reportFile.isAbsolute()) {
            // can't read and it is at a relative path
            throw new ReportFileReadException("Report file " + reportFile.getPath() + "\ncannot be read.  Please select an absolute path.");
        } else if (!reportFile.canRead()) {
            // can't read
            throw new ReportFileReadException("Report file " + reportFile.getPath() + "\ncannot be read.  Please verify that the Report file is valid.");
        } else {
            // good, the file is valid, normalize it
            try {
                return reportFile.getCanonicalFile();
            } catch (IOException ioe) {
                throw new ReportFileReadException("Report file " + reportFile.getPath() + "\ncannot be read: " + ioe);
            }
        }
    }

    // load saved reports
    public void loadSavedReports(Preferences savedReports) {

        int i = 0;
        while (true) {
            // generate the preferences variable names
            String reportIndex = Integer.toString(i);
            String featuresDirectoryString = savedReports.get("feature_directory_" + reportIndex, null);
            File featuresDirectory = (featuresDirectoryString == null) ? null : new File(featuresDirectoryString);
            String reportImageFileString = savedReports.get("image_file_" + reportIndex, null);
            File reportImageFile = (reportImageFileString == null) ? null : new File(reportImageFileString);

            // stop loading reports when there are no more saved values
            if (featuresDirectory == null) {
                break;
            }
            if (reportImageFile == null) {
                logger.info("BEPreferences: loadSavedReports: unexpected null reportImageFile");
                break;
            }

            // load the saved report into the model
            reportsModel.addReport(featuresDirectory, reportImageFile);

            // move to the next index
            i++;
        }
    }

    // save reports
    public void saveReports(Preferences savedReports) throws Exception {
        // clear and recreate reports node
        savedReports.clear();

        // get reports to save
        Enumeration<ReportsModel.ReportTreeNode> e = reportsModel.elements();
        int i = 0;
        while (e.hasMoreElements()) {
            ReportsModel.ReportTreeNode reportTreeNode = e.nextElement();

            // get the File strings
            String featuresDirectoryString = FileTools.getAbsolutePath(reportTreeNode.featuresDirectory);
            String reportImageFileString = FileTools.getAbsolutePath(reportTreeNode.reportImageFile);

            // save the indexed feature directory and image file preferences
            String reportIndex = Integer.toString(i);
            savedReports.put("feature_directory_" + reportIndex, featuresDirectoryString);
            savedReports.put("image_file_" + reportIndex, reportImageFileString);

            i++;
        }
    }
}
