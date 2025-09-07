package co.rob.io.features;

import co.rob.state.FastFeatureLineTable;

/**
 * Decomposed from FeaturesParserThread
 */
public class FeaturesParser {
    private final FeaturesFilterMatcher filterMatcher;
    private final FastFeatureLineTable table;
    private int parseState = 0;

    private static final int PARSING_START = 0;
    private static final int PARSING_TYPE_FIELD = 1;
    private static final int PARSING_FEATURE_FIELD = 2;
    private static final int PARSING_CONTEXT_FIELD = 3;
    private static final int PARSING_DROP = 4;
    private static final int PARSING_KEEP = 5;

    public FeaturesParser(FastFeatureLineTable table, FeaturesFilterMatcher filterMatcher) {
        this.table = table;
        this.filterMatcher = filterMatcher;
    }

    public void parseChunk(byte[] buffer, int length, long offset) {
        long lineStart = offset;

        for (int i = 0; i < length; i++) {
            byte b = buffer[i];
            if (b == '\n') {
                long lineStop = offset + i;
                if (parseState == PARSING_KEEP || (!filterMatcher.isEnabled() && parseState != PARSING_DROP)) {
                    long lineLength = lineStop - lineStart;
                    if (lineLength > Integer.MAX_VALUE) {
                        throw new RuntimeException("Unexpected line length");
                    }
                    table.put(lineStart, (int)lineLength);
                }
                lineStart = lineStop + 1;
                parseState = PARSING_START;
            } else {
                updateState(b, buffer, i, length);
            }
        }
    }

    private void updateState(byte b, byte[] buffer, int i, int length) {
        switch (parseState) {
            case PARSING_START:
                if (b == '#') parseState = PARSING_DROP;
                else if (b == '\t') parseState = PARSING_FEATURE_FIELD;
                else parseState = PARSING_TYPE_FIELD;
                break;
            case PARSING_TYPE_FIELD:
                if (b == '\t') parseState = PARSING_FEATURE_FIELD;
                break;
            case PARSING_FEATURE_FIELD:
                if (b == '\t') parseState = PARSING_CONTEXT_FIELD;
                else if (filterMatcher.isEnabled() && filterMatcher.matches(buffer, i, length)) {
                    parseState = PARSING_KEEP;
                }
                break;
            default:
                // context, drop, keep = no-op
        }
    }

    public FastFeatureLineTable getTable() {
        return table;
    }
}