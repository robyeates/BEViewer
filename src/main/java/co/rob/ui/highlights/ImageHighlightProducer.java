package co.rob.ui.highlights;

import co.rob.state.ImageModel;
import co.rob.ui.renderer.FeatureFieldFormatter;
import co.rob.util.UTF16Tools;
import co.rob.util.UTF8Tools;

import java.util.Arrays;
import java.util.List;


/**
 * This class produces image highlight flag output from the models that offer highlighting.
 */
public class ImageHighlightProducer {

  // resources

  // resources from models used temporarily during calculation
  private byte[] paddedPageBytes;
  private int paddingPrefixSize;
  private int availablePageSize;
  private boolean highlightMatchCase;

  // calculated
  private boolean[] pageHighlightFlags;

  /**
   * Creates a highlight producer for calculating page highlight flags.
   */
  public ImageHighlightProducer() {
  }

  /**
   * Returns highlight flags based on highlight values in models.
   */
  public boolean[] getPageHighlightFlags(ImageModel.ImagePage imagePage,
                                         List<byte[]> userHighlights,
                                         boolean highlightMatchCase) {

    // cache some page parameters
    paddedPageBytes = imagePage.paddedPageBytes();
    paddingPrefixSize = imagePage.paddingPrefixSize();
    availablePageSize = imagePage.pageBytes().length;

    // initialize the highlight flags array given the available page size
    pageHighlightFlags = new boolean[availablePageSize];

    // set highlightMatchCase
    this.highlightMatchCase = highlightMatchCase;

    // set highlight flags for the image page
    setFlags(FeatureFieldFormatter.getImageHighlightVector(imagePage));

    // set highlight flags for each user highlight
    if (userHighlights != null) {
      setFlags(userHighlights);
    }

    return pageHighlightFlags;
  }

  private void setFlags(List<byte[]> highlightVector) {
      highlightVector.stream()
              .map(UTF8Tools::unescapeBytes)
              .peek(this::addHighlightFlags) // Set highlight flags for UTF8
              .map(UTF16Tools::getUTF16Bytes)
              .forEach(this::addHighlightFlags); // Set highlight flags for UTF16
  }

    /**
     * Adds highlight flags for a given byte array. This method
     * identifies all occurrences of the `highlightBytes` within the
     * `paddedPageBytes` and marks the corresponding positions in
     * `pageHighlightFlags`.
     * Original comment - create flags in overlapHighlightFlags and then OR them to pageHighlightFlags
     * @param highlightBytes The byte array to search for and highlight.
     */
    private void addHighlightFlags(byte[] highlightBytes) {
        if (highlightBytes == null || highlightBytes.length == 0) {
            return;
        }

        int highlightLength = highlightBytes.length;
        int paddedPageLength = paddedPageBytes.length;

        // Use a temporary boolean array to mark matches without concurrent modification issues.
        boolean[] tempHighlights = new boolean[paddedPageLength];

        // Find and set highlights for all matches in the padded page bytes.
        for (int i = 0; i <= paddedPageLength - highlightLength; i++) {
            boolean match = true;
            for (int j = 0; j < highlightLength; j++) {
                byte paddedByte = paddedPageBytes[i + j];
                byte highlightByte = highlightBytes[j];

                // Normalize bytes to lowercase if case-insensitive matching is enabled.
                if (!highlightMatchCase) {
                    paddedByte = UTF8Tools.asciiToLower(paddedByte);
                    highlightByte = UTF8Tools.asciiToLower(highlightByte);
                }

                if (paddedByte != highlightByte) {
                    match = false;
                    break;
                }
            }

            if (match) {
                // Mark the entire matched range in the temporary array.
                Arrays.fill(tempHighlights, i, i + highlightLength, true);
            }
        }

        // Merge the temporary highlights into the final pageHighlightFlags,
        // considering the padding offset.
        for (int i = 0; i < availablePageSize; i++) {
            if (tempHighlights[i + paddingPrefixSize]) {
                pageHighlightFlags[i] = true;
            }
        }
    }
}

