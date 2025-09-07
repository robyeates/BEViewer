package co.rob.state;


import co.rob.pojo.FeatureLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.List;

import static co.rob.io.features.FeatureLineUtils.isBlank;
import static co.rob.io.features.FeatureLineUtils.isFromReport;

/**
 * Provides bookmark services, specifically, management and selection of
 * a bookmark list, by wrapping DefaultListModel and DefaultComboBoxModel
 * services.
 * Note that equality is redefined to use equals rather than sameness.
 */
@Singleton
public class BookmarksModel {

    private static final Logger logger = LoggerFactory.getLogger(BookmarksModel.class);

    @Inject
    public BookmarksModel(){

    }

    private final BookmarksComboBoxModel bookmarksComboBoxModel = new BookmarksComboBoxModel();

    public void addElement(FeatureLine featureLine) {
        if (isBlank(featureLine)) {
            // this can happen on keystroke bookmark request
            logger.info("BookmarksModel.addElement blank element not added");
        } else if (contains(featureLine)) {
            // this can happen on keystroke bookmark request
            logger.info("BookmarksModel.addElement already has element");
        } else {
            bookmarksComboBoxModel.addElement(featureLine);
        }
    }

    public FeatureLine get(int i) {
        return bookmarksComboBoxModel.getElementAt(i);
    }

    public int size() {
        return bookmarksComboBoxModel.getSize();
    }

    public boolean isEmpty() {
        return (bookmarksComboBoxModel.getSize() == 0);
    }

    public boolean contains(FeatureLine featureLine) {
        int size = bookmarksComboBoxModel.getSize();
        // can't just use getIndexOf because we check sameness, not same object
        for (int i = 0; i < size; i++) {
            if (featureLine.equals(bookmarksComboBoxModel.getElementAt(i))) {
                return true;
            }
        }
        return false;
        // this fails because it checks sameness, not object equality
        //return (bookmarksComboBoxModel.getIndexOf((Object)featureLine) >= 0);
    }

    public void removeElement(FeatureLine featureLine) {
        int size = bookmarksComboBoxModel.getSize();
        for (int i = 0; i < size; i++) {
            if (featureLine.equals(bookmarksComboBoxModel.getElementAt(i))) {
                bookmarksComboBoxModel.removeElementAt(i);
                return;
            }
        }
        logger.warn("requested element failed to remove: {}", featureLine);
    }

    public void clear() {
        bookmarksComboBoxModel.removeAllElements();
    }

    public List<FeatureLine> getBookmarks() {
        int size = bookmarksComboBoxModel.getSize();
        
        List<FeatureLine> featureLines = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            featureLines.add(bookmarksComboBoxModel.getElementAt(i));
        }
        return featureLines;
    }

    public void fireViewChanged() {
        bookmarksComboBoxModel.fireViewChanged();
    }

    // selection
    public FeatureLine getSelectedItem() {
        return (FeatureLine) bookmarksComboBoxModel.getSelectedItem();
    }

    public void setSelectedItem(FeatureLine featureLine) {
        bookmarksComboBoxModel.setSelectedItem(featureLine);
    }

    // bookmark-specific remove capability
    public void removeAssociatedFeatureLines(ReportsModel.ReportTreeNode reportTreeNode) {
        // get feature line list to use
        List<FeatureLine> featureLines = getBookmarks();

        // now remove the associated features from each wrapped model
        featureLines.removeIf(featureLine -> isFromReport(featureLine, reportTreeNode));
    }

    public void addListDataListener(ListDataListener listDataListener) {
        bookmarksComboBoxModel.addListDataListener(listDataListener);
    }

    public JList<FeatureLine> createJList() {
        return new JList<>(bookmarksComboBoxModel);
    }

    public static class BookmarksComboBoxModel extends DefaultComboBoxModel<FeatureLine> {
        /*
         * Offer facility to manage when the feature's view changes.
         */
        public void fireViewChanged() {
            fireContentsChanged(this, 0, getSize());
        }
    }
}
