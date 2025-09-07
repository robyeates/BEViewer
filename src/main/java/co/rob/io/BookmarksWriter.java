package co.rob.io;

import co.rob.DaggerContext;
import co.rob.pojo.FeatureLine;
import co.rob.state.*;
import co.rob.ui.dialog.WError;
import co.rob.util.PathFormat;
import co.rob.util.UserTextFormatSettings;
import co.rob.util.file.FileTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;
import java.util.stream.IntStream;

import static co.rob.io.features.FeatureLineUtils.getSummaryString;
import static co.rob.ui.dialog.WManageBookmarks.closeWindow;
import static java.util.concurrent.CompletableFuture.*;

@Singleton
public class BookmarksWriter {

    private static final Logger logger = LoggerFactory.getLogger(BookmarksWriter.class);
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final BookmarksModel bookmarksModel;

    @Inject
    public BookmarksWriter(BookmarksModel bookmarksModel) {
        this.bookmarksModel = bookmarksModel;
    }

    public void exportBookmarks(File bookmarkFile) {
        if(!validateTargetPath(bookmarkFile)) {
            return;
        }
        supplyAsync(() -> {
            try {
                return new PrintWriter(new BufferedWriter(new FileWriter(bookmarkFile)));
            } catch (IOException e) {
                WError.showError("Unable to write to new file " + bookmarkFile + ".", "BEViewer file error", e);
                throw new CompletionException(e);
            }
        }, executor)
                .thenCompose(writer ->
                        allOf(bookmarksModel.getBookmarks().stream()//probably a bit overkill
                                .map(f -> runAsync(() -> exportText(writer, f), executor))
                                .toArray(CompletableFuture[]::new)
                        ).thenApply(v -> writer)
                )
                .thenAccept(writer -> {
                    writer.close();
                    closeWindow();
                    logger.info("WManageBookmarks: export bookmarks done. Exported [{}] bookmarks", bookmarksModel.getBookmarks().size());
                })
                .exceptionally(ex -> {
                    WError.showError("Export failed for " + bookmarkFile, "BEViewer file error", ex);
                    return null;
                });
    }

    // validate that the export path is empty and writable
    private void exportText(PrintWriter writer, FeatureLine featureLine) {
        // print the summary
        writer.println(getSummaryString(featureLine, UserTextFormatSettings.getPathFormat() == PathFormat.HEX_FORMAT));

        ImageModel bookmarkImageModel = DaggerContext.get().imageModel();
        ImageView bookmarkImageView = DaggerContext.get().imageView();

        bookmarkImageModel.setImageSelection(featureLine);

        // wait until generation is done (could be improved with callbacks)
        while (bookmarkImageModel.isBusy()) {
            Thread.yield();
        }

        var imagePage = bookmarkImageModel.getImagePage();
        bookmarkImageView.setImagePage(imagePage);

        for (int i = 0; i < bookmarkImageView.getNumLines(); i++) {
            var imageLine = bookmarkImageView.getLine(i);
            writer.println(imageLine.text());
        }
        writer.println();
    }

    private static boolean validateTargetPath(File bookmarkFile) {
        // make sure the requested filename does not exist
        if (bookmarkFile.exists()) {
            WError.showError("File '" + bookmarkFile + "' already exists.", "BEViewer file error", null);
            return false;
        }
        // create the output file
        try {
            if (!bookmarkFile.createNewFile()) {
                WError.showError("File '" + bookmarkFile + "' cannot be created.", "BEViewer file error", null);
                return false;
            }
        } catch (IOException e) {
            WError.showError("File '" + bookmarkFile + "' cannot be created.", "BEViewer file error", e);
            return false;
        }
        // verified
        return true;
    }

    // save bookmarks
    public void saveBookmarksAsync(Preferences savedBookmarks) {
        // Clear the node asynchronously
        runAsync(() -> {
            try {
                savedBookmarks.clear();

                // Create a list of async tasks for saving each bookmark
                CompletableFuture<?>[] saveTasks = IntStream.range(0, bookmarksModel.size())
                        .mapToObj(i -> runAsync(() -> {
                            FeatureLine featureLine = bookmarksModel.get(i);

                            savedBookmarks.put("image_file_" + i, FileTools.getAbsolutePath(featureLine.reportImageFile()));
                            savedBookmarks.put("features_file_" + i, FileTools.getAbsolutePath(featureLine.featuresFile()));
                            savedBookmarks.putLong("start_byte_" + i, featureLine.startByte());
                            savedBookmarks.putInt("num_bytes_" + i, featureLine.numBytes());
                        }, executor)).toArray(CompletableFuture[]::new);

                // Wait for all save tasks to complete
                allOf(saveTasks).join();

            } catch (Exception e) {
                throw new CompletionException("Failed to save bookmarks", e);
            }
        }, executor).exceptionally(ex -> {
            WError.showError("Failed to save bookmarks", "BEViewer preferences error", ex);
            return null;
        });
    }
}
