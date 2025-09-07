package co.rob.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class UserTextFormatSettings {
    public static final String PATH_FORMAT_PROPERTY_CHANGE = "pathFormatChange";
    public static final String LINE_FORMAT_PATH_PROPERTY_CHANGE = "lineFormatChange";

    private static final PropertyChangeSupport support = new PropertyChangeSupport(UserTextFormatSettings.getInstance());

    // The single instance of the class
    private static volatile UserTextFormatSettings instance;

    //useHexPath
    private static PathFormat pathFormat = PathFormat.TEXT_FORMAT;
    private static ImageLineFormat imageLineFormat = ImageLineFormat.TEXT_FORMAT;


    public static ImageLineFormat getLineFormat() {
        return imageLineFormat;
    }

    /**
     * Decimal or Hex
     */
    public static PathFormat getPathFormat() {
        return pathFormat;
    }

    public static void setLineFormat(ImageLineFormat newImageLineFormat) {
        ImageLineFormat oldImageLineFormat = imageLineFormat;
        imageLineFormat = newImageLineFormat;
        support.firePropertyChange(LINE_FORMAT_PATH_PROPERTY_CHANGE, oldImageLineFormat, newImageLineFormat);
    }

    //useHexPath
    public static void setPathFormat(PathFormat newPathFormat) {
        PathFormat oldPathFormat = pathFormat;
        pathFormat = newPathFormat;
        support.firePropertyChange(PATH_FORMAT_PROPERTY_CHANGE, oldPathFormat, newPathFormat);
    }

    public void addImageLineFormatPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public static UserTextFormatSettings getInstance() {
        if (instance == null) {
            synchronized (UserTextFormatSettings.class) {
                if (instance == null) {
                    instance = new UserTextFormatSettings();
                }
            }
        }
        return instance;
    }
}
