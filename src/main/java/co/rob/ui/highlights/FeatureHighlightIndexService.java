package co.rob.ui.highlights;

import co.rob.pojo.FeatureLine;
import co.rob.state.UserHighlightModel;
import co.rob.ui.selection.FeatureLineSelectionManager;
import co.rob.util.UTF8Tools;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static co.rob.util.UTF8Tools.asciiToLower;

public class FeatureHighlightIndexService {

    private final FeatureLineSelectionManager featureLineSelectionManager;
    private final UserHighlightModel userHighlightModel;

    @Inject
    public FeatureHighlightIndexService(FeatureLineSelectionManager featureLineSelectionManager, UserHighlightModel userHighlightModel) {
        this.featureLineSelectionManager = featureLineSelectionManager;
        this.userHighlightModel = userHighlightModel;
    }

    /**
     * obtain character highlight indexes for character matches
     * in the selected feature line and in user's text.
     */
    public List<FeatureHighlightIndex> getHighlightIndexes(char[] featureChars) {

        List<FeatureHighlightIndex> highlightIndexes = new ArrayList<>();

        // if the feature line matches the selected feature line, highlight the match
        final FeatureLine highlightFeatureLine = featureLineSelectionManager.getFeatureLineSelection();
        if (highlightFeatureLine != null) {
            String highlightText = highlightFeatureLine.formattedFeature();
            char[] highlightChars = highlightText.toCharArray();
            addHighlights(highlightIndexes, featureChars, highlightChars);
        }

        // add any highlights that match user's highlighting from the highlight model
        List<byte[]> highlightArray = userHighlightModel.getUserHighlightByteVector();

        highlightArray.forEach(b -> {
            char[] highlightChars = new String(UTF8Tools.unescapeBytes(b)).toCharArray();
            addHighlights(highlightIndexes, featureChars, highlightChars);
        });

        return highlightIndexes;
    }

    // add highlight indexes where highlightChars are in featureChars
    private void addHighlights(List<FeatureHighlightIndex> highlightIndexes, char[] featureChars, char[] highlightChars) {

        if (highlightChars.length == 0) {
            // do not generate 0-length highlight indexes
            return;
        }

        final boolean highlightMatchCase = userHighlightModel.isHighlightMatchCase();
        final int lastBeginIndex = featureChars.length - highlightChars.length;

        // scan featureChars for matching highlightChars
        for (int i = 0; i <= lastBeginIndex; i++) {
            boolean match = true;
            for (int j = 0; j < highlightChars.length; j++) {
                char featureChar = featureChars[i + j];
                char highlightChar = highlightChars[j];

                // manage case
                if (!highlightMatchCase) {
                    featureChar = asciiToLower(featureChar);
                    highlightChar = asciiToLower(highlightChar);
                }

                // check for match between chars in position
                if (featureChar != highlightChar) {
                    match = false;
                    break;
                }
            }
            if (match) {
                highlightIndexes.add(new FeatureHighlightIndex(i, highlightChars.length));
            }
        }
    }
}
