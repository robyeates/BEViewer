package co.rob.ui.selection;

import co.rob.pojo.FeatureLine;
import co.rob.util.GenericPropertyChangeListener;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.beans.PropertyChangeSupport;

import static co.rob.util.PropertyChangeConstants.FEATURE_LINE;

/**
 * This simple class provides notification when a FeatureLine is selected
 * and decouples the selected feature line from the ImageModel.
 */
@Singleton
public class FeatureLineSelectionManager {

    // model state
    private FeatureLine featureLine = null;
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);


    @Inject
    public FeatureLineSelectionManager() {
    }

    /**
     * Sets the selected FeatureLine.
     */
    public void setFeatureLineSelection(FeatureLine featureLine) {
        // do nothing if the feature line is already selected
        // determine equivalency, allowing null
        if (featureLine == null || this.featureLine == null) {
            if (featureLine != this.featureLine) {
                var old = this.featureLine;
                this.featureLine = featureLine;
                propertyChangeSupport.firePropertyChange(FEATURE_LINE.propertyName(), old, featureLine);
            }
        } else if (!featureLine.equals(this.featureLine)) {
            var old = this.featureLine;
            this.featureLine = featureLine;
            propertyChangeSupport.firePropertyChange(FEATURE_LINE.propertyName(), old, featureLine);
        }
        // else no change
    }

    /**
     * Returns the selected FeatureLine.
     *
     * TODO should be unused - line is in the event on the callback/prop listener
     */
    public FeatureLine getFeatureLineSelection() {
        return featureLine;
    }

    /**
     * Adds an <code>Observer</code> to the listener list.
     *
     * @param featureLineSelectionManagerChangedListener the <code>Observer</code> to be added
     */
    public void addFeatureLineSelectionManagerChangedListener(GenericPropertyChangeListener<FeatureLine> featureLineSelectionManagerChangedListener) {
        propertyChangeSupport.addPropertyChangeListener(featureLineSelectionManagerChangedListener);
    }

    /**
     * Removes <code>Observer</code> from the listener list.
     *
     * @param featureLineSelectionManagerChangedListener the <code>Observer</code> to be removed
     */
    public void removeFeatureLineSelectionManagerChangedListener(GenericPropertyChangeListener<FeatureLine> featureLineSelectionManagerChangedListener) {
        propertyChangeSupport.removePropertyChangeListener(featureLineSelectionManagerChangedListener);
    }
}

