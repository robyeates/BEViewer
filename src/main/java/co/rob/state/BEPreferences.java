package co.rob.state;

import co.rob.BEViewer;
import co.rob.DaggerContext;
import co.rob.ui.dialog.WError;
import co.rob.util.ImageLineFormat;
import co.rob.util.PathFormat;
import co.rob.util.UserTextFormatSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * The <code>BEViewer</code> class provides the main entry
 * for the Bulk Extractor Viewer application.
 */
public class BEPreferences {

    private static final Logger logger = LoggerFactory.getLogger(BEPreferences.class);
    // root preferences node
    private static final Preferences preferences = Preferences.userNodeForPackage(BEViewer.class);

    // Defaults
    /**
     * Default show toolbar, {@value}.
     */
    public static final boolean DEFAULT_SHOW_TOOLBAR = true;
    /**
     * Default font size for Feature listing, {@value}.
     */
    public static final int DEFAULT_FEATURE_FONT_SIZE = 12;
    /**
     * Default font size for Image listing, {@value}.
     */
    public static final int DEFAULT_IMAGE_FONT_SIZE = 12;
    /**
     * Default forensic path numeric base hex, {@value}.
     */
    public static final boolean DEFAULT_FORENSIC_PATH_NUMERIC_BASE_HEX = false;
    /**
     * Default format for presenting Image lines, {@value}.
     */
    public static final String DEFAULT_IMAGE_LINE_FORMAT = "Text";
    /**
     * Default request for hiding the referenced feature view, {@value}.
     */
    public static final boolean DEFAULT_REQUEST_HIDE_REFERENCED_FEATURE_VIEW = false;
    /**
     * Default to show stoplist files, {@value}.
     */
    public static final boolean DEFAULT_SHOW_STOPLIST_FILES = false;
    /**
     * Default to show empty files, {@value}.
     */
    public static final boolean DEFAULT_SHOW_EMPTY_FILES = false;

    public BEPreferences() {

    }

    /**
     * clears all preferences
     */
    public static void clearPreferences() {
        try {
            preferences.removeNode();
//      preferences.flush();
            logger.info("BEPreferences.clearPreferences: preferences cleared.");
        } catch (Exception e) {
            // report error but do not rethrow error
            WError.showError("Failure while clearing user preferences.", "BEViewer Preferences Error", e);
        }
    }

    /**
     * used only during validation testing.
     */
    public static String getPreferencesString() throws Exception {
        ByteArrayOutputStream preferencesStream = new ByteArrayOutputStream();
        preferences.exportSubtree(preferencesStream);
        return preferencesStream.toString();
    }

    // load user preferences saved locally from last run, if available, else load defaults
    public static void loadPreferences(Consumer<Boolean> toolbarConsumer) {
        var reportsModel = DaggerContext.get().reportsModel();
        var featuresModel = DaggerContext.get().featuresModel();
        var referencedFeaturesModel = DaggerContext.get().referencedFeaturesModel();
        var imageView = DaggerContext.get().imageView();

        var reportSelectionManager = DaggerContext.get().reportSelectionManager();

        // load preference to show the toolbar
        boolean showToolbar = preferences.getBoolean("show_toolbar", DEFAULT_SHOW_TOOLBAR);
        toolbarConsumer.accept(showToolbar);
        //toolbar.setVisible(showToolbar);

        // load feature font size
        int featureFontSize = preferences.getInt("feature_font_size", DEFAULT_FEATURE_FONT_SIZE);
        featuresModel.setFontSize(featureFontSize);
        referencedFeaturesModel.setFontSize(featureFontSize);

        // load image file font size
        int imageFontSize = preferences.getInt("image_font_size", DEFAULT_IMAGE_FONT_SIZE);
        imageView.setFontSize(imageFontSize);

        // load forensic path numeric base
        boolean forensicPathNumericBaseHex = preferences.getBoolean("forensic_path_numeric_base_hex", DEFAULT_FORENSIC_PATH_NUMERIC_BASE_HEX);
        UserTextFormatSettings.setPathFormat(forensicPathNumericBaseHex ? PathFormat.HEX_FORMAT : PathFormat.TEXT_FORMAT);
        //featuresModel.setUseHexPath(forensicPathNumericBaseHex);
        //imageView.setUseHexPath(forensicPathNumericBaseHex);

        // load the image line format, note that ImageLine.LineFormat establishes the default
        String imageLineFormat = preferences.get("image_line_format", DEFAULT_IMAGE_LINE_FORMAT);
        //BEViewer.imageView.setLineFormat(ImageLine.LineFormat.lookup(imageLineFormat));
        UserTextFormatSettings.setLineFormat(ImageLineFormat.lookup(imageLineFormat));

        // load preference to hide the referenced feature view
        boolean requestHide = preferences.getBoolean("request_hide_referenced_feature_view", DEFAULT_REQUEST_HIDE_REFERENCED_FEATURE_VIEW);
        reportSelectionManager.setRequestHideReferencedFeatureView(requestHide);

        // load preference to show stoplist files
        boolean requestShowStoplistFiles = preferences.getBoolean("show_stoplist_files", DEFAULT_SHOW_STOPLIST_FILES);
        reportsModel.setIncludeStoplistFiles(requestShowStoplistFiles);

        // load preference to show empty files
        boolean requestShowEmptyFiles = preferences.getBoolean("show_empty_files", DEFAULT_SHOW_EMPTY_FILES);
        reportsModel.setIncludeEmptyFiles(requestShowEmptyFiles);

        // load test settings
        //TODO switchable flag for startup to enable tests - toolbar disabled for now
        //WTest.testWorkSettingsFileString = preferences.get("test_work_settings_file", "");
        //WTest.testOutputDirectoryString = preferences.get("test_output_directory", "");
        //WTest.testScanDirective = preferences.get("test_scan_directive", "bulk_extractor,");

        // load saved reports
        DaggerContext.get().reportFileReader().loadSavedReports(preferences.node("saved_reports"));

        // load saved bookmarks
        DaggerContext.get().bookmarksReader().loadSavedBookmarks(preferences.node("saved_bookmarks"));
    }


    // save user preferences set during run, then quit
    public void savePreferences(boolean toolbarVisible) {

        var reportsModel = DaggerContext.get().reportsModel();
        var bookmarksModel = DaggerContext.get().bookmarksModel();
        var featuresModel = DaggerContext.get().featuresModel();
        var referencedFeaturesModel = DaggerContext.get().referencedFeaturesModel();
        var imageView = DaggerContext.get().imageView();
        var imageModellll = DaggerContext.get().imageModel();


        var rangeSelectionManager = DaggerContext.get().rangeSelectionManager();
        var reportSelectionManager = DaggerContext.get().reportSelectionManager();
        var featureLineSelectionManager = DaggerContext.get().featureLineSelectionManager();

        var bookmarksWriter = DaggerContext.get().bookmarksWriter();

        try {

            // preference to show the toolbar
            preferences.putBoolean("show_toolbar", toolbarVisible);

            // feature font size
            preferences.putInt("feature_font_size", featuresModel.getFontSize());

            // image file font size
            preferences.putInt("image_font_size", imageView.getFontSize());

            // forensic path numeric base, the same for featuresModel and imageView
            preferences.putBoolean("forensic_path_numeric_base_hex", UserTextFormatSettings.getPathFormat() == PathFormat.HEX_FORMAT);

            // image line format
            preferences.put("image_line_format", UserTextFormatSettings.getLineFormat().name());

            // hide referenced feature view
            preferences.putBoolean("request_hide_referenced_feature_view", reportSelectionManager.isRequestHideReferencedFeatureView());

            // preference to show stoplist files
            preferences.putBoolean("show_stoplist_files", reportsModel.isIncludeStoplistFiles());

            // preference to show empty files
            preferences.putBoolean("show_empty_files", reportsModel.isIncludeEmptyFiles());

            // save test settings
            //TODO switchable flag for startup to enable tests - toolbar disabled for now
           //preferences.put("test_work_settings_file", WTest.testWorkSettingsFileString);
           //preferences.put("test_output_directory", WTest.testOutputDirectoryString);
           //preferences.put("test_scan_directive", WTest.testScanDirective);

            // save reports
            DaggerContext.get().reportFileReader().saveReports(preferences.node("saved_reports"));

            // save bookmarks
            bookmarksWriter.saveBookmarksAsync(preferences.node("saved_bookmarks"));

        } catch (Exception e) {
            // report error but do not rethrow error
            WError.showError("Unable to save user preferences.", "BEViewer Preferences Error", e);
        }
    }






}

