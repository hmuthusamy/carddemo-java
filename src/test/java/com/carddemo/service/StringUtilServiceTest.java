package com.carddemo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StringUtilService}.
 *
 * <p>Covers all COBOL string-handling patterns:
 * <ul>
 *   <li>INSPECT … CONVERTING</li>
 *   <li>INSPECT … TALLYING ALL / LEADING</li>
 *   <li>FUNCTION TRIM / UPPER-CASE / LOWER-CASE</li>
 *   <li>Left/right justification and PIC X(n) padding</li>
 *   <li>STRING … DELIMITED BY SIZE / SPACE INTO</li>
 *   <li>UNSTRING … DELIMITED BY … INTO</li>
 *   <li>PIC Z(n)9 display editing</li>
 *   <li>IS NUMERIC condition / FUNCTION TEST-NUMVAL</li>
 * </ul>
 */
@DisplayName("StringUtilService – COBOL string-operation migration tests")
class StringUtilServiceTest {

    private StringUtilService service;

    private static final String ALPHA_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String ALPHA_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @BeforeEach
    void setUp() {
        service = new StringUtilService();
    }

    // =========================================================================
    // 1. INSPECT … CONVERTING
    // =========================================================================

    @Nested
    @DisplayName("convertChars() – INSPECT … CONVERTING")
    class ConvertCharsTests {

        @Test
        @DisplayName("Converts lowercase to uppercase (standard COBOL CONVERTING)")
        void lowercaseToUppercase() {
            assertEquals("HELLO WORLD",
                    service.convertChars("hello world", ALPHA_LOWER, ALPHA_UPPER));
        }

        @Test
        @DisplayName("Converts uppercase to lowercase")
        void uppercaseToLowercase() {
            assertEquals("hello",
                    service.convertChars("HELLO", ALPHA_UPPER, ALPHA_LOWER));
        }

        @Test
        @DisplayName("Non-matching characters are left unchanged")
        void nonMatchingCharsUnchanged() {
            assertEquals("123 ABC",
                    service.convertChars("123 ABC", ALPHA_LOWER, ALPHA_UPPER));
        }

        @Test
        @DisplayName("Null input returns empty string")
        void nullInput_returnsEmpty() {
            assertEquals("", service.convertChars(null, ALPHA_LOWER, ALPHA_UPPER));
        }

        @Test
        @DisplayName("Empty fromChars returns input unchanged")
        void emptyFromChars_returnsInput() {
            assertEquals("hello", service.convertChars("hello", "", ""));
        }

        @Test
        @DisplayName("Mismatched from/to lengths throw IllegalArgumentException")
        void mismatchedLengths_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.convertChars("abc", "abc", "ab"));
        }
    }

    // =========================================================================
    // 2. INSPECT … TALLYING
    // =========================================================================

    @Nested
    @DisplayName("tallyAll() / tallyLeading() – INSPECT … TALLYING")
    class TallyTests {

        @Test
        @DisplayName("tallyAll counts all occurrences of a character")
        void tallyAll_countsAll() {
            assertEquals(3, service.tallyAll("banana", 'a'));
        }

        @Test
        @DisplayName("tallyAll returns 0 for null input")
        void tallyAll_nullReturnsZero() {
            assertEquals(0, service.tallyAll(null, 'a'));
        }

        @Test
        @DisplayName("tallyLeading counts only leading occurrences")
        void tallyLeading_onlyLeading() {
            assertEquals(3, service.tallyLeading("   hello", ' '));
        }

        @Test
        @DisplayName("tallyLeading stops at first non-matching character")
        void tallyLeading_stopsAtFirstNonMatch() {
            assertEquals(2, service.tallyLeading("  abc  ", ' '));
        }

        @Test
        @DisplayName("tallyLeadingSpaces counts leading spaces")
        void tallyLeadingSpaces() {
            assertEquals(4, service.tallyLeadingSpaces("    test"));
        }

        @Test
        @DisplayName("tallyLeadingSpaces returns 0 for non-space start")
        void tallyLeadingSpaces_noLeadingSpaces() {
            assertEquals(0, service.tallyLeadingSpaces("test  "));
        }
    }

    // =========================================================================
    // 3. FUNCTION TRIM / UPPER-CASE / LOWER-CASE
    // =========================================================================

    @Nested
    @DisplayName("FUNCTION TRIM / UPPER-CASE / LOWER-CASE")
    class TrimCaseTests {

        @Test
        @DisplayName("trim removes leading and trailing spaces")
        void trim_removesLeadingAndTrailing() {
            assertEquals("hello", service.trim("  hello  "));
        }

        @Test
        @DisplayName("trim on null returns empty string")
        void trim_nullReturnsEmpty() {
            assertEquals("", service.trim(null));
        }

        @Test
        @DisplayName("trimLeading removes only leading spaces")
        void trimLeading_onlyLeading() {
            assertEquals("hello  ", service.trimLeading("  hello  "));
        }

        @Test
        @DisplayName("trimTrailing removes only trailing spaces")
        void trimTrailing_onlyTrailing() {
            assertEquals("  hello", service.trimTrailing("  hello  "));
        }

        @Test
        @DisplayName("toUpperCase converts all chars to upper")
        void toUpperCase() {
            assertEquals("CARDDEMO", service.toUpperCase("CardDemo"));
        }

        @Test
        @DisplayName("toLowerCase converts all chars to lower")
        void toLowerCase() {
            assertEquals("carddemo", service.toLowerCase("CardDemo"));
        }

        @Test
        @DisplayName("toUpperCase on null returns empty")
        void toUpperCase_nullReturnsEmpty() {
            assertEquals("", service.toUpperCase(null));
        }
    }

    // =========================================================================
    // 4. Left / right justification and PIC X(n) padding
    // =========================================================================

    @Nested
    @DisplayName("leftJustify() / rightJustify() / zeroPadLeft() – PIC X(n) MOVE")
    class JustificationTests {

        @Test
        @DisplayName("leftJustify pads with spaces on the right")
        void leftJustify_rightPadsWithSpaces() {
            assertEquals("HELLO     ", service.leftJustify("HELLO", 10));
        }

        @Test
        @DisplayName("leftJustify truncates to field width")
        void leftJustify_truncatesLong() {
            assertEquals("HELLO", service.leftJustify("HELLO WORLD", 5));
        }

        @Test
        @DisplayName("leftJustify with null produces all spaces")
        void leftJustify_nullProducesSpaces() {
            assertEquals("     ", service.leftJustify(null, 5));
        }

        @Test
        @DisplayName("rightJustify pads with spaces on the left")
        void rightJustify_leftPadsWithSpaces() {
            assertEquals("     HELLO", service.rightJustify("HELLO", 10));
        }

        @Test
        @DisplayName("rightJustify truncates to field width")
        void rightJustify_truncatesLong() {
            assertEquals("HELLO", service.rightJustify("HELLO WORLD", 5));
        }

        @Test
        @DisplayName("zeroPadLeft pads numeric string with leading zeros")
        void zeroPadLeft_addsLeadingZeros() {
            assertEquals("00042", service.zeroPadLeft("42", 5));
        }

        @Test
        @DisplayName("zeroPadLeft truncates from left when too long")
        void zeroPadLeft_truncatesFromLeft() {
            assertEquals("23456", service.zeroPadLeft("123456", 5));
        }

        @Test
        @DisplayName("moveToField left-justifies and pads to field width")
        void moveToField_leftJustifiesAndPads() {
            String result = service.moveToField("ACCT", 8);
            assertEquals("ACCT    ", result);
            assertEquals(8, result.length());
        }
    }

    // =========================================================================
    // 5. STRING … DELIMITED BY SIZE / SPACE INTO
    // =========================================================================

    @Nested
    @DisplayName("stringInto() / stringDelimitedBySpace() – STRING … INTO")
    class StringIntoTests {

        @Test
        @DisplayName("stringInto concatenates all parts by SIZE")
        void stringInto_concatenatesAll() {
            String result = service.stringInto(20, "Hello", " ", "World");
            assertEquals("Hello World         ", result);
        }

        @Test
        @DisplayName("stringInto truncates to maxLength")
        void stringInto_truncatesToMax() {
            String result = service.stringInto(5, "Hello", " World");
            assertEquals("Hello", result);
        }

        @Test
        @DisplayName("stringInto with null parts skips them")
        void stringInto_skipNulls() {
            String result = service.stringInto(10, "Hello", null, "!");
            assertEquals("Hello!    ", result);
        }

        @Test
        @DisplayName("stringDelimitedBySpace stops at first space in each part")
        void stringDelimitedBySpace_stopsAtSpace() {
            // "Hello World" → delimited by SPACE → takes "Hello" only
            String result = service.stringDelimitedBySpace(20, "Hello World", "!");
            assertEquals("Hello!              ", result);
        }

        @Test
        @DisplayName("stringDelimitedBySpace includes full part if no space")
        void stringDelimitedBySpace_includesFullPartIfNoSpace() {
            String result = service.stringDelimitedBySpace(10, "CARD", "DEMO");
            assertEquals("CARDDEMO  ", result);
        }
    }

    // =========================================================================
    // 6. UNSTRING … DELIMITED BY … INTO
    // =========================================================================

    @Nested
    @DisplayName("unstringToken() – UNSTRING … DELIMITED BY … INTO")
    class UnstringTests {

        @Test
        @DisplayName("Splits on '/' and returns first token")
        void unstring_firstToken() {
            assertEquals("2023      ", service.unstringToken("2023/12/31", "/", 0, 10));
        }

        @Test
        @DisplayName("Splits on '/' and returns second token")
        void unstring_secondToken() {
            assertEquals("12   ", service.unstringToken("2023/12/31", "/", 1, 5));
        }

        @Test
        @DisplayName("Returns spaces if index out of range")
        void unstring_outOfRange_returnsSpaces() {
            assertEquals("     ", service.unstringToken("2023/12", "/", 5, 5));
        }

        @Test
        @DisplayName("Splits on comma delimiter")
        void unstring_commaDelimiter() {
            assertEquals("SMITH     ", service.unstringToken("SMITH,JOHN,A", ",", 0, 10));
        }
    }

    // =========================================================================
    // 7. PIC Z(n)9 display editing
    // =========================================================================

    @Nested
    @DisplayName("formatPicZ() – PIC Z(n)9 leading zero suppression")
    class FormatPicZTests {

        @Test
        @DisplayName("Leading zeros replaced by spaces (PIC ZZZZZ9)")
        void formatPicZ_replacesLeadingZeros() {
            assertEquals("    42", service.formatPicZ("000042", 6));
        }

        @Test
        @DisplayName("Last digit always shown even if zero (PIC ZZZZ9)")
        void formatPicZ_lastDigitShown() {
            assertEquals("     0", service.formatPicZ("000000", 6));
        }

        @Test
        @DisplayName("No leading zeros means no replacement")
        void formatPicZ_noLeadingZeros() {
            assertEquals("123456", service.formatPicZ("123456", 6));
        }
    }

    // =========================================================================
    // 8. IS NUMERIC / EQUAL SPACES conditions
    // =========================================================================

    @Nested
    @DisplayName("isNumeric() / isSpaces() – COBOL condition tests")
    class ConditionTests {

        @ParameterizedTest(name = "isNumeric({0}) → true")
        @ValueSource(strings = {"0", "123", "9999999999"})
        void isNumeric_trueForDigits(String s) {
            assertTrue(service.isNumeric(s));
        }

        @ParameterizedTest(name = "isNumeric({0}) → false")
        @ValueSource(strings = {"", "abc", "12.3", "1 2", " "})
        void isNumeric_falseForNonDigits(String s) {
            assertFalse(service.isNumeric(s));
        }

        @Test
        void isNumeric_nullReturnsFalse() {
            assertFalse(service.isNumeric(null));
        }

        @ParameterizedTest(name = "isSpaces({0}) → true")
        @ValueSource(strings = {"", " ", "   "})
        void isSpaces_trueForBlank(String s) {
            assertTrue(service.isSpaces(s));
        }

        @Test
        void isSpaces_nullReturnsTrue() {
            assertTrue(service.isSpaces(null));
        }

        @Test
        void isSpaces_falseForNonBlank() {
            assertFalse(service.isSpaces("X"));
        }

        @Test
        @DisplayName("isAllChar detects all-same-character strings")
        void isAllChar_allSameChar() {
            assertTrue(service.isAllChar("ZZZZ", 'Z'));
            assertFalse(service.isAllChar("ZZAZ", 'Z'));
        }
    }
}
