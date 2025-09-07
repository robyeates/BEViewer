package co.rob;


import co.rob.io.*;
import co.rob.io.print.FeatureRangePrinter;
import co.rob.state.*;
import co.rob.state.qualifier.Features;
import co.rob.state.qualifier.ReferencedFeatures;
import co.rob.ui.highlights.FeatureHighlightIndexService;
import co.rob.ui.highlights.HighlightModule;
import co.rob.ui.pane.FeaturesPane;
import co.rob.ui.pane.NavigationPane;
import co.rob.ui.pane.ReportsPane;
import co.rob.ui.pane.UIPaneModule;
import co.rob.ui.selection.*;
import dagger.Component;
import dagger.Provides;

import javax.inject.Singleton;

@Singleton
@Component(modules = {
        StatefulModule.class,
        FileModule.class,
        HighlightModule.class,
        UIPaneModule.class
})
public interface AppComponent {

    ScanSettingsListModel scanSettingsListModel();

    ReportsModel reportsModel();
    BookmarksModel bookmarksModel();
    ImageModel imageModel();

    ImageView imageView();

    @Features
    FeaturesModel featuresModel();
    @ReferencedFeatures
    FeaturesModel referencedFeaturesModel();

    FeatureHighlightIndexService featureHighlightIndexService();
    UserHighlightModel userHighlightModel();

    ReportSelectionManager reportSelectionManager();
    RangeSelectionManager rangeSelectionManager();
    FeatureLineSelectionManager featureLineSelectionManager();

    HighlightColorSelectionModel highlightColorSelectionModel();
    FeatureNavigationComboBoxModel featureNavigationComboBoxModel();

    ImageFileReader imageFileReader();
    ReportFileReader reportFileReader();

    WorkSettingsWriter workSettingsWriter();
    WorkSettingsReader workSettingsReader();

    BookmarksWriter bookmarksWriter();
    BookmarksReader bookmarksReader();
    FeatureRangePrinter featureRangePrinter();

    ReportsPane reportsPane();
    FeaturesPane featuresPane();
    NavigationPane navigationPane();
}
