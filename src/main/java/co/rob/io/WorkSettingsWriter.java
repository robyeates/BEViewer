package co.rob.io;

import co.rob.util.file.FileTools;
import co.rob.state.BookmarksModel;
import co.rob.state.ReportsModel;
import co.rob.ui.dialog.WError;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.IntStream;

@Singleton
public class WorkSettingsWriter {

    @Inject
    public WorkSettingsWriter() {

    }

    /**
     * Exports work settings in XML format to the given filename.
     *
     * @param workSettingsFile the file to export preferences to
     * @return true if successful, false if failed
     */
    public boolean exportWorkSettings(File workSettingsFile, Enumeration<ReportsModel.ReportTreeNode> reportTreeNodeEnumeration, BookmarksModel bookmarksModel) {
        if (!validWorkSettingsFile(workSettingsFile)) {
            return false;
        }

        try {
            // create the DOM doc object
            var builderFactory = DocumentBuilderFactory.newInstance();
            var builder = builderFactory.newDocumentBuilder();
            var doc = builder.newDocument();

            // create the root dfxml element
            var root = doc.createElement("dfxml");
            root.setAttribute("xmloutputversion", "1.0");
            doc.appendChild(root);

            // create the work settings element
            var workSettings = doc.createElement("work_settings");
            root.appendChild(workSettings);

            // fill the Report elements
            Collections.list(reportTreeNodeEnumeration).stream()
                    .map(reportTreeNode -> {
                        var report = doc.createElement("report");
                        report.setAttribute("feature_directory", FileTools.getAbsolutePath(reportTreeNode.featuresDirectory));
                        report.setAttribute("image_file", FileTools.getAbsolutePath(reportTreeNode.reportImageFile));
                        return report;
                    })
                    .forEach(workSettings::appendChild);

            // fill the Bookmark elements via IntStream
            IntStream.range(0, bookmarksModel.size())
                    .mapToObj(bookmarksModel::get)
                    .map(featureLine -> {
                        var bookmark = doc.createElement("bookmark");
                        bookmark.setAttribute("image_file", FileTools.getAbsolutePath(featureLine.reportImageFile()));
                        bookmark.setAttribute("feature_file", FileTools.getAbsolutePath(featureLine.featuresFile()));
                        bookmark.setAttribute("start_byte", Long.toString(featureLine.startByte()));
                        bookmark.setAttribute("num_bytes", Integer.toString(featureLine.numBytes()));
                        // Handling byte arrays
                        bookmark.setAttribute("first_field", Base64.getEncoder().encodeToString(featureLine.firstField()));
                        bookmark.setAttribute("feature_field", Base64.getEncoder().encodeToString(featureLine.featureField()));
                        bookmark.setAttribute("context_field", Base64.getEncoder().encodeToString(featureLine.contextField()));
                        // Handling other fields
                        bookmark.setAttribute("actual_image_file", FileTools.getAbsolutePath(featureLine.actualImageFile()));
                        bookmark.setAttribute("forensic_path", featureLine.forensicPath());
                        bookmark.setAttribute("formatted_feature", featureLine.formattedFeature());
                        bookmark.setAttribute("is_failed_initialization", Boolean.toString(featureLine.isFailedInitialization()));

                        return bookmark;
                    })
                    .forEach(workSettings::appendChild);

            // create transformer for converting DOM doc source to XML-formatted StreamResult object
            var transformerFactory = TransformerFactory.newInstance();
            var transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            var domSource = new DOMSource(doc);

            // create transformer destination to work settings file
            try (var fileOutputStream = new FileOutputStream(workSettingsFile)) {
                var streamResult = new StreamResult(fileOutputStream);
                // transform DOM doc to XML-formatted StreamResult object
                transformer.transform(domSource, streamResult);
            }
            return true;

        } catch (Exception e) {
            WError.showError("Unable to export work settings to file " + workSettingsFile + ".",
                    "BEViewer file error", e);
            return false;
        }
    }

    private boolean validWorkSettingsFile(File workSettingsFile) {
        // make sure the requested filename does not exist
        if (workSettingsFile.exists()) {
            WError.showError("File " + workSettingsFile + " already exists.", "BEViewer file error", null);
            return false;
        }

        // create the output file
        try {
            if (!workSettingsFile.createNewFile()) {
                WError.showError("File " + workSettingsFile + " already exists.", "BEViewer file error", null);
                return false;
            }
        } catch (IOException e) {
            WError.showError("File " + workSettingsFile + " cannot be created.", "BEViewer file error", e);
            return false;
        }
        return true;
    }
}
