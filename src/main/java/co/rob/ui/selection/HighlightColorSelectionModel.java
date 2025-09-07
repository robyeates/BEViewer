package co.rob.ui.selection;

import co.rob.util.GenericPropertyChangeListener;

import javax.inject.Inject;
import java.awt.*;
import java.beans.PropertyChangeSupport;
import java.util.Objects;

public class HighlightColorSelectionModel {

    public static final String SELECTED_COLOR = "selectedColor.change"; //TODO externalise prop change constants
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private Color selectedColor = Color.YELLOW;

    @Inject
    public HighlightColorSelectionModel() {

    }

    public Color getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(Color selectedColor) {
        if (selectedColor == null) {
            selectedColor = getDefaultColor();
        }
        //or do this in a callback since all panes will have to update as well
        if (!Objects.equals(this.selectedColor, selectedColor)) {
            this.selectedColor = selectedColor;
            support.firePropertyChange(SELECTED_COLOR, null, selectedColor);
        }
    }

    protected Color getDefaultColor() {
        return Color.YELLOW;
    }

    public void addPropertyChangeListener(GenericPropertyChangeListener<Color> listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(GenericPropertyChangeListener<Color> listener) {
        support.removePropertyChangeListener(listener);
    }
}
