package co.rob.state;

import co.rob.util.UTF8Tools;

import javax.inject.Inject;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * The <code>UserHighlightModel</code> manages user highlight settings.
 */

public class UserHighlightModel {

    // input settings
    private byte[] highlightBytes = new byte[0];
    private boolean highlightMatchCase = true;

    // calculated output
    private final List<byte[]> highlights  = new ArrayList<>();

    // resources
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    @Inject
    public UserHighlightModel(){

    }

    /**
     * Sets the text to be highlighted
     * by filling byte[] and char[] Vectors
     * suitable for Image and Feature views.
     */
    public void setHighlightBytes(byte[] newBytes) {
        Objects.requireNonNull(newBytes, "highlightBytes cannot be null");

        if (UTF8Tools.bytesMatch(highlightBytes, this.highlightBytes)) {
            // no change
            return;
        }

        // accept highlight bytes and respond to change
        var old = this.highlightBytes;
        this.highlightBytes = newBytes;

        // clear existing values
        highlights.clear();

        if (highlightBytes.length > 0) {
            // there are user highlights to parse
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            for (byte highlightByte : highlightBytes) {
                if (highlightByte == '|' && s.size() > 0) {
                    // add highlight to vector and clear stream
                    highlights.add(s.toByteArray());
                    s.reset();
                } else {
                    // add byte to stream
                    s.write(highlightByte);
                }
            }

            // add highlight
            if (s.size() > 0) {
                highlights.add(s.toByteArray());
                s.reset();
            }
        }

        // signal model changed
        propertyChangeSupport.firePropertyChange("highlightBytes", old, newBytes);

        propertyChangeSupport.firePropertyChange("highlights", null, List.copyOf(highlights));
    }

    /**
     * Returns highlight string that the user has set.
     */
    public byte[] getHighlightBytes() {
        return highlightBytes;
    }

    /**
     * Returns Vector<byte[]> from user's highlightBytes which may include escape codes.
     *
     * Should be fetched from pcs prop change indirectly
     */
   public List<byte[]> getUserHighlightByteVector() {
       return highlights;
   }

    /**
     * returns highlight match case.
     */
    public boolean isHighlightMatchCase() {
        return highlightMatchCase;
    }

    /**
     * Sets highlight match case.
     */
    public void setHighlightMatchCase(boolean highlightMatchCase) {
        // ignore if no change
        if (this.highlightMatchCase == highlightMatchCase) {
            return;
        }
        var old = this.highlightMatchCase;
        this.highlightMatchCase = highlightMatchCase;
        // signal model changed
        propertyChangeSupport.firePropertyChange("highlightMatchCase", old, highlightMatchCase);
    }

    // ************************************************************
    // listener registry
    // ************************************************************

    /**
     * Adds an <code>PropertyChangeListener</code> to PropertyChangeSupport.
     * This could be a <code>Consumer</code> but PCS plays better with Swing
     *
     * @param highlightChangedListener the <code>PropertyChangeListener</code> to be added
     */
    public void addUserHighlightModelChangedListener(PropertyChangeListener highlightChangedListener) {
        propertyChangeSupport.addPropertyChangeListener(highlightChangedListener);
    }

    /**
     * Removes <code>PropertyChangeListener</code> from PropertyChangeSupport.
     *
     * @param highlightChangedListener the <code>Observer</code> to be removed
     */
    public void removeUserHighlightModelChangedListener(PropertyChangeListener highlightChangedListener) {
        propertyChangeSupport.removePropertyChangeListener(highlightChangedListener);
    }
}

