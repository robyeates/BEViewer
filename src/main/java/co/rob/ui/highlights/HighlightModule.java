package co.rob.ui.highlights;

import co.rob.state.UserHighlightModel;
import co.rob.ui.selection.FeatureLineSelectionManager;
import dagger.Provides;
import dagger.Module;

import javax.inject.Singleton;

@Module
public class HighlightModule {

    @Provides
    @Singleton
    FeatureHighlightIndexService featureHighlightIndexService(FeatureLineSelectionManager featureLineSelectionManager, UserHighlightModel userHighlightModel) {
        return new FeatureHighlightIndexService(featureLineSelectionManager, userHighlightModel);
    }

    @Provides
    @Singleton
    UserHighlightModel userHighlightModel() {
        return new UserHighlightModel();
    }
}