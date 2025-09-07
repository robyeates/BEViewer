package co.rob.io;

import co.rob.pojo.WorkSettings;
import co.rob.ui.dialog.WError;
import co.rob.util.WorkSettingsFileReadException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Consumer;

@Singleton
public class WorkSettingsReader {

    @Inject
    public WorkSettingsReader() {

    }

    /**
     * Imports work settings in XML format from the given filename.
     *
     * @param workSettingsFile         the file to import preferences from
     * @param keepExistingWorkSettings true to keep settings, false to clear navigation history,
     *                                 reports, readers, and the bookmarks list
     */
    public Optional<WorkSettings> importWorkSettings(File workSettingsFile, boolean keepExistingWorkSettings, Consumer<Void> closeAllReportsCallback) throws WorkSettingsFileReadException {
        try {
            int i;
            // read workSettingsFile into Document
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(workSettingsFile);

            // drill down to work settings element
            Element root = document.getDocumentElement();
            Element element = (Element)(root.getElementsByTagName("work_settings").item(0));

            if (!keepExistingWorkSettings) {
                // dump all the old work settings
                closeAllReportsCallback.accept(null);
            }
            // load the new work settings
            // load Reports
            NodeList reportList = element.getElementsByTagName("report");
            WorkSettings workSettings = new WorkSettings();
            for (i=0; i<reportList.getLength(); i++) {

                // get the report files
                Element report = (Element)reportList.item(i);
                workSettings.captureReportSettings(
                        new File(report.getAttribute("feature_directory")),
                        new File(report.getAttribute("image_file")));

            }

            // load Bookmarks
            NodeList bookmarkList = element.getElementsByTagName("bookmark");
            for (i = 0; i < bookmarkList.getLength(); i++) { //TODO use counter
                // get the bookmark attributes
                Element bookmark = (Element) bookmarkList.item(i);
                // create the feature from the saved values
                workSettings.captureFeatureLines(
                        new File(bookmark.getAttribute("image_file")),
                        new File(bookmark.getAttribute("feature_file")),
                        Long.parseLong(bookmark.getAttribute("start_byte")),
                        Integer.parseInt(bookmark.getAttribute("num_bytes")),
                        // Base64 decoding for byte arrays
                        Base64.getDecoder().decode(bookmark.getAttribute("first_field")),
                        Base64.getDecoder().decode(bookmark.getAttribute("feature_field")),
                        Base64.getDecoder().decode(bookmark.getAttribute("context_field")),
                        // Parsing other fields
                        new File(bookmark.getAttribute("actual_image_file")),
                        bookmark.getAttribute("forensic_path"),
                        bookmark.getAttribute("formatted_feature"),
                        Boolean.parseBoolean(bookmark.getAttribute("is_failed_initialization")));
            }
            return Optional.of(workSettings);
        } catch (Exception e) {
            WError.showError("Unable to load work settings from file " + workSettingsFile + ".",
                    "BEViewer file error", e);
            throw new WorkSettingsFileReadException(e.getMessage());
        }
    }
}
