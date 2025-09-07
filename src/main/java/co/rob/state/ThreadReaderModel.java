package co.rob.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * The <code>ThreadReaderModel</code> monitors a stream and fires events when data arrives.
 */
public class ThreadReaderModel extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ThreadReaderModel.class);

    private final BufferedReader bufferedReader;
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public ThreadReaderModel(BufferedReader bufferedReader) {
        this.bufferedReader = bufferedReader;
        start();
    }

    // entry point
    public void run() {
        // notify lines of input until buffered reader closes
        try {
            String input;
            while ((input = bufferedReader.readLine()) != null) {
                // fire each line as an event
                propertyChangeSupport.firePropertyChange("line", null, input);
            }
        } catch (IOException e) {
            logger.error("ThreadReaderModel: I/O aborted", e);
        }
    }

    /**
     * Adds a <code>PropertyChangeListener</code>
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes <code>PropertyChangeListener</code>
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
}

