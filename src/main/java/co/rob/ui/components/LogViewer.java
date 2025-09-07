package co.rob.ui.components;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.recovery.ResilientFileOutputStream;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class LogViewer extends JFrame {
    private final JTextArea logArea;
    private volatile boolean running = true;
    private boolean autoScroll = true;
    private final File logFile;

    private static final int CHUNK_SIZE = 500;   // how many lines to load per scroll-up
    private long loadPosition;                   // where we've loaded up to (from bottom backwards)

    public LogViewer() {
        super("Bulk Extractor Viewer Log");
        this.logFile = getLogFile();

        logArea = new JTextArea();
        logArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(logArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(700,700));

        add(scrollPane, BorderLayout.CENTER);

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Load last N lines first
        loadLastLines();

        // Scroll listener
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            Adjustable adj = e.getAdjustable();
            int max = adj.getMaximum();
            int extent = adj.getVisibleAmount();
            int value = adj.getValue();

            autoScroll = (value + extent >= max - 20);

            if (value == 0) {
                loadOlderLines();
            }
        });

        // Background thread for tailing
        new Thread(this::tailFile).start();
    }

    private File getLogFile() {

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        for (ch.qos.logback.classic.Logger logger : context.getLoggerList()) {
            for (Iterator<Appender<ILoggingEvent>> index = logger.iteratorForAppenders(); index.hasNext();) {
                Appender<ILoggingEvent> appender = index.next();
                if (appender instanceof FileAppender<ILoggingEvent> fileAppender) {
                    ResilientFileOutputStream resilientFileOutputStream = (ResilientFileOutputStream)fileAppender.getOutputStream();
                    return resilientFileOutputStream.getFile();
                }
            }
        }
        throw new RuntimeException("Failed to find log file from LoggerFactory");
    }

    /**
     * Reads last N lines of the file.
     */
    private void loadLastLines() {
        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
            loadPosition = raf.length();

            List<String> lines = readLinesBackward(raf, LogViewer.CHUNK_SIZE);
            Collections.reverse(lines); // show oldest first
            for (String l : lines) {
                logArea.append(l + "\n");
            }

            // Scroll to bottom
            SwingUtilities.invokeLater(() ->
                    logArea.setCaretPosition(logArea.getDocument().getLength())
            );

        } catch (IOException e) {
            e.printStackTrace();//TODO
        }
    }

    /**
     * Loads older lines when user scrolls to top.
     */
    private void loadOlderLines() {
        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
            raf.seek(loadPosition);

            List<String> lines = readLinesBackward(raf, LogViewer.CHUNK_SIZE);
            loadPosition = raf.getFilePointer();

            Collections.reverse(lines);
            StringBuilder sb = new StringBuilder();
            for (String l : lines) {
                sb.append(l).append("\n");
            }

            // Prepend
            int currentPos = logArea.getCaretPosition();
            logArea.insert(sb.toString(), 0);

            // Keep view from jumping
            SwingUtilities.invokeLater(() ->
                    logArea.setCaretPosition(currentPos + sb.length())
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads up to numLines lines backwards from the current file pointer.
     */
    private List<String> readLinesBackward(RandomAccessFile raf, int numLines) throws IOException {
        List<String> result = new ArrayList<>();
        long filePointer = raf.getFilePointer();
        if (filePointer == 0) return result;

        byte[] buffer = new byte[4096];
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();

        while (filePointer > 0 && result.size() < numLines) {
            int readSize = (int) Math.min(buffer.length, filePointer);
            filePointer -= readSize;
            raf.seek(filePointer);
            raf.readFully(buffer, 0, readSize);

            for (int i = readSize - 1; i >= 0; i--) {
                byte b = buffer[i];
                if (b == '\n') {
                    if (lineBuffer.size() > 0) {
                        String line = new StringBuilder(lineBuffer.toString(StandardCharsets.UTF_8))
                                .reverse().toString();
                        result.add(line);
                        lineBuffer.reset();
                        if (result.size() >= numLines) {
                            raf.seek(filePointer + i); // update pointer
                            return result;
                        }
                    }
                } else {
                    lineBuffer.write(b);
                }
            }
        }

        // last line (if file didn't end with \n)
        if (lineBuffer.size() > 0) {
            String line = new StringBuilder(lineBuffer.toString(StandardCharsets.UTF_8))
                    .reverse().toString();
            result.add(line);
            lineBuffer.reset();
        }

        raf.seek(filePointer);
        return result;
    }

    private void tailFile() {
        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
            long filePointer = raf.length();

            while (running) {
                long fileLength = logFile.length();
                if (fileLength < filePointer) {
                    filePointer = fileLength; // log rotation
                } else if (fileLength > filePointer) {
                    raf.seek(filePointer);
                    String line;
                    while ((line = raf.readLine()) != null) {
                        String finalLine = line;
                        SwingUtilities.invokeLater(() -> {
                            logArea.append(finalLine + "\n");
                            if (autoScroll) {
                                logArea.setCaretPosition(logArea.getDocument().getLength());
                            }
                        });
                    }
                    filePointer = raf.getFilePointer();
                    loadPosition = Math.min(loadPosition, filePointer); // keep consistent
                }
                Thread.sleep(500);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void clearWindow() {
        //TODO
    }

    public String getTextAreaContent() {
        //TODO
        return null;
    }
}
