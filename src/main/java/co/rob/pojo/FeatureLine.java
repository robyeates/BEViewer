package co.rob.pojo;

import co.rob.io.features.FeatureLineFactory;
import co.rob.util.file.FileTools;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;

/**
 * An immutable data carrier for a feature line.
 */
public record FeatureLine(
        File reportImageFile,
        File featuresFile,
        long startByte,
        int numBytes,
        byte[] firstField, // firstField is text before first tab
        byte[] featureField, // featureField is text after first tab and before second tab
        byte[] contextField,
        File actualImageFile,
        String forensicPath,
        String formattedFeature,
        boolean isFailedInitialization
) {

    // The canonical constructor is public by default, but we'll use a private static factory.
    // This record only serves as a data holder.

    public static FeatureLine create(File reportImageFile, File featuresFile, long startByte, int numBytes) {
        return FeatureLineFactory.create(reportImageFile, featuresFile, startByte, numBytes);
    }

    /**
     * Returns a string detailing the state of the feature line.
     */
    @NotNull
    public String toString() {
        return "reportImageFile: " + reportImageFile +
                ", actualImageFile: " + actualImageFile +
                ", featuresFile: " + featuresFile +
                ", startByte: " + startByte +
                ", numBytes: " + numBytes +

                // derived fields
                ", firstField: " + Arrays.toString(firstField) +
                ", forensicPath: " + forensicPath +
                ", featureField: " + Arrays.toString(featureField) +
                ", contextField: " + Arrays.toString(contextField);
    }

    /**
     * Identifies when two FeatureLine objects are equivalent
     *
     * @return true when equivalent
     */
    public boolean equals(FeatureLine featureLine) {
        return (FileTools.filesAreEqual(reportImageFile, featureLine.reportImageFile)
                && FileTools.filesAreEqual(featuresFile, featureLine.featuresFile)
                && startByte == featureLine.startByte
                && numBytes == featureLine.numBytes);
    }

    public int hashCode() {
        // sufficient effort to avoid collision
        return (int) startByte;
    }
}

