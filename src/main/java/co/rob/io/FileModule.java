package co.rob.io;

import co.rob.io.print.FeatureRangePrinter;
import co.rob.state.BookmarksModel;
import co.rob.state.FeaturesModel;
import co.rob.state.ImageView;
import co.rob.state.ReportsModel;
import co.rob.state.qualifier.Features;
import co.rob.state.qualifier.ReferencedFeatures;
import co.rob.ui.highlights.FeatureHighlightIndexService;
import co.rob.ui.selection.HighlightColorSelectionModel;
import co.rob.ui.selection.RangeSelectionManager;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;


@Module
public class FileModule {

    @Provides
    @Singleton
    ImageFileReader imageFileReader() {
        return new ImageFileReader();
    }

    @Provides
    @Singleton
    ReportFileReader reportFileReader(ReportsModel reportsModel) {
        return new ReportFileReader(reportsModel);
    }

    @Provides
    @Singleton
    WorkSettingsReader workSettingsReader() {
        return new WorkSettingsReader();
    }

    @Provides
    @Singleton
    WorkSettingsWriter workSettingsWriter() {
        return new WorkSettingsWriter();
    }

    @Provides
    @Singleton
    BookmarksWriter bookmarksWriter(BookmarksModel bookmarksModel) {
        return new BookmarksWriter(bookmarksModel);
    }

    @Provides
    @Singleton
    BookmarksReader bookmarksReader() {
        return new BookmarksReader();
    }

    @Provides
    @Singleton
    FeatureRangePrinter featureRangePrinter(RangeSelectionManager rangeSelectionManager, HighlightColorSelectionModel highlightColorSelectionModel, FeatureHighlightIndexService featureHighlightIndexService,
                                            ImageView imageView, @Features FeaturesModel featuresModel, @ReferencedFeatures FeaturesModel referencedFeaturesModel) {
        return new FeatureRangePrinter(rangeSelectionManager, highlightColorSelectionModel, featureHighlightIndexService, imageView, featuresModel, referencedFeaturesModel);
    }
}
