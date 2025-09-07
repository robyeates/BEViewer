package co.rob.pojo;

import org.jetbrains.annotations.NotNull;

public record ImagePathPreference(String name) {
    // class for enumerating the requested image source
        public static final ImagePathPreference DEFAULT = new ImagePathPreference("Default");
        public static final ImagePathPreference CUSTOM = new ImagePathPreference("Custom");
        public static final ImagePathPreference NONE = new ImagePathPreference("None");

        @NotNull
        public String toString() {
            return name;
        }
    }
