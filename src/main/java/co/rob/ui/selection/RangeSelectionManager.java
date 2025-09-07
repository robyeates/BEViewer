package co.rob.ui.selection;


import co.rob.ui.CopyableLineProvider;
import co.rob.ui.dialog.WError;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * The <code>RangeSelectionManager</code> class manages a single range selection
 * offered by a <code>CopyableLineInterface</code> provider
 * and tracks the data provider behind it.
 * Obtain the actual selection using the data provider.
 */
@Singleton
public class RangeSelectionManager {

    // resources
    private static final Clipboard selectionClipboard = Toolkit.getDefaultToolkit().getSystemSelection();
    private static final Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    // state
    private CopyableLineProvider provider = null;
    private int minSelectionIndex = -1;
    private int maxSelectionIndex = -1;

    // the selection as a Transferable object
    private Transferable transferableSelection = new StringSelection("");
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    @Inject
    public RangeSelectionManager() {
    }

    /**
     * Selects the provider and range.
     */
    public void setRange(CopyableLineProvider provider, int minSelectionIndex, int maxSelectionIndex) {
        if (provider == this.provider && minSelectionIndex == this.minSelectionIndex && maxSelectionIndex == this.maxSelectionIndex) {
            // no change
            return;
        }

        // change the selection provider and range
        this.provider = provider;
        this.minSelectionIndex = minSelectionIndex;
        this.maxSelectionIndex = maxSelectionIndex;

        // set the selection as a Transferable object
        transferableSelection = getTransferableSelection();

        // if supported, set the system selection clipboard
        // Note that the system selection clipboard is not the system clipboard.
        setSelectionClipboard(transferableSelection);

        // fire changed
        propertyChangeSupport.firePropertyChange("RangeSelectionManager.setSelection", null, null);
    }

    /**
     * Clears any selected provider and range.
     */
    public void clear() {
        setRange(null, -1, -1);
    }

    /**
     * Returns the selected provider or null.
     */
    public CopyableLineProvider getProvider() {
        return provider;
    }

    /**
     * Returns the smallest selected index, inclusive.
     */
    public int getMinSelectionIndex() {
        return minSelectionIndex;
    }

    /**
     * Returns the largest selected index, inclusive.
     */
    public int getMaxSelectionIndex() {
        return maxSelectionIndex;
    }

    /**
     * Indicates whether the selection manager has an active selection.
     */
    public boolean hasSelection() {
        // a provider is defined and the range is valid
        return (provider != null && minSelectionIndex > -1);
    }

    /**
     * Returns the range as a Transferable object created from strings delimited with newlines
     * suitable for copying to a Clipboard object.
     */
    public Transferable getSelection() {
        return transferableSelection;
    }

    // Returns the range as a Transferable delimited with newlines.
    private Transferable getTransferableSelection() {
        if (provider == null || minSelectionIndex == -1) {
            return new StringSelection("");
        }

        // create a string buffer for containing the selection
        StringBuilder builder = new StringBuilder();

        if (minSelectionIndex == maxSelectionIndex) {
            // no "\n" appended
            builder.append(provider.getCopyableLine(minSelectionIndex));
        } else {

            // loop through each selected line to compose the text
            for (int line = minSelectionIndex; line <= maxSelectionIndex; line++) {

                // append the composed line to the selection buffer
                builder.append(provider.getCopyableLine(line));
                builder.append("\n");
            }
        }

        String selection = builder.toString();
        return new StringSelection(selection);
    }

    /**
     * Static convenience function for setting the System clipboard with a Transferable object.
     */
    public static void setSystemClipboard(Transferable transferable) {
        // copy the log to the system clipboard
        try {
            // put text onto Clipboard
            systemClipboard.setContents(transferable, null);
        } catch (IllegalStateException exception) {
            WError.showError("Copy Log to System Clipboard failed.", "BEViewer System Clipboard error", exception);
        }
    }

    /**
     * Static convenience function for setting the Selection clipboard with a Transferable object.
     */
    public static void setSelectionClipboard(Transferable transferable) {
        try {
            // put text onto Clipboard
            if (selectionClipboard != null) {
                selectionClipboard.setContents(transferable, null);
            }
        } catch (IllegalStateException exception) {
            WError.showError("Copy Log to Selection Clipboard failed.", "BEViewer Selection Clipboard error", exception);
        }
    }

    /**
     * Adds an <code>Observer</code> to the listener list.
     *
     * @param selectionManagerChangedListener the <code>Observer</code> to be added
     */
    public void addRangeSelectionManagerChangedListener(PropertyChangeListener selectionManagerChangedListener) {
        propertyChangeSupport.addPropertyChangeListener(selectionManagerChangedListener);
    }

    /**
     * Removes <code>Observer</code> from the listener list.
     *
     * @param selectionManagerChangedListener the <code>Observer</code> to be removed
     */
    public void removeRangeSelectionManagerChangedListener(PropertyChangeListener selectionManagerChangedListener) {
        propertyChangeSupport.removePropertyChangeListener(selectionManagerChangedListener);
    }
}

