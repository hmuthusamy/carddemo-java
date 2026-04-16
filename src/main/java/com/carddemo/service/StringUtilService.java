package com.carddemo.service;

import org.springframework.stereotype.Service;

/**
 * StringUtilService – Java equivalent of COBOL string-handling intrinsics and
 * INSPECT / STRING / UNSTRING idioms used throughout the CardDemo programs.
 *
 * <h3>COBOL source patterns covered</h3>
 * <ul>
 *   <li>{@code INSPECT … CONVERTING … } – character-set transliteration</li>
 *   <li>{@code INSPECT … TALLYING … }   – character counting</li>
 *   <li>{@code FUNCTION TRIM(field) }   – leading / trailing space removal</li>
 *   <li>{@code FUNCTION UPPER-CASE / LOWER-CASE} – case conversion</li>
 *   <li>Left / right justification with PIC X(n) space-padding</li>
 *   <li>Zero-padding for numeric strings (COMP-3 → display format)</li>
 *   <li>{@code STRING … DELIMITED BY SIZE / SPACE INTO …}</li>
 *   <li>{@code UNSTRING … DELIMITED BY '/' INTO … }</li>
 * </ul>
 *
 * <p>All COBOL COMP-3 (packed-decimal) numeric parameters that map to string
 * lengths or counts are represented as {@code int} (widths ≤ PIC 9(4)) or
 * {@link java.math.BigDecimal} where financial precision matters.
 */
@Service
public class StringUtilService {

    // ---------------------------------------------------------------------------
    // INSPECT … CONVERTING (character transliteration)
    // ---------------------------------------------------------------------------

    /**
     * Replaces every character in {@code fromChars} with the corresponding
     * character in {@code toChars} – exactly as COBOL
     * {@code INSPECT src CONVERTING fromChars TO toChars}.
     *
     * <p>Example:
     * <pre>
     *   COBOL: INSPECT WS-FIELD CONVERTING
     *            'abcdefghijklmnopqrstuvwxyz'
     *          TO 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
     *   Java:  convertChars(input, ALPHA_LOWER, ALPHA_UPPER)
     * </pre>
     *
     * @param input     source string (may be null – treated as empty)
     * @param fromChars characters to replace
     * @param toChars   replacement characters (same length as {@code fromChars})
     * @return transliterated string
     * @throws IllegalArgumentException if fromChars and toChars have different lengths
     */
    public String convertChars(String input, String fromChars, String toChars) {
        if (input == null || input.isEmpty())  return input == null ? "" : input;
        if (fromChars == null || fromChars.isEmpty()) return input;
        if (fromChars.length() != toChars.length())
            throw new IllegalArgumentException(
                "INSPECT CONVERTING: fromChars and toChars must have the same length");

        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            int idx = fromChars.indexOf(c);
            sb.append(idx >= 0 ? toChars.charAt(idx) : c);
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------------
    // INSPECT … TALLYING (character counting)
    // ---------------------------------------------------------------------------

    /**
     * Counts occurrences of {@code target} in {@code input}.
     * COBOL: {@code INSPECT src TALLYING count FOR ALL target}.
     */
    public int tallyAll(String input, char target) {
        if (input == null) return 0;
        int count = 0;
        for (char c : input.toCharArray()) {
            if (c == target) count++;
        }
        return count;
    }

    /**
     * Counts leading occurrences of {@code target}.
     * COBOL: {@code INSPECT src TALLYING count FOR LEADING target}.
     */
    public int tallyLeading(String input, char target) {
        if (input == null) return 0;
        int count = 0;
        for (char c : input.toCharArray()) {
            if (c == target) count++;
            else break;
        }
        return count;
    }

    /**
     * Counts leading spaces (most frequent INSPECT LEADING SPACES usage).
     */
    public int tallyLeadingSpaces(String input) {
        return tallyLeading(input, ' ');
    }

    // ---------------------------------------------------------------------------
    // FUNCTION TRIM equivalents
    // ---------------------------------------------------------------------------

    /**
     * Removes leading AND trailing spaces.
     * COBOL: {@code FUNCTION TRIM(field)} (default = both ends).
     */
    public String trim(String input) {
        return input == null ? "" : input.strip();
    }

    /**
     * Removes leading spaces only.
     * COBOL: {@code FUNCTION TRIM(field LEADING)}.
     */
    public String trimLeading(String input) {
        return input == null ? "" : input.stripLeading();
    }

    /**
     * Removes trailing spaces only.
     * COBOL: {@code FUNCTION TRIM(field TRAILING)}.
     */
    public String trimTrailing(String input) {
        return input == null ? "" : input.stripTrailing();
    }

    // ---------------------------------------------------------------------------
    // FUNCTION UPPER-CASE / LOWER-CASE
    // ---------------------------------------------------------------------------

    /** COBOL: {@code FUNCTION UPPER-CASE(field)}. */
    public String toUpperCase(String input) {
        return input == null ? "" : input.toUpperCase();
    }

    /** COBOL: {@code FUNCTION LOWER-CASE(field)}. */
    public String toLowerCase(String input) {
        return input == null ? "" : input.toLowerCase();
    }

    // ---------------------------------------------------------------------------
    // Left / right justification and PIC X(n) padding
    // ---------------------------------------------------------------------------

    /**
     * Left-justifies {@code value} in a field of {@code width} characters,
     * padding with spaces on the right.
     *
     * <p>COBOL equivalent: MOVE value TO PIC X(width) (left-justified by default).
     *
     * @param value source string
     * @param width target field width
     * @return padded string of exactly {@code width} characters
     */
    public String leftJustify(String value, int width) {
        if (value == null) value = "";
        if (value.length() >= width) return value.substring(0, width);
        return value + " ".repeat(width - value.length());
    }

    /**
     * Right-justifies {@code value} in a field of {@code width} characters,
     * padding with spaces on the left.
     *
     * <p>COBOL: achieved with INSPECT … TALLYING leading spaces then STRING.
     */
    public String rightJustify(String value, int width) {
        if (value == null) value = "";
        if (value.length() >= width) return value.substring(0, width);
        return " ".repeat(width - value.length()) + value;
    }

    /**
     * Zero-pads a numeric string on the left to {@code width} digits.
     * COBOL: MOVE numeric TO PIC 9(width) effectively zero-fills left.
     */
    public String zeroPadLeft(String numericStr, int width) {
        if (numericStr == null) numericStr = "";
        String stripped = numericStr.stripLeading();
        if (stripped.length() >= width) return stripped.substring(stripped.length() - width);
        return "0".repeat(width - stripped.length()) + stripped;
    }

    // ---------------------------------------------------------------------------
    // STRING … DELIMITED BY SIZE / SPACE INTO …
    // ---------------------------------------------------------------------------

    /**
     * Concatenates parts into a result field of {@code maxLength}, delimited
     * by SIZE (i.e. include full length of each part).
     *
     * <p>COBOL: {@code STRING part1 part2 … DELIMITED BY SIZE INTO target}.
     *
     * @param maxLength total output field length (PIC X(n))
     * @param parts     string parts to concatenate
     * @return concatenated string, truncated / right-space-padded to maxLength
     */
    public String stringInto(int maxLength, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null) sb.append(part);
        }
        return leftJustify(sb.toString(), maxLength);
    }

    /**
     * Concatenates parts into a result field, each part delimited by SPACE
     * (i.e. include only the portion up to the first trailing space in each part).
     *
     * <p>COBOL: {@code STRING part1 part2 … DELIMITED BY SPACE INTO target}.
     */
    public String stringDelimitedBySpace(int maxLength, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null) {
                // Include characters up to (but not including) the first space
                int spaceIdx = part.indexOf(' ');
                sb.append(spaceIdx >= 0 ? part.substring(0, spaceIdx) : part);
            }
        }
        return leftJustify(sb.toString(), maxLength);
    }

    // ---------------------------------------------------------------------------
    // UNSTRING … DELIMITED BY … INTO …
    // ---------------------------------------------------------------------------

    /**
     * Splits {@code input} on {@code delimiter} and returns the token at
     * {@code index} (0-based), space-padded to {@code width}.
     *
     * <p>COBOL: {@code UNSTRING input DELIMITED BY delim INTO token1 token2 …}
     *
     * @param input     source string
     * @param delimiter delimiter character(s)
     * @param index     0-based token index
     * @param width     output field width (PIC X(n))
     * @return token padded to {@code width}, or spaces if index out of range
     */
    public String unstringToken(String input, String delimiter, int index, int width) {
        if (input == null) return " ".repeat(width);
        String[] tokens = input.split(java.util.regex.Pattern.quote(delimiter), -1);
        String token = (index < tokens.length) ? tokens[index] : "";
        return leftJustify(token, width);
    }

    // ---------------------------------------------------------------------------
    // Numeric display / editing
    // ---------------------------------------------------------------------------

    /**
     * Formats a numeric value as an edited PIC Z(n)9 string
     * (leading-zero suppression with space replacement).
     *
     * <p>COBOL: PIC Z(m)9 MOVE … displays leading zeros as spaces.
     *
     * @param value    numeric string (digits only)
     * @param width    total display width
     * @return space-leading-suppressed string of length {@code width}
     */
    public String formatPicZ(String value, int width) {
        String padded = zeroPadLeft(value, width);
        StringBuilder sb = new StringBuilder(padded);
        for (int i = 0; i < sb.length() - 1; i++) {
            if (sb.charAt(i) == '0') sb.setCharAt(i, ' ');
            else break;
        }
        return sb.toString();
    }

    /**
     * Tests if a string is numeric (equivalent to COBOL {@code IS NUMERIC}
     * condition and {@code FUNCTION TEST-NUMVAL}).
     *
     * @return true if {@code s} contains only digits (optionally with leading
     *         / trailing spaces which are stripped first)
     */
    public boolean isNumeric(String s) {
        if (s == null) return false;
        String trimmed = s.strip();
        if (trimmed.isEmpty()) return false;
        for (char c : trimmed.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    /**
     * Checks whether a string consists entirely of spaces or is empty.
     * COBOL: {@code field EQUAL SPACES} condition.
     */
    public boolean isSpaces(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Checks whether a string consists entirely of the supplied character.
     * COBOL: {@code field EQUAL ALL literal} condition.
     */
    public boolean isAllChar(String s, char ch) {
        if (s == null || s.isEmpty()) return false;
        for (char c : s.toCharArray()) {
            if (c != ch) return false;
        }
        return true;
    }

    /**
     * Moves {@code src} into a fixed-width {@code PIC X(n)} field:
     * left-aligns and truncates or right-pads with spaces.
     * COBOL: {@code MOVE src TO dest} where dest is PIC X(n).
     */
    public String moveToField(String src, int fieldWidth) {
        return leftJustify(src == null ? "" : src, fieldWidth);
    }
}
