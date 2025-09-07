package co.rob.io.features;

import co.rob.pojo.FeatureLine;
import co.rob.state.ReportsModel;
import co.rob.util.ForensicPath;
import co.rob.util.file.FileTools;

import java.io.File;
import java.util.Arrays;

public class FeatureLineUtils {

    private FeatureLineUtils() {}

    /**
     * Returns a printable summary string of this feature line.
     * //, imageView.getUseHexPath()
     */
    public static String getSummaryString(FeatureLine featureLine, boolean isUseHexPath) {
        // compose a printable summary that fits on one line
        String summary;
        if (featureLine.actualImageFile() == null) {
            if (featureLine.featuresFile() == null) {
                // this string is returned by the FeatureListCellRenderer as the prototype display value
                return "no image file and no features file";
            } else {
                // indicate what is available
                summary = "No image file selected, " + Arrays.toString(featureLine.firstField()) + ", " + featureLine.formattedFeature();
            }
        } else {
            summary = ForensicPath.getPrintablePath(featureLine.forensicPath(), isUseHexPath)
                    + ", " + featureLine.featuresFile().getName()
                    + ", " + featureLine.actualImageFile().getName()
                    + ", " + featureLine.formattedFeature();
        }

        // truncate the sumamry
        final int MAX_LENGTH = 200;
        if (summary.length() > MAX_LENGTH) {
            summary = summary.substring(0, MAX_LENGTH) + "\u2026";
        }

        return summary;
    }

    /**
     * Identifies whether this is really a blank feature line.
     */
    public static boolean isBlank(FeatureLine featureLine) {
        if (featureLine == null) {
            return true;
        }
        return (featureLine.reportImageFile() == null && featureLine.featuresFile() == null);
    }

    /**
     * Identifies if this FeatureLine was unable to initialize.
     */
    public static boolean isBad(FeatureLine featureLine) {
        return featureLine.isFailedInitialization();
    }

    /**
     * Identifies whether the feature line is from the given report
     */
    public static boolean isFromReport(FeatureLine featureLine, ReportsModel.ReportTreeNode reportTreeNode) {
        // image file and features directory must be equivalent
        File reportImageFile = reportTreeNode.reportImageFile;
        File reportFeaturesDirectory = reportTreeNode.featuresDirectory;
        File featuresDirectory = (featureLine.featuresFile() == null) ? null : featureLine.featuresFile().getParentFile();
        return (FileTools.filesAreEqual(reportImageFile, reportImageFile)
                && FileTools.filesAreEqual(reportFeaturesDirectory, featuresDirectory));
    }
}
