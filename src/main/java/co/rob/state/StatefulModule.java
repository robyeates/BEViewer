package co.rob.state;

import co.rob.state.qualifier.Features;
import co.rob.state.qualifier.ReferencedFeatures;
import co.rob.ui.selection.*;
import dagger.Provides;
import dagger.Module;

import javax.inject.Singleton;

@Module
public class StatefulModule {

    @Provides
    @Singleton
    ScanSettingsListModel scanSettingsListModel(){
        return new ScanSettingsListModel();
    }

    @Provides
    @Singleton
    ReportsModel reportsModel() {
        return new ReportsModel();
    }

    @Provides
    @Singleton
    BookmarksModel bookmarksModel() {
        return new BookmarksModel();
    }

    @Provides
    @Singleton
    ImageModel imageModel(FeatureLineSelectionManager featureLineSelectionManager) {
        return new ImageModel(featureLineSelectionManager);
    }

    @Provides
    @Singleton
    ImageView imageView(ImageModel imageModel, UserHighlightModel userHighlightModel) {
        return new ImageView(imageModel, userHighlightModel);
    }

    @Provides
    @Singleton
    @Features
    FeaturesModel featuresModel(ReportSelectionManager reportSelectionManager) {
        return new FeaturesModel(reportSelectionManager, FeaturesModel.ModelType.FEATURES_OR_HISTOGRAM);
    }

    @Provides
    @Singleton
    @ReferencedFeatures
    FeaturesModel referencedFeaturesModel(ReportSelectionManager reportSelectionManager) {
        return new FeaturesModel(reportSelectionManager, FeaturesModel.ModelType.REFERENCED_FEATURES);
    }

    @Provides
    @Singleton
    ReportSelectionManager reportSelectionManager(){
        return new ReportSelectionManager();
    }

    @Provides
    @Singleton
    RangeSelectionManager rangeSelectionManager() {
        return new RangeSelectionManager();
    }

    @Provides
    @Singleton
    FeatureLineSelectionManager featureLineSelectionManager(){
        return new FeatureLineSelectionManager();
    }

    @Provides
    @Singleton
    HighlightColorSelectionModel highlightColorSelectionModel(){
        return new HighlightColorSelectionModel();
    }

    @Provides
    @Singleton
    FeatureNavigationComboBoxModel featureNavigationComboBoxModel(FeatureLineSelectionManager featureLineSelectionManager){
        return new FeatureNavigationComboBoxModel(featureLineSelectionManager);
    }

}
