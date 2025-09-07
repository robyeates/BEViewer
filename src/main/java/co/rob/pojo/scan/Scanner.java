package co.rob.pojo.scan;

/**
 * This class contains a scanner name and whether it is enabled.
 * defaultUseScanner tracks what bulk_extractor wants.
 * useScanner tracks what the user wants.
 */
public final class Scanner {
    private final String name;
    private final boolean defaultUseScanner;
    private boolean useScanner;

    public Scanner(String name, boolean defaultUseScanner) {
        this.name = name;
        this.defaultUseScanner = defaultUseScanner;
        this.useScanner = defaultUseScanner;
    }

    public Scanner(String name, boolean defaultUseScanner, boolean useScanner) {
        this.name = name;
        this.defaultUseScanner = defaultUseScanner;
        this.useScanner = useScanner;
    }

    public String getName() {
        return name;
    }

    public boolean isUseScanner() {
        return useScanner;
    }

    public void setUseScanner(boolean b) {
        useScanner = b;
    }

    public boolean isDefaultUseScanner() {
        return defaultUseScanner;
    }
}