package co.rob.pojo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WorkSettings {

    private final List<ReportSettings> reportSettings = new ArrayList<>();
    private final List<FeatureLine> featureLines = new ArrayList<>();

    public void captureReportSettings(File reportFeaturesDirectory, File reportImageFile) {
        reportSettings.add(new ReportSettings(reportFeaturesDirectory, reportImageFile));
    }

    public List<ReportSettings> getReportSettings() {
        return reportSettings;
    }

    public List<FeatureLine> getFeatureLines() {
        return featureLines;
    }

    public void captureFeatureLines(File reportImageFile, File featuresFile, long startByte, int numBytes, byte[] firstField, byte[] featureField, byte[] contextField,
                                    File actualImageFile, String forensicPath, String formattedFeature, boolean isFailedInitialization) {
        featureLines.add(new FeatureLine(
                reportImageFile,
                featuresFile,
                startByte,
                numBytes,
                firstField,
                featureField,
                contextField,
                actualImageFile,
                forensicPath,
                formattedFeature,
                isFailedInitialization
        ));
    }

    public record ReportSettings(File reportFeaturesDirectory, File reportImageFile ) {
    }

}

