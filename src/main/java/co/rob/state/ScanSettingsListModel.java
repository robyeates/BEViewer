package co.rob.state;

import co.rob.pojo.scan.ScanSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Manages the jobs list, provides list capability for a JList,
 * and provides a list selection model suitable for this use case,
 * all in a threadsafe way.
 * <p>
 * This should be a static singleton class, but abstract class
 * AbstractListModel requires that it not be static.
 * <p>
 * Consumer: use ListDataListener to listen for intervalAdded event,
 * then try to consume a ScanSettings job object, if available.
 * Consumer must loop to consume queued jobs.
 * <p>
 * To avoid broken synchronization state issues between Swing and consumer
 * threads, all interfaces run on the Swing thread.
 * When interfaces are called from other threads, they block
 * until the Swing thread completes the actions on its behalf.
 */
@Singleton
public class ScanSettingsListModel extends AbstractListModel<ScanSettings> {

    private static final Logger logger = LoggerFactory.getLogger(ScanSettingsListModel.class);

    private final DefaultListSelectionModel selectionModel;

    // the jobs being managed by this sync list model
    private final List<ScanSettings> jobs = Collections.synchronizedList(new ArrayList<>());

    // AbstractModel requires that this singleton resource be non-static
    @Inject
    public ScanSettingsListModel() {
        selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /**
     * Add ScanSettings to tail (bottom) of LIFO job queue
     * and increment the semaphore using the Swing thread.
     */
    public void add(ScanSettings scanSettings) {
        callOnEventDispatchThread(() -> {
            logger.info("ScanSettingsListModel.add job '{}'", scanSettings.getCommandString());
            jobs.add(scanSettings);
            int last = jobs.size() - 1;
            fireIntervalAdded(this, last, last);
            selectionModel.setSelectionInterval(last, last);
            return null;
        });
    }

    /**
     * Remove and return ScanSettings from head (top) of LIFO job queue
     * and decrement the semaphore else return null.
     */
    public ScanSettings remove() {
        return callOnEventDispatchThread(() -> {
            if (jobs.isEmpty()) {
                logger.info("ScanSettingsRunQueue.remove top: no top element to remove");
                return null;
            }
            ScanSettings s = jobs.removeFirst();
            logger.info("ScanSettingsListModel.remove top job '{}'", s.getCommandString());
            fireIntervalRemoved(this, 0, 0);
            return s;
        });
    }

    /**
     * Remove specified ScanSettings job from within the job queue
     * and decrement the semaphore else return null.
     */
    public boolean remove(ScanSettings scanSettings) {
        return callOnEventDispatchThread(() -> {
            int index = jobs.indexOf(scanSettings);
            if (index < 0) {
                logger.info("ScanSettingsRunQueue.remove scanSettings: no element to remove");
                return false;
            }
            logger.info("ScanSettingsListModel.remove job '{}'", scanSettings.getCommandString());
            jobs.remove(index);
            fireIntervalRemoved(this, index, index);
            return true;
        });
    }

    /**
     * Move ScanSettings up toward the top of the queue.
     */
    public boolean moveUp(ScanSettings scanSettings) {
        return callOnEventDispatchThread(() -> {
            int n = jobs.indexOf(scanSettings);
            if (n > 0) {
                ScanSettings displaced = jobs.set(n - 1, scanSettings);
                jobs.set(n, displaced);
                fireContentsChanged(this, n - 1, n);
                selectionModel.setSelectionInterval(n - 1, n - 1);
                return true;
            }
            logger.info("ScanSettingsRunQueue.moveUp: failure at index {}", n);
            return false;
        });
    }

    /**
     * Move ScanSettings down toward the bottom of the queue.
     */
    public boolean moveDown(ScanSettings scanSettings) {
        return callOnEventDispatchThread(() -> {
            int n = jobs.indexOf(scanSettings);
            if (n >= 0 && n < jobs.size() - 1) {
                ScanSettings displaced = jobs.set(n + 1, scanSettings);
                jobs.set(n, displaced);
                fireContentsChanged(this, n, n + 1);
                selectionModel.setSelectionInterval(n + 1, n + 1);
                return true;
            }
            logger.info("ScanSettingsRunQueue.moveDown: failure at index {}", n);
            return false;
        });
    }

    /**
     * Number of scan settings enqueued.
     * Required by interface ListModel.
     */
    @Override
    public int getSize() {
        return callOnEventDispatchThread(jobs::size);
    }

    /**
     * Element at index.
     * Required by interface ListModel.
     */
    @Override
    public ScanSettings getElementAt(int index) {
        return callOnEventDispatchThread(() -> jobs.get(index));
    }

    public DefaultListSelectionModel getSelectionModel() {
        return selectionModel;
    }


    private <T> T callOnEventDispatchThread(Callable<T> task) {
        if (SwingUtilities.isEventDispatchThread()) {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            final Object[] result = new Object[1];
            try {
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        result[0] = task.call();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                logger.error("ScanSettingsListModel.callOnEdt error", e);
            }
            return (T) result[0];
        }
    }
}

