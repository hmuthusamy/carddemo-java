package com.carddemo.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * DateUtilService – Java equivalent of COBOL utility program CSUTLDTC.
 *
 * <p>COBOL source: app/cbl/CSUTLDTC.cbl
 * <ul>
 *   <li>CSUTLDTC validates a date string against a supplied format mask by
 *       delegating to the LE runtime service CEEDAYS (Lillian-day conversion).
 *       This implementation replaces CEEDAYS with {@link java.time.LocalDate}
 *       parsing, which provides equivalent Gregorian-calendar validation.</li>
 *   <li>Date arithmetic (INTEGER-OF-DATE / Lillian offset) is replaced by
 *       {@link LocalDate#toEpochDay()} and {@link ChronoUnit#DAYS}.</li>
 *   <li>Date-format conversions defined in CODATECN.cpy are implemented as
 *       explicit {@code convert*()} methods.</li>
 *   <li>COMP-3 numeric parameters that carried Lillian values are represented
 *       as {@link BigDecimal} to preserve precision fidelity.</li>
 * </ul>
 *
 * <h3>COBOL feedback-code mapping</h3>
 * <pre>
 *   FC-INVALID-DATE        → "Date is valid"    (severity 0 = OK)
 *   FC-INSUFFICIENT-DATA   → "Insufficient"
 *   FC-BAD-DATE-VALUE      → "Datevalue error"
 *   FC-INVALID-ERA         → "Invalid Era"
 *   FC-UNSUPP-RANGE        → "Unsupp. Range"
 *   FC-INVALID-MONTH       → "Invalid month"
 *   FC-BAD-PIC-STRING      → "Bad Pic String"
 *   FC-NON-NUMERIC-DATA    → "Nonnumeric data"
 *   FC-YEAR-IN-ERA-ZERO    → "YearInEra is 0"
 *   OTHER                  → "Date is invalid"
 * </pre>
 */
@Service
public class DateUtilService {

    // ---------------------------------------------------------------------------
    // Supported format masks (COBOL CEEDAYS picture strings → DateTimeFormatter)
    // ---------------------------------------------------------------------------
    private static final DateTimeFormatter FMT_YYYYMMDD   = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FMT_YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FMT_MM_DD_YYYY = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter FMT_MMDDYYYY   = DateTimeFormatter.ofPattern("MMddyyyy");
    private static final DateTimeFormatter FMT_DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Valid century values as per CSUTLDWY.cpy: THIS-CENTURY (20) and LAST-CENTURY (19). */
    private static final Set<Integer> VALID_CENTURIES = Set.of(19, 20);

    /** Months with 31 days (COBOL 88 WS-31-DAY-MONTH). */
    private static final Set<Integer> MONTHS_31 = Set.of(1, 3, 5, 7, 8, 10, 12);

    // ---------------------------------------------------------------------------
    // DateValidationResult – mirrors the WS-DATE-VALIDATION-RESULT layout in
    // CSUTLDWY.cpy so callers can inspect severity / message codes exactly as
    // the COBOL RETURN-CODE / WS-SEVERITY-N / WS-MSG-NO-N fields did.
    // ---------------------------------------------------------------------------

    /**
     * Mirrors the CEEDAYS feedback structure returned in LS-RESULT (80 chars).
     */
    public static class DateValidationResult {
        private int    severityCode;   // WS-SEVERITY-N  (0 = OK)
        private int    messageCode;    // WS-MSG-NO-N
        private String result;         // WS-RESULT      (15 chars)
        private String testedDate;     // WS-DATE        (10 chars)
        private String maskUsed;       // WS-DATE-FMT    (10 chars)

        public boolean isValid()        { return severityCode == 0; }
        public int     getSeverityCode(){ return severityCode; }
        public int     getMessageCode() { return messageCode; }
        public String  getResult()      { return result; }
        public String  getTestedDate()  { return testedDate; }
        public String  getMaskUsed()    { return maskUsed; }

        @Override
        public String toString() {
            return String.format("%04d Mesg Code:%04d %s TstDate:%-10s Mask used:%-10s",
                    severityCode, messageCode, pad(result, 15), testedDate, maskUsed);
        }

        private static String pad(String s, int len) {
            if (s == null) s = "";
            return s.length() >= len ? s.substring(0, len) : s + " ".repeat(len - s.length());
        }
    }

    // ---------------------------------------------------------------------------
    // Primary entry-point: CSUTLDTC USING LS-DATE, LS-DATE-FORMAT, LS-RESULT
    // ---------------------------------------------------------------------------

    /**
     * Validates {@code dateStr} against {@code formatMask}.
     *
     * <p>Mirrors the PROCEDURE DIVISION entry point of CSUTLDTC.cbl.
     *
     * @param dateStr    date string to validate (up to 10 chars), e.g. "20231231"
     * @param formatMask COBOL picture mask, e.g. "YYYYMMDD", "YYYY-MM-DD",
     *                   "MM/DD/YYYY", "MMDDYYYY", "DD/MM/YYYY"
     * @return {@link DateValidationResult} – severityCode 0 means valid
     */
    public DateValidationResult validateDate(String dateStr, String formatMask) {
        DateValidationResult r = new DateValidationResult();
        r.testedDate = dateStr == null ? "" : dateStr.trim();
        r.maskUsed   = formatMask == null ? "" : formatMask.trim();

        if (dateStr == null || dateStr.isBlank()) {
            r.severityCode = 3;
            r.messageCode  = 0x09CB;  // FC-INSUFFICIENT-DATA equivalent
            r.result       = "Insufficient";
            return r;
        }

        DateTimeFormatter formatter = resolveFormatter(formatMask);
        if (formatter == null) {
            r.severityCode = 3;
            r.messageCode  = 0x09D6;  // FC-BAD-PIC-STRING
            r.result       = "Bad Pic String";
            return r;
        }

        // Non-numeric guard for purely numeric formats
        if (isNumericFormat(formatMask) && !isNumeric(dateStr.trim())) {
            r.severityCode = 3;
            r.messageCode  = 0x09D8;  // FC-NON-NUMERIC-DATA
            r.result       = "Nonnumeric data";
            return r;
        }

        try {
            LocalDate parsed = LocalDate.parse(dateStr.trim(), formatter);

            // Detect when parsing silently adjusted the date (e.g. Feb 30 → Mar 2)
            // by formatting back and comparing
            String reformatted = parsed.format(formatter);
            if (!reformatted.equalsIgnoreCase(dateStr.trim())) {
                r.severityCode = 3;
                r.messageCode  = 0x09CC;  // FC-BAD-DATE-VALUE
                r.result       = "Datevalue error";
                return r;
            }

            // Century check (COBOL: THIS-CENTURY=20, LAST-CENTURY=19)
            int century = parsed.getYear() / 100;
            if (!VALID_CENTURIES.contains(century)) {
                r.severityCode = 3;
                r.messageCode  = 0x09CD;  // FC-INVALID-ERA
                r.result       = "Invalid Era";
                return r;
            }

            // Year-in-era zero check (year component == 0 means era-year 0)
            if (parsed.getYear() == 0) {
                r.severityCode = 3;
                r.messageCode  = 0x09D9;  // FC-YEAR-IN-ERA-ZERO
                r.result       = "YearInEra is 0";
                return r;
            }

            // All good – FC-INVALID-DATE value == all-zero = "Date is valid"
            r.severityCode = 0;
            r.messageCode  = 0;
            r.result       = "Date is valid";
        } catch (DateTimeParseException e) {
            r.severityCode = 3;
            r.messageCode  = 0x09CC;  // FC-BAD-DATE-VALUE
            r.result       = "Datevalue error";
        }

        return r;
    }

    // ---------------------------------------------------------------------------
    // Date arithmetic (COBOL INTEGER-OF-DATE / CEEDAYS Lillian)
    // ---------------------------------------------------------------------------

    /**
     * Returns the number of days between two dates.
     *
     * <p>COBOL equivalent:
     * <pre>
     *   COMPUTE WS-EDIT-DATE-BINARY   = FUNCTION INTEGER-OF-DATE(date1)
     *   COMPUTE WS-CURRENT-DATE-BINARY = FUNCTION INTEGER-OF-DATE(date2)
     *   COMPUTE DIFF = WS-CURRENT-DATE-BINARY - WS-EDIT-DATE-BINARY
     * </pre>
     *
     * @param from start date in YYYYMMDD format
     * @param to   end date   in YYYYMMDD format
     * @return BigDecimal day-count (COMP-3 compatible precision)
     */
    public BigDecimal daysBetween(String from, String to) {
        LocalDate fromDate = LocalDate.parse(from.trim(), FMT_YYYYMMDD);
        LocalDate toDate   = LocalDate.parse(to.trim(),   FMT_YYYYMMDD);
        return BigDecimal.valueOf(ChronoUnit.DAYS.between(fromDate, toDate));
    }

    /**
     * Returns the "integer of date" (days since epoch) for a YYYYMMDD string.
     * Replaces COBOL FUNCTION INTEGER-OF-DATE.
     *
     * <p>COMP-3 equivalent: returned as {@link BigDecimal}.
     */
    public BigDecimal integerOfDate(String yyyymmdd) {
        LocalDate d = LocalDate.parse(yyyymmdd.trim(), FMT_YYYYMMDD);
        return BigDecimal.valueOf(d.toEpochDay());
    }

    /**
     * Checks whether a date-of-birth (YYYYMMDD) is in the past.
     *
     * <p>COBOL: EDIT-DATE-OF-BIRTH paragraph in CSUTLDPY.cpy –
     * {@code FUNCTION INTEGER-OF-DATE(today) > INTEGER-OF-DATE(dob)}.
     *
     * @return true if dob is strictly before today
     */
    public boolean isDateOfBirthValid(String dobYyyyMmDd) {
        LocalDate dob   = LocalDate.parse(dobYyyyMmDd.trim(), FMT_YYYYMMDD);
        LocalDate today = LocalDate.now();
        return dob.isBefore(today);
    }

    // ---------------------------------------------------------------------------
    // Date-format conversions (CODATECN.cpy)
    // ---------------------------------------------------------------------------

    /**
     * Converts YYYYMMDD → YYYY-MM-DD.
     * COBOL: CODATECN-TYPE "1" (YYYYMMDD-IN) → output type "1" (YYYY-MM-DD-OP).
     */
    public String yyyymmddToHyphenated(String yyyymmdd) {
        LocalDate d = LocalDate.parse(yyyymmdd.trim(), FMT_YYYYMMDD);
        return d.format(FMT_YYYY_MM_DD);
    }

    /**
     * Converts YYYY-MM-DD → YYYYMMDD.
     * COBOL: CODATECN-TYPE "2" (YYYY-MM-DD-IN) → output type "2" (YYYYMMDD-OP).
     */
    public String hyphenatedToYyyymmdd(String hyphenated) {
        LocalDate d = LocalDate.parse(hyphenated.trim(), FMT_YYYY_MM_DD);
        return d.format(FMT_YYYYMMDD);
    }

    /**
     * Converts YYYYMMDD → MM/DD/YYYY (CICS FORMATTIME MMDDYYYY with slash separator).
     * Used by CODATE01.cbl EXEC CICS FORMATTIME … MMDDYYYY DATESEP('/').
     */
    public String yyyymmddToMmDdYyyy(String yyyymmdd) {
        LocalDate d = LocalDate.parse(yyyymmdd.trim(), FMT_YYYYMMDD);
        return d.format(FMT_MM_DD_YYYY);
    }

    /**
     * Converts MM/DD/YYYY → YYYYMMDD.
     */
    public String mmDdYyyyToYyyymmdd(String mmDdYyyy) {
        LocalDate d = LocalDate.parse(mmDdYyyy.trim(), FMT_MM_DD_YYYY);
        return d.format(FMT_YYYYMMDD);
    }

    /**
     * Returns today's date as YYYYMMDD string.
     * COBOL: MOVE FUNCTION CURRENT-DATE TO WS-CURRENT-DATE-YYYYMMDD.
     */
    public String currentDateYyyymmdd() {
        return LocalDate.now().format(FMT_YYYYMMDD);
    }

    /**
     * Returns today's date as MM-DD-YYYY string (CICS FORMATTIME style).
     */
    public String currentDateMmDdYyyy() {
        return LocalDate.now().format(FMT_MM_DD_YYYY);
    }

    // ---------------------------------------------------------------------------
    // Date component validation helpers (CSUTLDWY.cpy / CSUTLDPY.cpy)
    // ---------------------------------------------------------------------------

    /**
     * Validates a year string (CCYY).
     * COBOL: EDIT-YEAR-CCYY paragraph – checks numeric, century 19 or 20.
     */
    public boolean isValidYear(String ccyy) {
        if (ccyy == null || ccyy.isBlank()) return false;
        if (!isNumeric(ccyy.trim()))         return false;
        int year    = Integer.parseInt(ccyy.trim());
        int century = year / 100;
        return VALID_CENTURIES.contains(century);
    }

    /**
     * Validates a month string (MM).
     * COBOL: EDIT-MONTH paragraph – value 1 through 12.
     */
    public boolean isValidMonth(String mm) {
        if (mm == null || mm.isBlank()) return false;
        if (!isNumeric(mm.trim()))       return false;
        int m = Integer.parseInt(mm.trim());
        return m >= 1 && m <= 12;
    }

    /**
     * Validates a day string (DD) independently.
     * COBOL: EDIT-DAY paragraph – value 1 through 31.
     */
    public boolean isValidDay(String dd) {
        if (dd == null || dd.isBlank()) return false;
        if (!isNumeric(dd.trim()))       return false;
        int d = Integer.parseInt(dd.trim());
        return d >= 1 && d <= 31;
    }

    /**
     * Validates a complete YYYYMMDD date including month/day/leap-year rules.
     * COBOL: EDIT-DATE-CCYYMMDD + EDIT-DAY-MONTH-YEAR paragraphs.
     *
     * @return empty string if valid, otherwise an error message
     *         (mirrors WS-RETURN-MSG content)
     */
    public String validateDateComponents(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.isBlank() || yyyymmdd.length() < 8)
            return "Date must be supplied.";

        String ccyy = yyyymmdd.substring(0, 4);
        String mm   = yyyymmdd.substring(4, 6);
        String dd   = yyyymmdd.substring(6, 8);

        if (!isValidYear(ccyy))  return "Year is not valid.";
        if (!isValidMonth(mm))   return "Month must be a number between 1 and 12.";
        if (!isValidDay(dd))     return "Day must be a number between 1 and 31.";

        int month = Integer.parseInt(mm);
        int day   = Integer.parseInt(dd);
        int year  = Integer.parseInt(ccyy);

        // 31-day month check (WS-31-DAY-MONTH)
        if (!MONTHS_31.contains(month) && day == 31)
            return "Cannot have 31 days in this month.";

        // February checks
        if (month == 2) {
            if (day == 30)
                return "Cannot have 30 days in February.";
            if (day == 29) {
                boolean leapYear = (year % 400 == 0) || (year % 4 == 0 && year % 100 != 0);
                if (!leapYear)
                    return "Not a leap year. Cannot have 29 days in February.";
            }
        }
        return ""; // valid
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private DateTimeFormatter resolveFormatter(String mask) {
        if (mask == null) return null;
        return switch (mask.trim().toUpperCase()) {
            case "YYYYMMDD"   -> FMT_YYYYMMDD;
            case "YYYY-MM-DD" -> FMT_YYYY_MM_DD;
            case "MM/DD/YYYY" -> FMT_MM_DD_YYYY;
            case "MMDDYYYY"   -> FMT_MMDDYYYY;
            case "DD/MM/YYYY" -> FMT_DD_MM_YYYY;
            default           -> null;
        };
    }

    private boolean isNumericFormat(String mask) {
        if (mask == null) return false;
        return switch (mask.trim().toUpperCase()) {
            case "YYYYMMDD", "MMDDYYYY" -> true;
            default -> false;
        };
    }

    private boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        for (char c : s.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }
}
