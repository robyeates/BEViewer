package co.rob.util;

import org.jetbrains.annotations.NotNull;

public record PropertyChangeConstants(String propertyName) {

    public static final PropertyChangeConstants FEATURE_LINE = new PropertyChangeConstants("FeatureLineSelectionManager.FeatureLine");

    @NotNull
    public String toString() {
        return propertyName;
    }
}
