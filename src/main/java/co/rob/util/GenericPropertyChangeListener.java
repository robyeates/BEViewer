package co.rob.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

@FunctionalInterface
public interface GenericPropertyChangeListener<T> extends PropertyChangeListener {

    @SuppressWarnings("unchecked")
    @Override
    default void propertyChange(PropertyChangeEvent evt) {
        propertyChange(evt, (T) evt.getOldValue(), (T) evt.getNewValue());
    }

    void propertyChange(PropertyChangeEvent evt, T oldValue, T newValue);
}
