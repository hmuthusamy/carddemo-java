package com.carddemo.service;

import com.carddemo.exception.ApplicationException;
import org.springframework.stereotype.Service;

/**
 * MessageService - Java equivalent of COBOL message/error handling defined in:
 * CSMSG01Y.cpy (common display messages), CSMSG02Y.cpy (abend/error work areas),
 * CSUTLDTC.cbl (CEEDAYS feedback code translations).
 */
@Service
public class MessageService {

    public enum ErrorCode {
        SUCCESS             (0,  "0000", "Operation completed successfully"),
        MSG_THANK_YOU       (0,  "0001", "Thank you for using CardDemo application..."),
        MSG_INVALID_KEY     (0,  "0002", "Invalid key pressed. Please see below..."),
        DATE_VALID          (0,  "0000", "Date is valid"),
        DATE_INSUFFICIENT   (3,  "09CB", "Insufficient data supplied for date"),
        DATE_BAD_VALUE      (3,  "09CC", "Date value error - date does not exist"),
        DATE_INVALID_ERA    (3,  "09CD", "Invalid era in date - century not 19 or 20"),
        DATE_UNSUPP_RANGE   (3,  "09D1", "Unsupported date range"),
        DATE_INVALID_MONTH  (3,  "09D5", "Invalid month - must be 01-12"),
        DATE_BAD_PIC_STRING (3,  "09D6", "Bad picture string / format mask"),
        DATE_NON_NUMERIC    (3,  "09D8", "Non-numeric data in date field"),
        DATE_ERA_YEAR_ZERO  (3,  "09D9", "Year-in-era is zero"),
        DATE_INVALID        (3,  "09FF", "Date is invalid"),
        DATE_YEAR_BLANK     (4,  "D001", "Year field is blank / not supplied"),
        DATE_YEAR_NOT_OK    (4,  "D002", "Year value is not valid"),
        DATE_MONTH_BLANK    (4,  "D003", "Month field is blank / not supplied"),
        DATE_MONTH_NOT_OK   (4,  "D004", "Month value is not valid"),
        DATE_DAY_BLANK      (4,  "D005", "Day field is blank / not supplied"),
        DATE_DAY_NOT_OK     (4,  "D006", "Day value is not valid"),
        DATE_FUTURE_DOB     (4,  "D007", "Date of birth cannot be in the future"),
        DATE_NOT_LEAP_YEAR  (4,  "D008", "Not a leap year - cannot have 29 days in Feb"),
        ABEND_CICS          (12, "A001", "CICS abend - unexpected response code"),
        ABEND_FILE_IO       (12, "A002", "File I/O abend - VSAM file error"),
        ABEND_DB_ERROR      (12, "A003", "Database abend - unexpected DB2/SQL error"),
        ABEND_MQ_ERROR      (12, "A004", "MQ abend - queue operation failed"),
        ABEND_GENERAL       (12, "A999", "General abend - program logic error"),
        ACCT_NOT_FOUND      (8,  "B001", "Account not found"),
        ACCT_INVALID_STATUS (8,  "B002", "Account has an invalid status"),
        CARD_NOT_FOUND      (8,  "B003", "Card record not found"),
        CARD_INACTIVE       (8,  "B004", "Card is inactive"),
        CUST_NOT_FOUND      (8,  "B005", "Customer record not found"),
        TXN_DUPLICATE       (8,  "B006", "Duplicate transaction detected"),
        TXN_INVALID_AMOUNT  (8,  "B007", "Transaction amount is invalid"),
        USER_NOT_FOUND      (8,  "B008", "User not found"),
        USER_INVALID_PWD    (8,  "B009", "Invalid password"),
        AUTH_FAILED         (8,  "B010", "Authorisation failed"),
        INPUT_REQUIRED      (4,  "V001", "Required field is missing"),
        INPUT_NOT_NUMERIC   (4,  "V002", "Field must be numeric"),
        INPUT_TOO_LONG      (4,  "V003", "Input exceeds maximum field length"),
        INPUT_INVALID_CHARS (4,  "V004", "Input contains invalid characters");

        private final int    severityCode;
        private final String messageCode;
        private final String description;

        ErrorCode(int severityCode, String messageCode, String description) {
            this.severityCode = severityCode;
            this.messageCode  = messageCode;
            this.description  = description;
        }

        public int    getSeverityCode() { return severityCode; }
        public String getMessageCode()  { return messageCode;  }
        public String getDescription()  { return description;  }
        /** Returns true when severity is 0 (no error). */
        public boolean isOk()    { return severityCode == 0; }
        /** Returns true when severity >= 12 (hard abend). */
        public boolean isAbend() { return severityCode >= 12; }
        /** Returns true when severity >= 4 (warning or error). */
        public boolean isError() { return severityCode >= 4; }
    }

    /**
     * CCDA-MSG-THANK-YOU from CSMSG01Y.cpy – PIC X(50).
     * COBOL VALUE 'Thank you for using CardDemo application...      '
     * = 43 printable chars right-padded to 50.
     */
    public static final String MSG_THANK_YOU =
            String.format("%-50s", "Thank you for using CardDemo application...");

    /**
     * CCDA-MSG-INVALID-KEY from CSMSG01Y.cpy – PIC X(50).
     * COBOL VALUE 'Invalid key pressed. Please see below...         '
     * = 41 printable chars right-padded to 50.
     */
    public static final String MSG_INVALID_KEY =
            String.format("%-50s", "Invalid key pressed. Please see below...");

    /**
     * Builds a structured 80-character message mirroring WS-MESSAGE from CSUTLDTC.cbl.
     * Layout: severity(4)|filler(11)|msgno(4)|space(1)|result(15)|space(1)|
     *         TstDate(9)|date(10)|space(1)|MaskUsed(10)|fmt(10)|space(1)|spaces(3) = 80
     */
    public String buildDateValidationMessage(ErrorCode errorCode,
                                             String testedDate,
                                             String maskUsed) {
        return String.format("%-4d%-11s%-4s %-15s %-9s%-10s %-10s%-10s    ",
                errorCode.getSeverityCode(),
                "Mesg Code:",
                errorCode.getMessageCode(),
                errorCode.getDescription(),
                "TstDate:",
                pad(testedDate, 10),
                "Mask used:",
                pad(maskUsed, 10));
    }

    /**
     * Builds a field-level error message.
     * COBOL: STRING variable-name ' : ' description DELIMITED BY SIZE INTO WS-RETURN-MSG.
     */
    public String buildFieldErrorMessage(String fieldName, ErrorCode errorCode) {
        String msg = String.format("%s : %s", trim(fieldName), errorCode.getDescription());
        return msg.length() > 80 ? msg.substring(0, 80) : msg;
    }

    /**
     * Builds an abend record mirroring CSMSG02Y.cpy ABEND-DATA:
     * ABEND-CODE(4)|ABEND-CULPRIT(8)|ABEND-REASON(50)|ABEND-MSG(72) = 134
     */
    public AbendData buildAbendData(String abendCode, String culprit,
                                    String reason, String message) {
        return new AbendData(
                pad(abendCode, 4), pad(culprit, 8), pad(reason, 50), pad(message, 72));
    }

    /** Throws ApplicationException – equivalent of PERFORM 9000-ABEND with RETURN-CODE. */
    public void raise(ErrorCode errorCode, String context) {
        throw new ApplicationException(errorCode, context);
    }

    /** Convenience overload – no context. */
    public void raise(ErrorCode errorCode) {
        raise(errorCode, "");
    }

    /** Mirrors ABEND-DATA group in CSMSG02Y.cpy. */
    public record AbendData(String abendCode, String culprit, String reason, String message) {
        @Override public String toString() { return abendCode + culprit + reason + message; }
    }

    /** Reverse-lookup ErrorCode by COBOL message code string. */
    public ErrorCode fromMessageCode(String messageCode) {
        if (messageCode == null) return ErrorCode.DATE_INVALID;
        for (ErrorCode ec : ErrorCode.values()) {
            if (ec.getMessageCode().equalsIgnoreCase(messageCode.trim())) return ec;
        }
        return ErrorCode.DATE_INVALID;
    }

    /** Returns description label for a CEEDAYS severity/message code pair. */
    public String ceedaysSeverityLabel(int severity, String messageCode) {
        return fromMessageCode(messageCode).getDescription();
    }

    private static String trim(String s) { return s == null ? "" : s.strip(); }

    private static String pad(String s, int len) {
        if (s == null) s = "";
        return s.length() >= len ? s.substring(0, len) : s + " ".repeat(len - s.length());
    }
}
