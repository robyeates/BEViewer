package co.rob.ui.icons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;

//TODO - this could be changed at runtime, maybe on resize?
public final class BEIcons {

    private static final Logger logger = LoggerFactory.getLogger(BEIcons.class);

    private static final String PATH = "/icons/light/";
    private static final String PATH_DARK = "/icons/dark/";

    public static final Icon CLOSE_16_LIGHT = getLightIcon("close.svg", 16);
    public static final Icon CLOSE_16_DARK = getDarkIcon("close_dark.svg", 16);

    public static final Icon MANAGE_BOOKMARKS_16_LIGHT = getLightIcon("bookmarksList.svg", 20);
    public static final Icon MANAGE_BOOKMARKS_16_DARK = getDarkIcon("bookmarksList_dark.svg", 20);

    public static final Icon ADD_BOOKMARK_16_LIGHT = getLightIcon("bookmark.svg", 20);
    public static final Icon ADD_BOOKMARK_16_DARK = getDarkIcon("bookmark_dark.svg", 20);

    public static final Icon EXPORT_BOOKMARKS_16_LIGHT = getLightIcon("export.svg", 20);
    public static final Icon EXPORT_BOOKMARKS_16_DARK = getDarkIcon("export_dark.svg", 20);

    public static final Icon DELETE_16_LIGHT = getLightIcon("delete.svg", 20);
    public static final Icon DELETE_16_DARK = getDarkIcon("delete_dark.svg", 20);
    public static final Icon DELETE_24_LIGHT = getLightIcon("delete.svg", 24);
    public static final Icon DELETE_24_DARK = getDarkIcon("delete_dark.svg", 24);

    public static final Icon EDIT_16_LIGHT = getLightIcon("edit.svg", 20);
    public static final Icon EDIT_16_DARK = getDarkIcon("edit_dark.svg", 20);

    public static final Icon EDIT_24_LIGHT = getLightIcon("edit.svg", 24);
    public static final Icon EDIT_24_DARK = getDarkIcon("edit_dark.svg", 24);

    public static final Icon UP_16_LIGHT = getLightIcon("up.svg", 20);
    public static final Icon UP_16_DARK = getDarkIcon("up_dark.svg", 20);
    public static final Icon UP_24_LIGHT = getLightIcon("up.svg", 24);
    public static final Icon UP_24_DARK = getDarkIcon("up_dark.svg", 24);

    public static final Icon DOWN_16_LIGHT = getLightIcon("down.svg", 20);
    public static final Icon DOWN_16_DARK = getDarkIcon("down_dark.svg", 20);
    public static final Icon DOWN_24_LIGHT = getLightIcon("down.svg", 24);
    public static final Icon DOWN_24_DARK = getDarkIcon("down_dark.svg", 24);

    public static final Icon REVERSE_16_LIGHT = getLightIcon("left.svg", 20);
    public static final Icon REVERSE_16_DARK = getDarkIcon("left_dark.svg", 20);

    public static final Icon FORWARD_16_LIGHT = getLightIcon("right.svg", 20);
    public static final Icon FORWARD_16_DARK = getDarkIcon("right_dark.svg", 20);

    public static final Icon HOME_16_LIGHT = getLightIcon("home.svg", 20);
    public static final Icon HOME_16_DARK = getDarkIcon("home_dark.svg", 20);

    public static final Icon WARNING_16_LIGHT = getLightIcon("warning.svg", 20);
    public static final Icon WARNING_16_DARK = getDarkIcon("warning_dark.svg", 20);

    public static final Icon OPEN_REPORT_16_LIGHT = getLightIcon("open.svg", 20);
    public static final Icon OPEN_REPORT_16_DARK = getDarkIcon("open_dark.svg", 20);

    public static final Icon COPY_16_LIGHT = getLightIcon("copy.svg", 20);
    public static final Icon COPY_16_DARK = getDarkIcon("copy_dark.svg", 20);

    public static final Icon RUN_BULK_EXTRACTOR_16_LIGHT = getLightIcon("run.svg", 20);
    public static final Icon RUN_BULK_EXTRACTOR_16_DARK = getDarkIcon("run_dark.svg", 20);

    public static final Icon RUN_BULK_EXTRACTOR_24_LIGHT = getLightIcon("run.svg", 24);
    public static final Icon RUN_BULK_EXTRACTOR_24_DARK = getDarkIcon("run_dark.svg", 24);

    public static final Icon PRINT_FEATURE_16_LIGHT = getLightIcon("print.svg", 20);
    public static final Icon PRINT_FEATURE_16_DARK = getDarkIcon("print_dark.svg", 20);

    public static final Icon THEME_PICKER_LIGHT_MODE = getLightIcon("lightMode.svg", 20);
    public static final Icon THEME_PICKER_DARK_MODE = getDarkIcon("darkMode.svg", 20);

    public static final Icon HELP_16_LIGHT = getLightIcon("help.svg", 20);
    public static final Icon HELP_16_DARK = getDarkIcon("help_dark.svg", 20);
    public static final Icon HELP_ABOUT_16_LIGHT = getLightIcon("help_about.svg", 20);
    public static final Icon HELP_ABOUT_16_DARK = getDarkIcon("help_about_dark.svg", 20);
    public static final Icon EXIT_16_LIGHT = getLightIcon("exit.svg", 20);
    public static final Icon EXIT_16_DARK = getDarkIcon("exit_dark.svg", 20);

    private BEIcons() {
    }

    private static Icon getIcon(String pathPrefix, String name, int size) {
        try {
            return SvgIconLoader.loadSvgIcon(pathPrefix + name, size);
        } catch (NullPointerException | IOException e) {
            logger.error("Error loading image in getIcon16 - path prefix [{}] name [{}]", pathPrefix, name);
            throw new RuntimeException(e);
        }
    }

    private static Icon getLightIcon(String name, int size) {
        return getIcon(PATH, name, size);
    }

    private static Icon getDarkIcon(String name, int size) {
        return getIcon(PATH_DARK, name, size);
    }
}

