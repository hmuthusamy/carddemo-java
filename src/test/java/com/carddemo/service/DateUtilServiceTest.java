package com.carddemo.service;

import com.carddemo.service.DateUtilService.DateValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DateUtilService}.
 *
 * <p>Tests cover all COBOL equivalents:
 * <ul>
 *   <li>CSUTLDTC.cbl – CEEDAYS FC-* feedback code paths</li>
 *   <li>CSUTLDWY.cpy – date-edit flag conditions</li>
 *   <li>CSUTLDPY.cpy – EDIT-* paragraphs</li>
 *   <li>CODATECN.cpy – format conversion types 1 and 2</li>
 *   <li>CODATE01.cbl – CICS FORMATTIME MMDDYYYY</li>
 * </ul>
 */
@DisplayName("DateUtilService – COBOL CSUTLDTC migration tests")
class DateUtilServiceTest {

    private DateUtilService service;

    @BeforeEach
    void setUp() {
        service = new DateUtilService();
    }

    // =========================================================================
    // 1. validateDate() – CEEDAYS FC-* feedback code coverage
    // =========================================================================

    @Nested
    @DisplayName("validateDate() – CEEDAYS feedback code paths (CSUTLDTC.cbl)")
    class ValidateDateTests {

        @Test
        @DisplayName("FC-INVALID-DATE (severity 0) → 'Date is valid' for valid YYYYMMDD")
        void validDateYyyymmdd_returnsSeverityZero() {
            DateValidationResult result = service.validateDate("20231231", "YYYYMMDD");
            assertTrue(result.isValid());
            assertEquals(0, result.getSeverityCode());
            assertEquals("Date is valid", result.getResult().trim());
        }

        @Test
        @DisplayName("FC-INSUFFICIENT-DATA → severity 3 for null/blank date")
        void nullDate_returnsInsufficient() {
            DateValidationResult r = service.validateDate(null, "YYYYMMDD");
            assertFalse(r.isValid());
            assertEquals(3, r.getSeverityCode());
            assertTrue(r.getResult().contains("Insufficient"));
        }

        @Test
        @DisplayName("FC-INSUFFICIENT-DATA → severity 3 for blank date string")
        void blankDate_returnsInsufficient() {
            DateValidationResult r = service.validateDate("   ", "YYYYMMDD");
            assertFalse(r.isValid());
            assertEquals(3, r.getSeverityCode());
        }

        @Test
        @DisplayName("FC-BAD-PIC-STRING → severity 3 for unknown format mask")
        void unknownFormatMask_returnsBadPicString() {
            DateValidationResult r = service.validateDate("20231231", "YYMMDD");
            assertFalse(r.isValid());
            assertEquals(3, r.getSeverityCode());
            assertTrue(r.getResult().contains("Bad Pic String"));
        }

        @Test
        @DisplayName("FC-BAD-DATE-VALUE → severity 3 for invalid date value (Feb 30)")
        void invalidDateFeb30_returnsBadDateValue() {
            DateValidationResult r = service.validateDate("20230230", "YYYYMMDD");
            assertFalse(r.isValid());
            assertEquals(3, r.getSeverityCode());
            assertTrue(r.getResult().contains("Datevalue error"));
        }

        @Test
        @DisplayName("FC-NON-NUMERIC-DATA → severity 3 for non-numeric in YYYYMMDD format")
        void nonNumericInYyyymmdd_returnsNonNumericData() {
            DateValidationResult r = service.validateDate("ABCD1231", "YYYYMMDD");
            assertFalse(r.isValid());
            assertEquals(3, r.getSeverityCode());
            assertTrue(r.getResult().contains("Nonnumeric data"));
        }

        @Test
        @DisplayName("FC-INVALID-ERA → severity 3 for year outside century 19/20")
        void year2100_returnsInvalidEra() {
            DateValidationResult r = service.validateDate("21001201", "YYYYMMDD");
            assertFalse(r.isValid());
            assertEquals(3, r.getSeverityCode());
            assertTrue(r.getResult().contains("Invalid Era"));
        }

        @Test
        @DisplayName("Valid YYYY-MM-DD format is accepted")
        void validHyphenatedDate_isValid() {
            DateValidationResult r = service.validateDate("2023-12-31", "YYYY-MM-DD");
            assertTrue(r.isValid());
        }

        @Test
        @DisplayName("Valid MM/DD/YYYY format is accepted")
        void validMmDdYyyy_isValid() {
            DateValidationResult r = service.validateDate("12/31/2023", "MM/DD/YYYY");
            assertTrue(r.isValid());
        }

        @Test
        @DisplayName("Tested date and mask are echoed in result (WS-DATE / WS-DATE-FMT)")
        void testedDateAndMaskEchoedInResult() {
            DateValidationResult r = service.validateDate("20231231", "YYYYMMDD");
            assertEquals("20231231", r.getTestedDate().trim());
            assertEquals("YYYYMMDD", r.getMaskUsed().trim());
        }
    }

    // =========================================================================
    // 2. Date arithmetic (INTEGER-OF-DATE / Lillian)
    // =========================================================================

    @Nested
    @DisplayName("Date arithmetic (COBOL INTEGER-OF-DATE / CEEDAYS Lillian)")
    class DateArithmeticTests {

        @Test
        @DisplayName("daysBetween same date returns 0")
        void daysBetweenSameDate_returnsZero() {
            BigDecimal diff = service.daysBetween("20230101", "20230101");
            assertEquals(BigDecimal.ZERO, diff);
        }

        @Test
        @DisplayName("daysBetween consecutive days returns 1")
        void daysBetweenConsecutiveDays_returnsOne() {
            BigDecimal diff = service.daysBetween("20230101", "20230102");
            assertEquals(BigDecimal.ONE, diff);
        }

        @Test
        @DisplayName("daysBetween across year boundary is correct")
        void daysBetweenAcrossYear_correct() {
            // 2022-12-31 to 2023-01-01 = 1 day
            BigDecimal diff = service.daysBetween("20221231", "20230101");
            assertEquals(BigDecimal.ONE, diff);
        }

        @Test
        @DisplayName("daysBetween negative when from > to")
        void daysBetweenNegative_whenFromAfterTo() {
            BigDecimal diff = service.daysBetween("20230102", "20230101");
            assertEquals(BigDecimal.valueOf(-1), diff);
        }

        @Test
        @DisplayName("integerOfDate returns consistent epoch value")
        void integerOfDate_consistent() {
            BigDecimal d1 = service.integerOfDate("20230101");
            BigDecimal d2 = service.integerOfDate("20230102");
            assertEquals(BigDecimal.ONE, d2.subtract(d1));
        }

        @Test
        @DisplayName("isDateOfBirthValid returns true for past date")
        void pastDateOfBirth_isValid() {
            assertTrue(service.isDateOfBirthValid("19900101"));
        }

        @Test
        @DisplayName("isDateOfBirthValid returns false for future date")
        void futureDateOfBirth_isInvalid() {
            String future = LocalDate.now().plusDays(1)
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            assertFalse(service.isDateOfBirthValid(future));
        }
    }

    // =========================================================================
    // 3. Date-format conversions (CODATECN.cpy)
    // =========================================================================

    @Nested
    @DisplayName("Date format conversions (CODATECN.cpy types 1 and 2)")
    class DateFormatConversionTests {

        @Test
        @DisplayName("yyyymmddToHyphenated converts 20231231 → 2023-12-31")
        void yyyymmddToHyphenated() {
            assertEquals("2023-12-31", service.yyyymmddToHyphenated("20231231"));
        }

        @Test
        @DisplayName("hyphenatedToYyyymmdd converts 2023-12-31 → 20231231")
        void hyphenatedToYyyymmdd() {
            assertEquals("20231231", service.hyphenatedToYyyymmdd("2023-12-31"));
        }

        @Test
        @DisplayName("yyyymmddToMmDdYyyy converts 20231231 → 12/31/2023")
        void yyyymmddToMmDdYyyy() {
            assertEquals("12/31/2023", service.yyyymmddToMmDdYyyy("20231231"));
        }

        @Test
        @DisplayName("mmDdYyyyToYyyymmdd converts 12/31/2023 → 20231231")
        void mmDdYyyyToYyyymmdd() {
            assertEquals("20231231", service.mmDdYyyyToYyyymmdd("12/31/2023"));
        }

        @Test
        @DisplayName("currentDateYyyymmdd returns 8-digit date string")
        void currentDateYyyymmdd_is8Digits() {
            String d = service.currentDateYyyymmdd();
            assertNotNull(d);
            assertEquals(8, d.length());
            assertTrue(d.matches("\\d{8}"));
        }

        @Test
        @DisplayName("currentDateMmDdYyyy returns MM/DD/YYYY format")
        void currentDateMmDdYyyy_hasCorrectFormat() {
            String d = service.currentDateMmDdYyyy();
            assertNotNull(d);
            assertTrue(d.matches("\\d{2}/\\d{2}/\\d{4}"));
        }

        @Test
        @DisplayName("Round-trip: YYYYMMDD → hyphenated → YYYYMMDD")
        void roundTripYyyymmddHyphenated() {
            String original = "20000229";  // leap year
            String rt = service.hyphenatedToYyyymmdd(service.yyyymmddToHyphenated(original));
            assertEquals(original, rt);
        }
    }

    // =========================================================================
    // 4. Date component validation (CSUTLDWY.cpy / CSUTLDPY.cpy paragraphs)
    // =========================================================================

    @Nested
    @DisplayName("Date component validation (CSUTLDWY / CSUTLDPY paragraphs)")
    class DateComponentValidationTests {

        // isValidYear
        @ParameterizedTest(name = "valid year={0}")
        @ValueSource(strings = {"1900", "1999", "2000", "2099", "2024"})
        void validYears_pass(String year) {
            assertTrue(service.isValidYear(year));
        }

        @ParameterizedTest(name = "invalid year={0}")
        @ValueSource(strings = {"1800", "2100", "ABCD", "", "  "})
        void invalidYears_fail(String year) {
            assertFalse(service.isValidYear(year));
        }

        // isValidMonth
        @ParameterizedTest(name = "valid month={0}")
        @ValueSource(strings = {"01", "1", "06", "12"})
        void validMonths_pass(String month) {
            assertTrue(service.isValidMonth(month));
        }

        @ParameterizedTest(name = "invalid month={0}")
        @ValueSource(strings = {"0", "00", "13", "AB"})
        void invalidMonths_fail(String month) {
            assertFalse(service.isValidMonth(month));
        }

        // isValidDay
        @ParameterizedTest(name = "valid day={0}")
        @ValueSource(strings = {"1", "01", "15", "31"})
        void validDays_pass(String day) {
            assertTrue(service.isValidDay(day));
        }

        @ParameterizedTest(name = "invalid day={0}")
        @ValueSource(strings = {"0", "00", "32", "AB"})
        void invalidDays_fail(String day) {
            assertFalse(service.isValidDay(day));
        }

        // validateDateComponents – 31-day month rule
        @Test
        @DisplayName("April 31 is invalid (not a 31-day month)")
        void april31_isInvalid() {
            String msg = service.validateDateComponents("20230431");
            assertFalse(msg.isEmpty());
            assertTrue(msg.contains("31 days"));
        }

        // validateDateComponents – February 30
        @Test
        @DisplayName("February 30 is always invalid")
        void feb30_isInvalid() {
            String msg = service.validateDateComponents("20230230");
            assertFalse(msg.isEmpty());
            assertTrue(msg.contains("30 days"));
        }

        // validateDateComponents – leap year Feb 29
        @Test
        @DisplayName("Feb 29 on leap year 2000 is valid")
        void feb29LeapYear2000_isValid() {
            String msg = service.validateDateComponents("20000229");
            assertTrue(msg.isEmpty(), "Expected valid but got: " + msg);
        }

        @Test
        @DisplayName("Feb 29 on non-leap year 2023 is invalid")
        void feb29NonLeapYear2023_isInvalid() {
            String msg = service.validateDateComponents("20230229");
            assertFalse(msg.isEmpty());
            assertTrue(msg.contains("leap year"));
        }

        @Test
        @DisplayName("Null/blank date returns error message")
        void nullDate_returnsError() {
            String msg = service.validateDateComponents(null);
            assertFalse(msg.isEmpty());
        }
    }

    // =========================================================================
    // 5. toString() of DateValidationResult mirrors WS-MESSAGE layout
    // =========================================================================

    @Test
    @DisplayName("DateValidationResult.toString() format matches WS-MESSAGE layout")
    void validationResultToString_matchesWsMessageLayout() {
        DateValidationResult r = service.validateDate("20231231", "YYYYMMDD");
        String s = r.toString();
        assertNotNull(s);
        assertTrue(s.contains("Mesg Code:"));
        assertTrue(s.contains("TstDate:"));
        assertTrue(s.contains("Mask used:"));
    }
}
