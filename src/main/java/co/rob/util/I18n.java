package co.rob.util;

import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {

    private static Locale currentLocale = Locale.ENGLISH;
    private static ResourceBundle bundle = ResourceBundle.getBundle("Dialogs", currentLocale);


    public static String textFor(String text) {
        return bundle.getString(text);
    }

    public static Locale getLocale() {
        if (currentLocale == null) {
            currentLocale = Locale.of(System.getProperty("user.language"), System.getProperty("user.country"));
        }
        return currentLocale;
    }

    public void setLocale(Locale locale) {
        currentLocale = locale;
        bundle = ResourceBundle.getBundle("Messages", currentLocale);
    }
}
