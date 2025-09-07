package co.rob;

public class DaggerContext {
    private static volatile AppComponent instance;

    private DaggerContext() {}

    public static AppComponent get() {
        if (instance == null) {
            synchronized (DaggerContext.class) {
                if (instance == null) {
                    instance = DaggerAppComponent.create();
                }
            }
        }
        return instance;
    }

    // allows test override
    public static void set(AppComponent component) {
        instance = component;
    }
}
