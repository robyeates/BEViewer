package co.rob;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Observable;
import java.util.Observer;

/**
 * The dialog window for showing progress on a started bulk_extractor scan process.
 * Multiple windows and scans may be active.  This window is not modal.
 */

public class WScanProgress extends JDialog {
  private static final long serialVersionUID = 1;

  private final FileComponent imageFileLabel = new FileComponent();
  private final FileComponent featureDirectoryLabel = new FileComponent();

  private final JProgressBar progressBar = new JProgressBar();
  private final JLabel progressL = new JLabel();
  private final JTextArea statusArea = new JTextArea();
  private final JButton cancelB = new JButton("Cancel");
  private final String[] cmd;
  private static final String[] envp = new String[0];

  private Process process;
  private Thread scannerThread;

  // state
  File imageFile = null;
  File featuresDirectory = null;

  static {
//    envp[0] = "LD_LIBRARY_PATH=/usr/local/lib";
  }

  /**
   * Returns the thread controlling the scan, allowing monitoring for completion.
   */
  public Thread getScannerThread() {
    return scannerThread;
  }

  public WScanProgress(Window parentWindow, String[] command) {
    cmd = command;
    buildInterface();
    setClosure();
    pack();
    setLocationRelativeTo(parentWindow);
    getRootPane().setDefaultButton(cancelB);
    wireActions();
    setVisible(true);
    startProcess();
  }

  // closure
  private void setClosure() {
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);	// close via handler
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        doClose();
      }
    });
  }
 
  private void startProcess() {
    scannerThread = new ScannerThread();
    scannerThread.start();
  }

  private class ScannerThread extends Thread {
    private BufferedReader readFromProcess;
    private BufferedReader errorFromProcess;
    public void run() {

      // set state
      // image file is last cmd
      imageFile = new File(cmd[cmd.length - 1]);
      // feature directory is cmd after "-o"
      for (int i=0; i<cmd.length; i++) {
        if (cmd[i].equals("-o")) {
          featuresDirectory = new File(cmd[i + 1]);
          break;
        }
      }

      // set file labels
      imageFileLabel.setFile(imageFile);
      featureDirectoryLabel.setFile(featuresDirectory);

      // log the scan command
      for (int i=0; i<cmd.length; i++) {
        WLog.log("WScanProgress.cmd[" + i + "]: '" + cmd[i] + "'");
      }

      // start the bulk_extractor scan or fail
      process = null;
      try {
        // start bulk_extractor
        process = Runtime.getRuntime().exec(cmd, envp);

      } catch (IOException e) {
        // alert and abort
        statusArea.append("Error: The bulk_extractor process failed to start.\n");
        WError.showError("bulk_extractor Scanner failed to start.",
                         "bulk_extractor failure", e);
        return;
      }

      // start the thread for reading stdout
      readFromProcess = new BufferedReader(new InputStreamReader(process.getInputStream()));
      ThreadReaderModel stdoutThread = new ThreadReaderModel(readFromProcess);
      stdoutThread.addReaderModelChangedListener(new Observer() {
        public void update(Observable o, Object arg) {
          String input = (String)arg;
          SwingUtilities.invokeLater(new RunnableStdout(input));
        }
      });
     
      // start the thread for reading stderr
      errorFromProcess = new BufferedReader(new InputStreamReader(process.getErrorStream()));
      ThreadReaderModel stderrThread = new ThreadReaderModel(errorFromProcess);
      stderrThread.addReaderModelChangedListener(new Observer() {
        public void update(Observable o, Object arg) {
          String input = (String)arg;
          SwingUtilities.invokeLater(new RunnableStderr(input));
        }
      });

      // wait for the bulk_extractor scan process to finish
      try {
        process.waitFor();
      } catch (InterruptedException ie) {
        WLog.log("WScanProgress ScannerThread interrupted");
      }

      // wait for the thread readers to finish
      try {
        stdoutThread.join();
      } catch (InterruptedException ie1) {
        throw new RuntimeException("unexpected event");
      }
      try {
        stderrThread.join();
      } catch (InterruptedException ie2) {
        throw new RuntimeException("unexpected event");
      }

      // set the final "done" state
      SwingUtilities.invokeLater(new RunnableDone());
    }
  }

  // This class integrates stdout into the UI
  private class RunnableStdout implements Runnable {
    private final String input;
    public RunnableStdout(String input) {
      this.input = input;
    }

    public void run() {
      // set progress % in progress bar
      // check input for progress identifier: "(.*%)"
      int leftParenIndex = input.indexOf("(");
      int rightParenIndex = input.indexOf("%)");
      if (leftParenIndex > 0 && rightParenIndex > leftParenIndex) {
        // this qualifies as a progress line

        // set input in progress label
        progressL.setText(input);

        // set % in progress bar
        String progress = input.substring(leftParenIndex + 1, rightParenIndex);
        try {
          float progressFloat = Float.parseFloat(progress);
          progressBar.setValue((int)progressFloat);
          progressBar.setString(Float.toString(progressFloat) + "%");
        } catch (NumberFormatException e) {
          WLog.log("WScanProgress.run: unexpected progress value '" + progress + "' in stdout: " + input);
        }

      } else if (input.startsWith("Time elapsed waiting")) {
        progressL.setText(input);
      } else if (input.equals("")) {
        // no action, note that this is emitted during "Time elapsed waiting"
      } else {
        WLog.log("WScanProgress stdout from bulk_extractor: '" + input + "'");
        statusArea.append(input + "\n");
      }
    }
  }

  // This class integrates stderr into the UI
  private class RunnableStderr implements Runnable {
    private final String input;
    public RunnableStderr(String input) {
      this.input = input;
    }

    public void run() {
      WLog.log("bulk_extractor scan error: '" + input + "'");
      statusArea.append(input + "\n");
    }
  }

  // This class integrates "done" information into the UI
  private class RunnableDone implements Runnable {
    public void run() {

      // change "cancel" button to say "close"
      cancelB.setText("Close");

      // respond to termination based on the process' exit value
      int exitValue = process.exitValue();
      if (exitValue == 0) {
        // successful run
        progressL.setText("bulk_extractor scan completed.  See Status for details.");
        progressBar.setValue(100);
        progressBar.setString("Done");
        statusArea.append("Done.\n");

        // alert completion
        WError.showMessage("bulk_extractor has completed.\nReport " + featuresDirectory.getName()
                  + " has been opened and is ready for viewing.", "Report is Ready");

        // As a user convenience, Open the report that has been generated
        // by this run
        BEViewer.reportsModel.addReport(featuresDirectory, imageFile);

//        doClose();

      } else {
        // failed run
        WLog.log("bulk_extractor error exit value: " + exitValue);
        statusArea.append("Error: " + exitValue + "\n");
        progressBar.setString("Error");
        WError.showError("bulk_extractor Scanner terminated.",
                         "bulk_extractor failure", null);
      }
    }
  }

  private void buildInterface() {
    // set the title to include the image filename
    setTitle("bulk_extractor Scan");

    // use GridBagLayout with GridBagConstraints
    GridBagConstraints c;
    Container pane = getContentPane();
    pane.setLayout(new GridBagLayout());

    // (0,0) File container
    c = new GridBagConstraints();
    c.insets = new Insets(15, 5, 0, 5);
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    pane.add(getFileContainer(), c);

    // (0,1) Command field container
    c = new GridBagConstraints();
    c.insets = new Insets(15, 5, 0, 5);
    c.gridx = 0;
    c.gridy = 1;
    c.anchor = GridBagConstraints.LINE_START;
    pane.add(getCommandContainer(), c);

    // (0,2) Progress container
    c = new GridBagConstraints();
    c.insets = new Insets(15, 5, 0, 5);
    c.gridx = 0;
    c.gridy = 2;
    c.anchor = GridBagConstraints.LINE_START;
    pane.add(getProgressContainer(), c);

    // (0,3) status area container
    c = new GridBagConstraints();
    c.insets = new Insets(15, 5, 0, 5);
    c.gridx = 0;
    c.gridy = 3;
    c.weightx= 1;
    c.weighty= 1;
    c.fill = GridBagConstraints.BOTH;
    pane.add(getStatusContainer(), c);

    // (0,4) Cancel
    c = new GridBagConstraints();
    c.insets = new Insets(15, 5, 15, 5);
    c.gridx = 0;
    c.gridy = 4;

    // add the cancel button
    pane.add(cancelB, c);
  }

  private void wireActions() {
    // service the cancel button
    cancelB.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        doClose();
      }
    });
  }

  private void doClose() {
    // cancel
    if (process != null) {
      process.destroy();
    }
    dispose();
  }

  // File container
  private Container getFileContainer() {
    GridBagConstraints c;
    Container container = new Container();
    container.setLayout(new GridBagLayout());

    // (0,0) "Image File"
    c = new GridBagConstraints();
    c.insets = new Insets(0, 0, 0, 10);
    c.gridx = 0;
    c.gridy = 0;
    c.anchor = GridBagConstraints.LINE_START;
    container.add(new JLabel("Image File"), c);

    // (1,0) <image file>
    c = new GridBagConstraints();
    c.insets = new Insets(0, 0, 0, 0);
    c.gridx = 1;
    c.gridy = 0;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    container.add(imageFileLabel, c);

    // (0,1) "Feature Directory"
    c = new GridBagConstraints();
    c.insets = new Insets(0, 0, 0, 10);
    c.gridx = 0;
    c.gridy = 1;
    c.anchor = GridBagConstraints.LINE_START;
    container.add(new JLabel("Feature Directory"), c);

    // (1,1) <feature directory>
    c = new GridBagConstraints();
    c.insets = new Insets(0, 0, 0, 0);
    c.gridx = 1;
    c.gridy = 1;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    container.add(featureDirectoryLabel, c);

    return container;
  }

  private String getCommandString(String[] command) {
    StringBuffer buffer = new StringBuffer();
    for (int i=0; i<command.length; i++) {
      // append space separator between command parts
      if (i > 0) {
        buffer.append(" ");
      }

      // append command part
      if (command[i].indexOf(" ") >= 0) {
        // append with quotes
        buffer.append("\"" + command[i] + "\"");
      } else {
        // append without quotes
        buffer.append(command[i]);
      }
    }
    buffer.append("\n");
    return buffer.toString();
  }

  // Command container
  private Container getCommandContainer() {
    GridBagConstraints c;
    Container container = new Container();
    container.setLayout(new GridBagLayout());

    // (0,0) "command"
    c = new GridBagConstraints();
    c.insets = new Insets(0, 0, 0, 0);
    c.gridx = 0;
    c.gridy = 0;
    c.anchor = GridBagConstraints.LINE_START;
    container.add(new JLabel("Command"), c);

    // (0,1) command text field
    JTextField commandField = new JTextField(getCommandString(cmd));
    commandField.setEditable(false);
    commandField.setCaretPosition(0);
    commandField.setMinimumSize(new Dimension(500, commandField.getPreferredSize().height));
    commandField.setPreferredSize(new Dimension(500, commandField.getPreferredSize().height));
 
    c = new GridBagConstraints();
    c.insets = new Insets(0, 0, 0, 0);
    c.gridx = 0;
    c.gridy = 1;
    c.weightx = 1;
    c.weighty = 1;
    c.anchor = GridBagConstraints.LINE_START;

    // add the command field
    container.add(commandField, c);

    return container;
  }

  // Progress container
  private Container getProgressContainer() {
    GridBagConstraints c;
    Container container = new Container();
    container.setLayout(new GridBagLayout());

    // (0,0) "progress"
    c = new GridBagConstraints();
    c.insets = new Insets(0, 0, 0, 10);
    c.gridx = 0;
    c.gridy = 0;
    c.anchor = GridBagConstraints.LINE_START;
    container.add(new JLabel("Progress"), c);

    // (1,0) progress bar
    c = new GridBagConstraints();
    c.insets = new Insets(0, 0, 0, 0);
    c.gridx = 1;
    c.gridy = 0;
    c.anchor = GridBagConstraints.LINE_START;
    progressBar.setPreferredSize(new Dimension(60, progressBar.getPreferredSize().height));
    progressBar.setMinimumSize(progressBar.getPreferredSize());
    progressBar.setStringPainted(true);
    progressBar.setMinimum(0);
    progressBar.setMaximum(100);
    progressBar.setValue(0);
    progressBar.setString("0%");
    container.add(progressBar, c);

    // (0,1) reported progress text
    c = new GridBagConstraints();
    c.insets = new Insets(0, 0, 0, 0);
    c.gridx = 0;
    c.gridy = 1;
    c.weightx = 1;
    c.weighty = 1;
    c.gridwidth = 2;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.LINE_START;
    container.add(progressL, c);

    return container;
  }

  // Status container
  private Container getStatusContainer() {
    GridBagConstraints c;
    Container container = new Container();
    container.setLayout(new GridBagLayout());

    // (0,0) "Status"
    c = new GridBagConstraints();
    c.insets = new Insets(0, 0, 0, 0);
    c.gridx = 0;
    c.gridy = 0;
    c.anchor = GridBagConstraints.LINE_START;
    container.add(new JLabel("Status"), c);

    // (0,1) status scrollpane for containing output from bulk_extractor
    statusArea.setEditable(false);
    JScrollPane statusScrollPane = new JScrollPane(statusArea,
                       ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                       ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    statusScrollPane.setPreferredSize(new Dimension(500, 200));
    c = new GridBagConstraints();
    c.insets = new Insets(0, 0, 0, 0);
    c.gridx = 0;
    c.gridy = 1;
    c.weightx = 1;
    c.weighty = 1;
    c.fill = GridBagConstraints.BOTH;

    // add the status scrollpane
    container.add(statusScrollPane, c);

    return container;
  }
}

