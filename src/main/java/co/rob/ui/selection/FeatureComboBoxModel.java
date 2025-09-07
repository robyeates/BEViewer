package co.rob.ui.selection;

import co.rob.pojo.FeatureLine;
import co.rob.state.ReportsModel;
import co.rob.util.ForensicPath;

import javax.swing.*;

import static co.rob.io.features.FeatureLineUtils.isBlank;
import static co.rob.io.features.FeatureLineUtils.isFromReport;

/**
 * The <code>FeatureComboBoxModel</code> class extends <code>DefaultComboBoxModel</code>
 * to provide a list of selectable features for a ComboBox.
 * It is supplied with features as feature selections are made.
 */
@Deprecated(since = "Maybe unused?")
public class FeatureComboBoxModel extends DefaultComboBoxModel<FeatureLine> {

  /**
   * Constructs a feature ComboBox model that adds and selects Feature Lines
   * from the image model as selections are made.
   * The feature list may be removed via <code>super.removeAllElements()</code>.
   */
  public FeatureComboBoxModel(FeatureLineSelectionManager featureLineSelectionManager) {
      featureLineSelectionManager.addFeatureLineSelectionManagerChangedListener((_, _, featureLine) -> {
          // feature lines are navigable if they have a valid path
          // but not if they have a histogram field
          if (!isBlank(featureLine) && !ForensicPath.isHistogram(featureLine.firstField())) { //changed from forensic path to match ImageModel
              selectFeature(featureLine);
          }
      });
  }

  /**
   * Selects the feature, creating it if it is not already there.
   */
  public void selectFeature(FeatureLine featureLine) {
    // if blank then select null
    if (isBlank(featureLine)) {
      doSelect(null);
      return;
    }

    // if available, select an equivalent feature line
    for (int i=0; i<getSize(); i++) {
      FeatureLine residentFeatureLine = getElementAt(i);
      if (residentFeatureLine.equals(featureLine)) {

        // select the feature line
        doSelect(residentFeatureLine);
        return;
      }
    }

    // it is new, so add it and then select it
    addElement(featureLine);
    doSelect(featureLine);
  }

  private void doSelect(FeatureLine featureLine) {
    if (featureLine != getSelectedItem()) {
      setSelectedItem(featureLine); // in DefaultComboBoxModel
    }
  }

  /**
   * Clear the feature history.
   */
  public void removeAllFeatures() {
    removeAllElements();
  }

  /**
   * Clear the feature history of features associated with the given Report.
   */
  public void removeAssociatedFeatures(ReportsModel.ReportTreeNode reportTreeNode) {

    // if the selected item is going to be removed, clear it or else the
    // model will assign another one
    FeatureLine selectedFeatureLine = (FeatureLine)getSelectedItem();
    if (selectedFeatureLine != null && isFromReport(selectedFeatureLine, reportTreeNode)) {
      setSelectedItem(null);
    }
    // DefaultComboBoxModel doesn't provide an iterator so copy to an array for use below
    FeatureLine[] featureLines = new FeatureLine[getSize()];
    for (int i=0; i<featureLines.length; i++) {
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

