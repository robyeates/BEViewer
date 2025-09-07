package co.rob.io.features;

import co.rob.state.FastFeatureLineTable;
import co.rob.state.FeaturesModel;
import co.rob.ui.dialog.WError;
import co.rob.ui.dialog.WProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

/**
 * Decomposed from FeaturesParserThread
 */
public class FeaturesParserTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(FeaturesParserTask.class);

    private final FeaturesModel callback;
    private final WProgress progressBar;
    private final File featuresFile;
    private final FeaturesParser parser;

    public FeaturesParserTask(FeaturesModel callback, File imageFile, File featuresFile,
                              byte[] requestedFilterBytes, boolean filterMatchCase) {
        this.callback = callback;
        this.progressBar = callback.progressBar;
        this.featuresFile = featuresFile;
        FastFeatureLineTable table = new FastFeatureLineTable(imageFile, featuresFile);

        this.parser = new FeaturesParser(table, new FeaturesFilterMatcher(requestedFilterBytes, filterMatchCase));
    }

    @Override
    public void run() {
        progressBar.setActive(true);
        try (FeaturesFileReader reader = new FeaturesFileReader(featuresFile)) {
            byte[] buffer = new byte[65536];
            int bytesRead;
            long offset = 0;
            while ((bytesRead = reader.readChunk(buffer)) != -1) {
                parser.parseChunk(buffer, bytesRead, offset);
                offset += bytesRead;
                progressBar.setPercent((int)(offset * 100 / reader.size()));
            }
        } catch (IOException e) {
            logger.error("Unable to read [{}]", featuresFile, e);
            WError.showError("Unable to read " + featuresFile, "BEViewer error", e);
        } finally {
            progressBar.setActive(false);
            logger.info("Read file [{}] setting featureLineTable", featuresFile);
            SwingUtilities.invokeLater(() -> callback.setFeatureLineTable(parser.getTable()));
        }
    }
}
