package co.rob.io;

import co.rob.DaggerContext;
import co.rob.io.features.FeatureLineFactory;
import co.rob.state.BookmarksModel;
import co.rob.ui.dialog.WError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

import static java.util.concurrent.CompletableFuture.supplyAsync;

@Singleton
public class BookmarksReader {

    private static final Logger logger = LoggerFactory.getLogger(BookmarksReader.class);
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Inject
    public BookmarksReader() {
    }

    // load saved bookmarks
    public void loadSavedBookmarks(Preferences bookmarksPreferences) {
        BookmarksModel bookmarksModel = DaggerContext.get().bookmarksModel();

        supplyAsync(() -> {
            int i = 0;
            while (true) {
                String bookmarkIndex = Integer.toString(i);

                String reportImageFileString = bookmarksPreferences.get("image_file_" + bookmarkIndex, null);
                File reportImageFile = (reportImageFileString == null) ? null : new File(reportImageFileString);

                String featuresFileString = bookmarksPreferences.get("features_file_" + bookmarkIndex, null);
                File featuresFile = (featuresFileString == null) ? null : new File(featuresFileString);

                long startByte = bookmarksPreferences.getLong("start_byte_" + bookmarkIndex, 0);
                int numBytes = bookmarksPreferences.getInt("num_bytes_" + bookmarkIndex, 0);

                // stop loading bookmarks when there are no more saved values
                if (featuresFile == null) {
                    break;
                }

                if (reportImageFile != null && !reportImageFile.exists()) {
                    WError.showError(
                            "The image file for the Feature being restored is not available:" +
                                    "\nImage file: " + reportImageFileString +
                                    "\nFeature file: " + featuresFileString +
                                    "\nFeature index: " + startByte,
                            "BEViewer Preferences Error", null);
                }

                if (!featuresFile.exists()) {
                    WError.showError(
                            "Unable to restore feature from saved preferences because the Feature file is not available:" +
                                    "\nImage file: " + reportImageFileString +
                                    "\nFeature file: " + featuresFileString +
                                    "\nFeature index: " + startByte,
                            "BEViewer Preferences Error", null);
                    break;
                }

                try {
                    var featureLine = FeatureLineFactory.create(reportImageFile, featuresFile, startByte, numBytes);
                    bookmarksModel.addElement(featureLine);
                } catch (Exception e) {
                    WError.showError(
                            "Unable to restore feature from saved preferences:" +
                                    "\nImage file: " + reportImageFileString +
                                    "\nFeature file: " + featuresFileString +
                                    "\nFeature index: " + startByte,
                            "BEViewer Preferences Error", e);
                }

                i++;
            }
            return bookmarksModel;
        }, executor).thenAccept(model -> {
            logger.info("BEPreferences: loadSavedBookmarks done. Restored [{}] bookmarks",
                    model.getBookmarks().size());
        }).exceptionally(ex -> {
            WError.showError("Failed to restore saved bookmarks", "BEViewer Preferences Error", ex);
            return null;
        });
    }
}
