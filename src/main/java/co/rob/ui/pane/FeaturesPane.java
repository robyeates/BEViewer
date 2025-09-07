package co.rob.ui.pane;

import co.rob.ui.components.FeaturesComponent;
import co.rob.state.FeaturesModel;
import co.rob.ui.highlights.TextComponent;
import co.rob.ui.selection.*;
import co.rob.ui.selection.ReportSelectionManager.ChangeType;
import co.rob.util.UTF8Tools;
import co.rob.ui.components.FileComponent;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static co.rob.util.BEConstants.*;

/**
 * The <code>FeaturesPane</code> class provides the feature view and the
 * interfaces for selecting a feature from a feature file.
 * When a feature is selected, the selected feature is set in the <code>ImageModel</code>.
 */
@Singleton
public class FeaturesPane extends Container {

    private static final int EDGE_PADDING = GUI_EDGE_PADDING;
    private static final int Y_PADDING = GUI_Y_PADDING;


    private final JLabel featuresFileL = new JLabel("Feature File");
    private final JCheckBox matchCaseCB = new JCheckBox("Match case");
    private final JTextField filterTF = new JTextField();

    private final FileComponent featuresFileLabel = new FileComponent();
    private final FileComponent referencedFeaturesFileLabel = new FileComponent();
    private final TextComponent referencedFeatureLabel = new TextComponent();

    private final Container featuresContainer;
    private final Container referencedFeaturesContainer;
    private final JSplitPane featuresSplitPane;

    private final FeaturesComponent featuresComponent;
    private final FeaturesComponent referencedFeaturesComponent;
    private final FeaturesModel featuresModel;
    private final FeaturesModel referencedFeaturesModel;
    private final ReportSelectionManager reportSelectionManager;
    private final HighlightColorSelectionModel highlightColorSelectionModel;
    private final RangeSelectionManager rangeSelectionManager;

    private int featuresSplitPaneDividerLocation = 300;//TODO hmmm

    // UI state
    private Color defaultFilterTFBackgroundColor = null;//TODO hmmm

    /**
     * Constructs the features pane and wires actions.
     */
    @Inject
    public FeaturesPane(FeaturesModel featuresModel, FeaturesModel referencedFeaturesModel, RangeSelectionManager rangeSelectionManager,
                        ReportSelectionManager reportSelectionManager, HighlightColorSelectionModel highlightColorSelectionModel) {
        this.featuresModel = featuresModel;
        this.referencedFeaturesModel = referencedFeaturesModel;
        this.rangeSelectionManager = rangeSelectionManager;
        this.reportSelectionManager = reportSelectionManager;
        this.highlightColorSelectionModel = highlightColorSelectionModel;

        featuresComponent = new FeaturesComponent(this.featuresModel, rangeSelectionManager);
        referencedFeaturesComponent = new FeaturesComponent(this.referencedFeaturesModel, rangeSelectionManager);

        featuresContainer = getFeaturesContainer();
        referencedFeaturesContainer = getReferencedFeaturesContainer();
        featuresSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, featuresContainer, referencedFeaturesContainer);

        // build the UI
        setComponents();
        // set Referenced Features pane visibility when the features file type model changes
        this.reportSelectionManager.addReportSelectionManagerChangedListener(evt -> {
            setReferencedFeatureViewVisibility();
            if (evt.getPropertyName().equals(ChangeType.SELECTION_CHANGED.name())) {
                // set feature fields if selection changed, not needed when user preference changes
                setFeatureFields();
            }
        });

        // keep case checkbox and filter text field in sync with the features model
        this.featuresModel.addFeaturesModelChangedListener(_ -> {
            // keep checkbox in sync
            matchCaseCB.setSelected(this.featuresModel.isFilterMatchCase());
            // keep feature filter text in sync
            // set filterTF text
            byte[] filterBytes = this.featuresModel.getFilterBytes();
            if (!UTF8Tools.bytesMatch(filterBytes, filterTF.getText().getBytes(UTF8Tools.UTF_8))) {
                filterTF.setText(new String(filterBytes));
            }
            // make filterTF background highlighted if it has text in it
            if (filterBytes.length > 0) {
                filterTF.setBackground(this.highlightColorSelectionModel.getSelectedColor());
            } else {
                //filterTF.setBackground(defaultFilterTFBackgroundColor);
            }
        });

        this.highlightColorSelectionModel.addPropertyChangeListener((evt, oldValue, newValue) -> {
            filterTF.putClientProperty("JComponent.outline", newValue);
            filterTF.setBackground(newValue);
        });

        // update filter text view when referenced features model changes
        this.referencedFeaturesModel.addFeaturesModelChangedListener(evt -> {
            // show referenced feature filter text used in the features model
            byte[] featureBytes = this.referencedFeaturesModel.getFilterBytes();
            if (featureBytes.length == 0) {
                // let the label component provide better text when "".
                referencedFeatureLabel.setComponentText(null);
            } else {
                if (UTF8Tools.escapedLooksLikeUTF16(featureBytes)) {
                    featureBytes = UTF8Tools.utf16To8Correct(featureBytes);
                }

                referencedFeatureLabel.setComponentText(new String(featureBytes));
            }
        });

    }

    private void setFeatureFields() {
        // field values depends on report type being histogram or features
        if (reportSelectionManager.getReportSelectionType() == ReportSelectionManager.ReportSelectionType.HISTOGRAM) {
            // top shows histogram featues and bottom shows referenced features
            // top says "Histogram File"
            featuresFileL.setText("Histogram File");
            // top shows histogram filename
            featuresFileLabel.setFile(reportSelectionManager.getHistogramFile());

            // bottom shows referenced features filename
            referencedFeaturesFileLabel.setFile(reportSelectionManager.getReferencedFeaturesFile());

        } else {
            //WLog.log("FeaturesPane.setFeaturesFields Features");
            // the top shows the featues and the bottom is blank

            // top says "Feature File"
            featuresFileL.setText("Feature File");

            // top shows features filename
            featuresFileLabel.setFile(reportSelectionManager.getFeaturesFile());

            // bottom shows blank features filename
            referencedFeaturesFileLabel.setFile(null);

            // bottom shows blank referenced feature filter text because it was not used in the features model
            //      referencedFeatureLabel.setComponentText(null);
        }
    }

    // collapse referenced feature view depending on feature file type and user preference
    private void setReferencedFeatureViewVisibility() {
        //WLog.log("FeaturesPane.setReferencedFeatureViewVisibility");
        // determine whether the referenced feature view is currently hidden
        boolean isHidden = (featuresSplitPane.getBottomComponent() == null);
        boolean wantsHidden = reportSelectionManager.isRequestHideReferencedFeatureView();
        boolean canHide = (reportSelectionManager.getReportSelectionType() != ReportSelectionManager.ReportSelectionType.HISTOGRAM);

        if (isHidden && (!wantsHidden || !canHide)) {
            // always show
            //WLog.log("FeaturesPane.setReferencedFeatureViewVisibility always show");
            featuresSplitPane.setBottomComponent(referencedFeaturesContainer);
            featuresSplitPane.setDividerLocation(featuresSplitPaneDividerLocation);
            featuresSplitPane.setDividerSize(SPLIT_PANE_DIVIDER_SIZE);

            // hide if possible
        } else if (!isHidden && (wantsHidden && canHide)) {
            //WLog.log("FeaturesPane.setReferencedFeatureViewVisibility hide if possible");
            featuresSplitPane.setBottomComponent(null);
            featuresSplitPaneDividerLocation = featuresSplitPane.getDividerLocation();
            featuresSplitPane.setDividerSize(0);
        } else {
            // leave alone
            //WLog.log("FeaturesPane.setReferencedFeatureViewVisibility hide if possible");
        }
    }

    /**
     * Constructs the interfaces for providing a feature selection list.
     */
    private void setComponents() {

        // use GridBagLayout with GridBagConstraints
        GridBagConstraints c;
        setLayout(new GridBagLayout());
        int y = 0;

        // ************************************************************
        // (0,0) filter controls
        // ************************************************************
        c = new GridBagConstraints();
        c.insets = new Insets(EDGE_PADDING, EDGE_PADDING, 0, EDGE_PADDING);
        c.gridx = 0;
        c.gridy = y++;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        // add the filter controls container
        add(getFilterControls(), c);

        // ************************************************************
        // (0,1) vertical JSplitPane containing features and, if HISTOGRAM_FILE, referencedFeatures.
        // ************************************************************
        c = new GridBagConstraints();
        c.insets = new Insets(Y_PADDING, 0, 0, 0);
        c.gridx = 0;
        c.gridy = y++;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;

        // create the filled split pane
        featuresSplitPane.setBorder(null);    // per recommendation from SplitPaneDemo2
        featuresSplitPane.setResizeWeight(0.5);
        featuresSplitPane.setDividerSize(SPLIT_PANE_DIVIDER_SIZE);

        // add the filled split pane
        add(featuresSplitPane, c);
    }

    // ************************************************************
    // features container
    // ************************************************************
    private Container getFeaturesContainer() {

        Container container = new Container();
        container.setLayout(new GridBagLayout());
        GridBagConstraints c;

        // (0,0) feature stats line
        c = new GridBagConstraints();
        c.insets = new Insets(0, EDGE_PADDING, 0, EDGE_PADDING);
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        container.add(getFeatureStats(), c);

        // (1,0) features component inside JScrollPane
        c = new GridBagConstraints();
        c.insets = new Insets(0, EDGE_PADDING, EDGE_PADDING, EDGE_PADDING);
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;

        // create the scrollpane with the features component inside it
        JScrollPane featuresComponentScrollPane = new JScrollPane(featuresComponent, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        featuresComponentScrollPane.setPreferredSize(new Dimension(430, 300));

        // NOTE: use SIMPLE_SCROLL_MODE in JViewport else clip gets overoptimized
        // and fails to render background color changes properly.
        featuresComponentScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        // add the scrollpane containing the features component
        container.add(featuresComponentScrollPane, c);

        return container;
    }

    private Container getFeatureStats() {
        Container container = new Container();
        container.setLayout(new GridBagLayout());
        GridBagConstraints c;

        // (0,0) "Feature File"
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 10);
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        container.add(featuresFileL, c);

        // (1,0) <feature file>
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        // add the feature file label
        container.add(featuresFileLabel, c);

        return container;
    }

    // ************************************************************
    // referenced features container
    // ************************************************************
    private Container getReferencedFeaturesContainer() {

        Container container = new Container();
        container.setLayout(new GridBagLayout());
        GridBagConstraints c;

        // (0,0) referenced feature stats: "Referenced Feature File:   <filename>"
        //                                 "Referenced Feature:        <feature text>"
        c = new GridBagConstraints();
        c.insets = new Insets(EDGE_PADDING, EDGE_PADDING, 0, EDGE_PADDING);
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        container.add(getReferencedFeatureStats(), c);

        // (1,0) referenced features component inside JScrollPane
        c = new GridBagConstraints();
        c.insets = new Insets(0, EDGE_PADDING, EDGE_PADDING, EDGE_PADDING);
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;

        // create the scrollpane with the referenced features component inside it
        JScrollPane referencedFeaturesComponentScrollPane = new JScrollPane(referencedFeaturesComponent, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        referencedFeaturesComponentScrollPane.setPreferredSize(new Dimension(430, 300));

        // NOTE: use SIMPLE_SCROLL_MODE in JViewport else clip gets overoptimized
        // and fails to render background color changes properly.
        referencedFeaturesComponentScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        // add the scrollpane containing the referenced features component
        container.add(referencedFeaturesComponentScrollPane, c);

        return container;
    }

    private Container getReferencedFeatureStats() {
        Container container = new Container();
        container.setLayout(new GridBagLayout());
        GridBagConstraints c;

        // (0,0) "Referenced Feature File"
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 10);
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        container.add(new JLabel("Referenced Feature File"), c);

        // (1,0) <referenced feature file>
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        // add the feature file label
        container.add(referencedFeaturesFileLabel, c);

        // (0,1) Feature text
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 10);
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        container.add(new JLabel("Referenced Feature"), c);

        // (1,1) <feature text>
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        container.add(referencedFeatureLabel, c);

        return container;
    }

    // ************************************************************
    // filter controls container
    // ************************************************************
    private Container getFilterControls() {
        Container container = new Container();

        // use GridBagLayout with GridBagConstraints
        GridBagConstraints c;
        container.setLayout(new GridBagLayout());

        // ****************************************
        // title
        // ****************************************
        // (0,0) title: "Feature Filter"
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 10);
        c.gridx = 0;
        c.gridy = 0;
        container.add(new JLabel("Feature Filter"), c);

        // ****************************************
        // Match Case selector
        // ****************************************
        // (1,0) Match Case selector
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;

        // set initial value
        matchCaseCB.setFocusable(false);
        matchCaseCB.setSelected(featuresModel.isFilterMatchCase());
        matchCaseCB.setRequestFocusEnabled(false);
        matchCaseCB.setToolTipText("Match case in filter text");

        // make it the same hight as text instead of a bit too tall
        Dimension caseCBDimension = new Dimension(matchCaseCB.getPreferredSize().width, new JLabel(" ").getPreferredSize().height);
        matchCaseCB.setMinimumSize(caseCBDimension);
        matchCaseCB.setPreferredSize(caseCBDimension);
        matchCaseCB.setMaximumSize(caseCBDimension);

        // match case action listener
        matchCaseCB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // toggle the case setting in the models
                featuresModel.setFilterMatchCase(matchCaseCB.isSelected());

                // as a convenience, change focus to the filterTF
                filterTF.requestFocusInWindow();
            }
        });

        // add the match case button
        container.add(matchCaseCB, c);

        // ****************************************
        // filter text
        // ****************************************
        // (0,1) JTextField filter input
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;

        // create the JTextField filterTF for containing the filter text
        defaultFilterTFBackgroundColor = filterTF.getBackground();
        filterTF.setToolTipText("Type text to filter Features");

        // whenever the filter text changes then change the filter in the features model
        filterTF.getDocument().addDocumentListener(new DocumentListener() {

            private void setFeatureFilter() {
                // set the filter in the features model
                featuresModel.setFilterBytes(filterTF.getText().getBytes(UTF8Tools.UTF_8));
            }

            // JTextField responds to Document change
            public void insertUpdate(DocumentEvent e) {
                setFeatureFilter();
            }

            public void removeUpdate(DocumentEvent e) {
                setFeatureFilter();
            }

            public void changedUpdate(DocumentEvent e) {
                setFeatureFilter();
            }
        });

        // add the filter text textfield
        container.add(filterTF, c);

        return container;
    }
}

