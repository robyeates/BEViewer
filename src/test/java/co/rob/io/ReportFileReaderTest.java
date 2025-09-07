package co.rob.io;

import co.rob.util.ReportFileReadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ReportFileReaderTest {

    //TODODO

    private ReportFileReader reader;

    @BeforeEach
    void setUp() {
        reader = new ReportFileReader(null);
    }

    @Test
    void validReportFile_shouldReturnCanonicalFile() throws Exception {
        // Arrange: create a temp file named "report.xml"
        File tempFile = File.createTempFile("report", ".xml");
        File renamedFile = new File(tempFile.getParent(), "report.xml");
        assertTrue(tempFile.renameTo(renamedFile));

        // Act
        File result = reader.validateReportFile(renamedFile.getPath(), "report.xml");

        // Assert
        assertEquals(renamedFile.getCanonicalFile(), result);
    }

    @Test
    void wrongFileName_shouldThrow() {
        File tempFile = new File("wrongname.txt");

        ReportFileReadException ex = assertThrows(
                ReportFileReadException.class,
                () -> reader.validateReportFile(tempFile.getPath(), "report.xml")
        );

        assertTrue(ex.getMessage().contains("does not end in report.xml"));
    }

    @Test
    void directoryInsteadOfFile_shouldThrow() {
        File dir = new File(System.getProperty("java.io.tmpdir"));

        ReportFileReadException ex = assertThrows(
                ReportFileReadException.class,
                () -> reader.validateReportFile(dir.getPath(), "report.xml")
        );

        assertTrue(ex.getMessage().contains("is not a valid Report file"));
    }

    @Test
    void unreadableFile_shouldThrow() throws IOException {
        // Arrange: create a temp file but make it unreadable
        File tempFile = File.createTempFile("report", ".xml");
        File renamedFile = new File(tempFile.getParent(), "report.xml");
        assertTrue(tempFile.renameTo(renamedFile));
        assertTrue(renamedFile.setReadable(false));

        // Act & Assert
        ReportFileReadException ex = assertThrows(
                ReportFileReadException.class,
                () -> reader.validateReportFile(renamedFile.getPath(), "report.xml")
        );

        assertTrue(ex.getMessage().contains("cannot be read"));
    }
}
