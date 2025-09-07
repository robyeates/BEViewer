package co.rob.io.features;

import co.rob.util.UTF8Tools;

/**
 * Decomposed from FeaturesParserThread
 */
public class FeaturesFilterMatcher {
    private final byte[] utf8Filter;
    private final byte[] utf16Filter;
    private final boolean matchCase;

    public FeaturesFilterMatcher(byte[] requestedFilterBytes, boolean matchCase) {
        this.matchCase = matchCase;
        if (requestedFilterBytes == null || requestedFilterBytes.length == 0) {
            utf8Filter = null;
            utf16Filter = null;
            return;
        }
        byte[] filter = matchCase
                ? requestedFilterBytes
                : UTF8Tools.asciiToLower(requestedFilterBytes);

        if (UTF8Tools.escapedLooksLikeUTF16(filter)) {
            utf8Filter = UTF8Tools.utf16To8Correct(filter);
            utf16Filter = filter;
        } else {
            utf8Filter = filter;
            utf16Filter = UTF8Tools.utf8To16Correct(filter);
        }
    }

    public boolean isEnabled() {
        return utf8Filter != null;
    }

    public boolean matches(byte[] bytes, int offset, int length) {
        if (!isEnabled()) return true;
        return matchesFilter(utf8Filter, bytes, offset, length) ||
                matchesFilter(utf16Filter, bytes, offset, length);
    }

    private boolean matchesFilter(byte[] filter, byte[] bytes, int offset, int length) {
        if (filter == null || filter.length > length - offset) return false;
        for (int i = 0; i < filter.length; i++) {
            byte b = bytes[offset + i];
            if (!matchCase && b >= 'A' && b <= 'Z') {
                b += 0x20;
            }
            if (b != filter[i]) return false;
        }
        return true;
    }
}
