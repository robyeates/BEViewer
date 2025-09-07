package co.rob.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DODGY AFFFFFFFF - thanks Gemini")
class UTF8ToolsTest {

    // Helper method to create byte arrays for method sources
    private static Stream<Arguments> getUnescapeBytesTestData() {
        return Stream.of(
                Arguments.of("test\\nstring".getBytes(), "test\nstring".getBytes()),
                Arguments.of("\\r\\t\\b".getBytes(), "\r\t\b".getBytes()),
                Arguments.of("\\u0041".getBytes(), "A".getBytes()),
                Arguments.of("\\\\".getBytes(), "\\".getBytes()),
                Arguments.of("no-escape".getBytes(), "no-escape".getBytes())
        );
    }

    @ParameterizedTest(name = "{0} should be unescaped to {1}")
    @MethodSource(value = "getUnescapeBytesTestData")
    @DisplayName("Test unescapeBytes() with various escape sequences")
    void testUnescapeBytes(byte[] input, byte[] expected) {
        assertArrayEquals(expected, UTF8Tools.unescapeBytes(input));
    }

    //------------------------------------------------------------------------------------------------------------------

    private static Stream<Arguments> getUnescapeEscapeTestData() {
        return Stream.of(
                Arguments.of("test\\\\x5Cstring".getBytes(), "test\\string".getBytes()),
                Arguments.of("another\\\\134test".getBytes(), "another\\test".getBytes()),
                Arguments.of("\\\\x5cthis\\\\134".getBytes(), "\\this\\".getBytes()),
                Arguments.of("unchanged\\\\ntest".getBytes(), "unchanged\\\\ntest".getBytes()),
                Arguments.of("\\\\".getBytes(), "\\\\".getBytes())
        );
    }

    @ParameterizedTest(name = "{0} should unescape backslash to {1}")
    @MethodSource("getUnescapeEscapeTestData")
    @DisplayName("Test unescapeEscape() to only unescape backslashes")
    void testUnescapeEscape(byte[] input, byte[] expected) {
        assertArrayEquals(expected, UTF8Tools.unescapeEscape(input));
    }

    //------------------------------------------------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} should convert to UTF-16 correctly")
    @CsvSource({
            "hello, \u0000h\u0000e\u0000l\u0000l\u0000o",
            "world, \u0000w\u0000o\u0000r\u0000l\u0000d",
            "A, \u0000A"
    })
    @DisplayName("Test utf8To16Correct() for correct encoding")
    void testUtf8To16Correct(String utf8Str, String expectedUtf16Str) {
        byte[] utf8Bytes = utf8Str.getBytes(StandardCharsets.UTF_8);
        byte[] expectedUtf16Bytes = expectedUtf16Str.getBytes(StandardCharsets.UTF_16);
        assertArrayEquals(expectedUtf16Bytes, UTF8Tools.utf8To16Correct(utf8Bytes));
    }

    //------------------------------------------------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} should convert to UTF-8 correctly")
    @CsvSource({
            "h, \u0000h",
            "world, \u0000w\u0000o\u0000r\u0000l\u0000d",
            "test, \u0000t\u0000e\u0000s\u0000t"
    })
    @DisplayName("Test utf16To8Correct() for correct encoding")
    void testUtf16To8Correct(String expectedUtf8Str, String utf16Str) {
        byte[] utf16Bytes = utf16Str.getBytes(StandardCharsets.UTF_16);
        byte[] expectedUtf8Bytes = expectedUtf8Str.getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expectedUtf8Bytes, UTF8Tools.utf16To8Correct(utf16Bytes));
    }

    //------------------------------------------------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} should be converted to lowercase {1}")
    @CsvSource({
            "ABC, abc",
            "Hello World, hello world",
            "123, 123",
            "aBcDe, abcde",
            "zZ, zz"
    })
    @DisplayName("Test toLower() to convert bytes to lowercase")
    void testAsciiToLower(String input, String expected) {
        assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8), UTF8Tools.asciiToLower(input.getBytes()));
    }

    //------------------------------------------------------------------------------------------------------------------

    @ParameterizedTest(name = "bytesMatch() should return {2} for {0} and {1}")
    @CsvSource({
            "abc, abc, true",
            "abc, def, false",
            ", , true",
            "abc, , false",
            ", abc, false",
            "abc, abcd, false"
    })
    @DisplayName("Test bytesMatch() for array equality")
    void testBytesMatch(String input1, String input2, boolean expected) {
        byte[] bytes1 = input1 != null ? input1.getBytes() : null;
        byte[] bytes2 = input2 != null ? input2.getBytes() : null;
        assertEquals(expected, UTF8Tools.bytesMatch(bytes1, bytes2));
    }

    //------------------------------------------------------------------------------------------------------------------

    @ParameterizedTest(name = "stripNulls() should remove escaped nulls from {0}")
    @CsvSource({
            "test\\\\000string, teststring",
            "another\\\\x00test, anothertest",
            "\\\\x00\\\\000, ''",
            "\\\\x00\\\\n\\\\t\\\\000, \\\\n\\\\t"
    })
    @DisplayName("Test stripNulls() to remove escaped null characters")
    void testStripNulls(String input, String expected) {
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expectedBytes, UTF8Tools.stripNulls(inputBytes));
    }

    //------------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("Test escapedLooksLikeUTF16() with UTF-16LE data")
    void testEscapedLooksLikeUTF16WithLE() {
        String testString = "hello world";
        // Create a byte array with a pattern that looks like UTF-16LE
        byte[] utf16LEBytes = testString.getBytes(StandardCharsets.UTF_16LE);
        // Add a backslash to escape
        String escapedUtf16LEString = "a\\u0000" + new String(utf16LEBytes);
        byte[] escapedBytes = escapedUtf16LEString.getBytes(StandardCharsets.UTF_8);
        assertTrue(UTF8Tools.escapedLooksLikeUTF16(escapedBytes));
    }

    @Test
    @DisplayName("Test escapedLooksLikeUTF16() with UTF-16BE data")
    void testEscapedLooksLikeUTF16WithBE() {
        String testString = "hello world";
        // Create a byte array with a pattern that looks like UTF-16BE
        byte[] utf16BEBytes = testString.getBytes(StandardCharsets.UTF_16BE);
        // Add a backslash to escape
        String escapedUtf16BEString = "a\\u0000" + new String(utf16BEBytes);
        byte[] escapedBytes = escapedUtf16BEString.getBytes(StandardCharsets.UTF_8);
        assertTrue(UTF8Tools.escapedLooksLikeUTF16(escapedBytes));
    }
}