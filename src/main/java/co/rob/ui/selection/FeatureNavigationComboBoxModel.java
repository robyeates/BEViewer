package co.rob.ui.selection;

import co.rob.pojo.FeatureLine;
import co.rob.state.ReportsModel;
import co.rob.util.ForensicPath;

import javax.inject.Inject;
import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

import static co.rob.io.features.FeatureLineUtils.isFromReport;

/**
 * The <code>FeatureNavigationComboBoxModel</code> class extends <code>DefaultComboBoxModel</code>
 * to provide a list of selectable features for a ComboBox.
 * It is supplied with features as feature selections are made.
 * Seems to have disappeared in 1.6? let's see if it can be saved
 */
public class FeatureNavigationComboBoxModel extends DefaultComboBoxModel<FeatureLine> {

    //TODO shove this in the image pane

    /**
     * Constructs a feature ComboBox model that adds and selects Feature Lines
     * from the image model as selections are made.
     * The feature list may be removed via <code>super.removeAllElements()</code>.
     */
    @Inject
    public FeatureNavigationComboBoxModel(FeatureLineSelectionManager featureLineSelectionManager) {
        featureLineSelectionManager.addFeatureLineSelectionManagerChangedListener((_, _, newValue) -> {
            // feature lines are navigable if they have a valid path
            // but not if they have a histogram field
            if (newValue != null && !ForensicPath.isHistogram(newValue.forensicPath().getBytes(StandardCharsets.UTF_8))) {
                selectFeature(newValue);
            }
        });
    }

    /**
     * Selects the feature, creating it if it is not already there.
     */
    public void selectFeature(FeatureLine featureLine) {
        // if null, select null
        if (featureLine == null) {
            doSelect(null);
            return;
        }

        // if available, select an equivalent feature line
        FeatureLine residentFeatureLine = IntStream.range(0, getSize())
                .mapToObj(this::getElementAt)
                .filter(featureLine::equals)
                .findFirst()
                .orElse(null);

        if (residentFeatureLine != null) {
            doSelect(residentFeatureLine);
        } else {
            addElement(featureLine);
            doSelect(featureLine);
        }
    }

    private void doSelect(FeatureLine featureLine) {
        if (featureLine != getSelectedItem()) {
            setSelectedItem(featureLine); // in DefaultComboBoxModel
        }
    }

    /**
     * Clear the navigation history.
     */
    public void removeAllFeatures() {
        removeAllElements();
    }

    /**
     * Clear the navigation history of features associated with the given Report.
     */
    public void removeAssociatedFeatures(ReportsModel.ReportTreeNode reportTreeNode) {

        // if the selected item is going to be removed, clear it or else the
        // model will assign another one
        FeatureLine selectedFeatureLine = (FeatureLine) getSelectedItem();
        if (selectedFeatureLine != null && isFromReport(selectedFeatureLine, reportTreeNode)) {
            setSelectedItem(null);
        }

        // DefaultComboBoxModel doesn't provide an iterator so copy to an array for use below
        FeatureLine[] featureLines = new FeatureLine[getSize()];
        for (int i = 0; i < featureLines.length; i++) {
            featureLines[i] = getElementAt(i);
        }

        // now remove the associated features from DefaultComboBoxModel
        for (FeatureLine featureLine : featureLines) {
            if (isFromReport(featureLine, reportTreeNode)) {
                removeElement(featureLine);
            }
        }
    }
}

