package co.rob;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Manage the debugging values.
 */
public class WScanBoxedDebugging {

  // defaults
  private static final int DEFAULT_START_ON_PAGE_NUMBER = 0;
  private static final int DEFAULT_START_AT_OFFSET = 0;
  private static final int DEFAULT_DEBUG_NUMBER = 1;

  // Debugging
  public boolean useStartOnPageNumber;
  public int startOnPageNumber;
  public boolean useDebugNumber;
  public int debugNumber;
  public boolean useEraseOutputDirectory;

  public final Component component;
  private final JCheckBox useStartOnPageNumberCB = new JCheckBox("Start on Page Number");
  private final JTextField startOnPageNumberTF = new JTextField();
  private final JCheckBox useDebugNumberCB = new JCheckBox("Use Debug Mode Number");
  private final JTextField debugNumberTF = new JTextField();
  private final JCheckBox useEraseOutputDirectoryCB = new JCheckBox("Erase Output Directory");

  public WScanBoxedDebugging() {
    component = buildContainer();
    wireActions();
  }

  private Component buildContainer() {
    // container using GridBagLayout with GridBagConstraints
    JPanel container = new JPanel();
    container.setBorder(BorderFactory.createTitledBorder("Debugging Options"));
    container.setLayout(new GridBagLayout());
    int y = 0;
    WScan.addOptionalTextLine(container, y++, useStartOnPageNumberCB, startOnPageNumberTF, WScan.WIDE_FIELD_WIDTH);
    WScan.addOptionalTextLine(container, y++, useDebugNumberCB, debugNumberTF, WScan.NARROW_FIELD_WIDTH);
    WScan.addOptionLine(container, y++, useEraseOutputDirectoryCB);

    return container;
  }

  public void setDefaultValues() {
    // Debugging
    useStartOnPageNumber = false;
    startOnPageNumber = DEFAULT_START_ON_PAGE_NUMBER;
    useDebugNumber = false;
    debugNumber = DEFAULT_DEBUG_NUMBER;
    useEraseOutputDirectory = false;
  }

  public void setUIValues() {
    // Debugging
    useStartOnPageNumberCB.setSelected(useStartOnPageNumber);
    startOnPageNumberTF.setEnabled(useStartOnPageNumber);
    startOnPageNumberTF.setText(Integer.toString(startOnPageNumber));
    useDebugNumberCB.setSelected(useDebugNumber);
    debugNumberTF.setEnabled(useDebugNumber);
    debugNumberTF.setText(Integer.toString(debugNumber));
    useEraseOutputDirectoryCB.setSelected(useEraseOutputDirectory);
  }

  public void getUIValues() {
    // Debugging
    useStartOnPageNumber = useStartOnPageNumberCB.isSelected();
    startOnPageNumber = WScan.getInt(startOnPageNumberTF, "start on page number", DEFAULT_START_ON_PAGE_NUMBER);
    useDebugNumber = useDebugNumberCB.isSelected();
    debugNumber = WScan.getInt(debugNumberTF, "debug mode number", DEFAULT_DEBUG_NUMBER);
    useEraseOutputDirectory = useEraseOutputDirectoryCB.isSelected();
  }

  public boolean validateValues() {
    return true;
  }

  // the sole purpose of this listener is to keep UI widget visibility up to date
  private class GetUIValuesActionListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      getUIValues();
      setUIValues();
    }
  }

  private void wireActions() {
    // Debugging
    GetUIValuesActionListener getUIValuesActionListener = new GetUIValuesActionListener();
    useStartOnPageNumberCB.addActionListener(getUIValuesActionListener);
    useDebugNumberCB.addActionListener(getUIValuesActionListener);
    useEraseOutputDirectoryCB.addActionListener(getUIValuesActionListener);
  }
}

