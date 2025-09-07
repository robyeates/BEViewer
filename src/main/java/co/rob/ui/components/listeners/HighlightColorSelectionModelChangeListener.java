package co.rob.ui.components.listeners;

import co.rob.util.GenericPropertyChangeListener;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;


public record HighlightColorSelectionModelChangeListener(
        JComponent component) implements GenericPropertyChangeListener<Color> {

    @Override
    public void propertyChange(PropertyChangeEvent evt, Color oldValue, Color newValue) {
        component.repaint();
    }
}
