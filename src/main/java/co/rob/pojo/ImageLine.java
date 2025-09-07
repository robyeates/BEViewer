package co.rob.pojo;

import java.util.Arrays;

/**
 * The {@code ImageLine} record provides accessors for rendering an image line.
 * It is an immutable data carrier.
 *
 * @param lineForensicPath The forensic path of the image line.
 * @param text             The text of the image line.
 * @param highlightIndexes The array of text index characters to be highlighted.
 */
public record ImageLine(String lineForensicPath, String text, int[] highlightIndexes, int numHighlights) {

    /**
     * Creates an <code>ImageLine</code> object.
     *
     * @param lineForensicPath The forensic path of the image line
     * @param text             The text of the image line
     * @param highlightIndexes The array of text index characters to be highlighted
     * @param numHighlights    The number of text index characters to be highlighted in the image line
     */
    public ImageLine {
        highlightIndexes = Arrays.copyOf(highlightIndexes, highlightIndexes.length);
    }
}

