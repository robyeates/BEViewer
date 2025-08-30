package co.rob;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Manage general option values.
 */

public class WScanBoxedGeneral {

  public final Component component;

  // general options
  public boolean useBannerFile;
  public String bannerFile;
  public boolean useAlertlistFile;
  public String alertlistFile;
  public boolean useStoplistFile;
  public String stoplistFile;
  public boolean useFindRegexTextFile;
  public String findRegexTextFile;
  public boolean useFindRegexText;
  public String findRegexText;
  public boolean useRandomSampling;
  public String randomSampling;

  private final JCheckBox useBannerFileCB = new JCheckBox("Use Banner File");
  private final JTextField bannerFileTF = new JTextField();
  private final JButton bannerFileChooserB = new FileChooserButton(WScan.getWScanWindow(), "Select Banner File", FileChooserButton.READ_FILE, bannerFileTF);
  private final JCheckBox useAlertlistFileCB = new JCheckBox("Use Alert List File");
  private final JTextField alertlistFileTF = new JTextField();
  private final JButton alertlistFileChooserB = new FileChooserButton(WScan.getWScanWindow(), "Select Alert List File", FileChooserButton.READ_FILE, alertlistFileTF);
  private final JCheckBox useStoplistFileCB = new JCheckBox("Use Stop List File");
  private final JTextField stoplistFileTF = new JTextField();
  private final JButton stoplistFileChooserB = new FileChooserButton(WScan.getWScanWindow(), "Select Stop List File", FileChooserButton.READ_FILE, stoplistFileTF);
  private final JCheckBox useFindRegexTextFileCB= new JCheckBox("Use Find Regex Text File");
  private final JTextField findRegexTextFileTF= new JTextField();
  private final JButton findRegexTextFileChooserB= new FileChooserButton(WScan.getWScanWindow(), "Select Find Regex Text File", FileChooserButton.READ_FILE, findRegexTextFileTF);
  private final JCheckBox useFindRegexTextCB = new JCheckBox("Use Find Regex Text");
  private final JTextField findRegexTextTF = new JTextField();
  private final JCheckBox useRandomSamplingCB = new JCheckBox("Use Random Sampling");
  private final JTextField randomSamplingTF = new JTextField();

  public WScanBoxedGeneral() {
    component = buildContainer();
    wireActions();
  }

  private Component buildContainer() {
    // container using GridBagLayout with GridBagConstraints
    JPanel container = new JPanel();
    container.setBorder(BorderFactory.createTitledBorder("General Options"));
    container.setLayout(new GridBagLayout());
    int y = 0;
    WScan.addOptionalFileLine(container, y++, useBannerFileCB, bannerFileTF, bannerFileChooserB);
    WScan.addOptionalFileLine(container, y++, useAlertlistFileCB, alertlistFileTF, alertlistFileChooserB);
    WScan.addOptionalFileLine(container, y++, useStoplistFileCB, stoplistFileTF, stoplistFileChooserB);
    WScan.addOptionalFileLine(container, y++, useFindRegexTextFileCB, findRegexTextFileTF,
                                        findRegexTextFileChooserB);
    WScan.addOptionalTextLine(container, y++, useFindRegexTextCB, findRegexTextTF, WScan.EXTRA_WIDE_FIELD_WIDTH);
    WScan.addOptionalTextLine(container, y++, useRandomSamplingCB, randomSamplingTF, WScan.WIDE_FIELD_WIDTH);

    // tool tip text
    useRandomSamplingCB.setToolTipText("Random Sampling in form frac[:passes]");

    return container;
  }

  public void setDefaultValues() {
    // general options
    useBannerFile = false;
    bannerFile = "";
    useAlertlistFile = false;
    alertlistFile = "";
    useStoplistFile = false;
    stoplistFile = "";
    useFindRegexTextFile = false;
    findRegexTextFile = "";
    useFindRegexText = false;
    findRegexText = "";
    useRandomSampling = false;
    randomSampling = "";
  }

  public void setUIValues() {
    // general options
    useBannerFileCB.setSelected(useBannerFile);
    bannerFileTF.setEnabled(useBannerFile);
    bannerFileTF.setText(bannerFile);
    bannerFileChooserB.setEnabled(useBannerFile);

    useAlertlistFileCB.setSelected(useAlertlistFile);
    alertlistFileTF.setEnabled(useAlertlistFile);
    alertlistFileTF.setText(alertlistFile);
    alertlistFileChooserB.setEnabled(useAlertlistFile);

    useStoplistFileCB.setSelected(useStoplistFile);
    stoplistFileTF.setEnabled(useStoplistFile);
    stoplistFileTF.setText(stoplistFile);
    stoplistFileChooserB.setEnabled(useStoplistFile);

    useFindRegexTextFileCB.setSelected(useFindRegexTextFile);
    findRegexTextFileTF.setEnabled(useFindRegexTextFile);
    findRegexTextFileTF.setText(findRegexTextFile);
    findRegexTextFileChooserB.setEnabled(useFindRegexTextFile);

    useFindRegexTextCB.setSelected(useFindRegexText);
    findRegexTextTF.setEnabled(useFindRegexText);
    findRegexTextTF.setText(findRegexText);

    useRandomSamplingCB.setSelected(useRandomSampling);
    randomSamplingTF.setEnabled(useRandomSampling);
    randomSamplingTF.setText(randomSampling);
  }

  public void getUIValues() {
    // general options
    useBannerFile = useBannerFileCB.isSelected();
    bannerFile = bannerFileTF.getText();
    useAlertlistFile = useAlertlistFileCB.isSelected();
    alertlistFile = alertlistFileTF.getText();
    useStoplistFile = useStoplistFileCB.isSelected();
    stoplistFile = stoplistFileTF.getText();
    useFindRegexTextFile = useFindRegexTextFileCB.isSelected();
    findRegexTextFile = findRegexTextFileTF.getText();
    useFindRegexText = useFindRegexTextCB.isSelected();
    findRegexText = findRegexTextTF.getText();
    useRandomSampling = useRandomSamplingCB.isSelected();
    randomSampling = randomSamplingTF.getText();
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
    ActionListener getUIValuesActionListener = new GetUIValuesActionListener();

    useBannerFileCB.addActionListener(getUIValuesActionListener);
    useAlertlistFileCB.addActionListener(getUIValuesActionListener);
    useStoplistFileCB.addActionListener(getUIValuesActionListener);
    useFindRegexTextFileCB.addActionListener(getUIValuesActionListener);
    useFindRegexTextCB.addActionListener(getUIValuesActionListener);
    useRandomSamplingCB.addActionListener(getUIValuesActionListener);
  }
}

