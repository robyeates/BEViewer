package co.rob.state;

import co.rob.io.features.FeatureLineFactory;
import co.rob.pojo.FeatureLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FastFeatureLineTable {

    private final List<Long> lineBeginIndices = new ArrayList<>();
    private final List<Integer> lineLengths = new ArrayList<>();
    private int widestLineLength = 0;

    private final File imageFile;
    private final File featureFile;

    public FastFeatureLineTable(File imageFile, File featureFile) {
        this.imageFile = imageFile;
        this.featureFile = featureFile;
    }

    /**
     * Adds a feature line index.
     */
    public void put(long startByte, int lineLength) {
        lineBeginIndices.add(startByte);
        lineLengths.add(lineLength);
        widestLineLength = Math.max(widestLineLength, lineLength);
    }

    /**
     * Returns the feature line indexed by the given line number.
     */
    public FeatureLine get(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= size()) {
            throw new IndexOutOfBoundsException("Index " + lineIndex + " out of bounds.");
        }
        return FeatureLineFactory.create(imageFile, featureFile, lineBeginIndices.get(lineIndex), lineLengths.get(lineIndex));
    }

    public int getWidestLineLength() {
        return widestLineLength;
    }

    public int size() {
        return lineBeginIndices.size();
    }
}
