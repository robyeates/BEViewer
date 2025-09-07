package co.rob.ui.components.listeners;

import co.rob.ui.CopyableLineProvider;
import co.rob.ui.selection.RangeSelectionManager;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class RangeSelectionManagerChangedListener implements PropertyChangeListener {

    private final RangeSelectionManager rangeSelectionManager;
    private final CopyableLineProvider copyableLineProvider;
    private int minSelectionIndex;
    private int maxSelectionIndex;
    private final JComponent component;

    public RangeSelectionManagerChangedListener(RangeSelectionManager rangeSelectionManager, CopyableLineProvider copyableLineProvider, int minSelectionIndex, int maxSelectionIndex, JComponent component) {
        this.rangeSelectionManager = rangeSelectionManager;
        this.copyableLineProvider = copyableLineProvider;
        this.minSelectionIndex = minSelectionIndex;
        this.maxSelectionIndex = maxSelectionIndex;
        this.component = component;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (rangeSelectionManager.getProvider() != copyableLineProvider) {
            minSelectionIndex = -1;
            maxSelectionIndex = -1;
            component.repaint();
        }
    }
}
