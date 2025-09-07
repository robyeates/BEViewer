package co.rob.ui.pane;

import co.rob.state.FeaturesModel;
import co.rob.state.ImageModel;
import co.rob.state.ImageView;
import co.rob.state.ReportsModel;
import co.rob.state.qualifier.Features;
import co.rob.state.qualifier.ReferencedFeatures;
import co.rob.ui.selection.FeatureLineSelectionManager;
import co.rob.ui.selection.HighlightColorSelectionModel;
import co.rob.ui.selection.RangeSelectionManager;
import co.rob.ui.selection.ReportSelectionManager;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class UIPaneModule {

    @Provides
    @Singleton
    ReportsPane reportsPane(ReportsModel reportsModel, ReportSelectionManager reportSelectionManager) {
        return new ReportsPane(reportsModel, reportSelectionManager);
    }

    @Provides
    @Singleton
    FeaturesPane featuresPane(@Features FeaturesModel featuresModel, @ReferencedFeatures FeaturesModel referencedFeaturesModel, RangeSelectionManager rangeSelectionManager,
                              ReportSelectionManager reportSelectionManager, HighlightColorSelectionModel highlightColorSelectionModel) {

        return new FeaturesPane(featuresModel, referencedFeaturesModel, rangeSelectionManager, reportSelectionManager, highlightColorSelectionModel);
    }

    @Provides
    @Singleton
    NavigationPane navigationPane(ImageModel imageModel, ImageView imageView, RangeSelectionManager rangeSelectionManager, FeatureLineSelectionManager featureLineSelectionManager) {
        return new NavigationPane(imageModel, imageView, rangeSelectionManager, featureLineSelectionManager);
    }

}
