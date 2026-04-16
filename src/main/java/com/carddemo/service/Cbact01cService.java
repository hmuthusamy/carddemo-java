package com.carddemo.service;

import com.carddemo.model.AccountData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Cbact01cService – extracted business logic from COBOL program CBACT01C.CBL.
 *
 * <p>The original COBOL program:
 * <ol>
 *   <li>Iterates VSAM KSDS account file (ACCTFILE) sequentially.</li>
 *   <li>Displays each account record to SYSOUT (1100-DISPLAY-ACCT-RECORD).</li>
 *   <li>Populates and writes a flat output record (OUT-FILE) with account
 *       fields plus a conditional default for ACCT-CURR-CYC-DEBIT
 *       (1300-POPUL-ACCT-RECORD / 1350-WRITE-ACCT-RECORD).</li>
 *   <li>Calls assembler routine COBDATFT to reformat ACCT-REISSUE-DATE.</li>
 *   <li>Populates an array record (ARRY-FILE) with current balance and
 *       hard-coded debit amounts for 3 of 5 array slots
 *       (1400-POPUL-ARRAY-RECORD / 1450-WRITE-ARRY-RECORD).</li>
 *   <li>Populates two variable-length output records (VBRC-FILE):
 *       VBR-REC1 (12 bytes: acct-id + active-status) and
 *       VBR-REC2 (39 bytes: acct-id + curr-bal + credit-limit + reissue-yyyy)
 *       (1500-POPUL-VBRC-RECORD / 1550 / 1575).</li>
 * </ol>
 *
 * <p>COMP-3 (packed-decimal) fields use {@link BigDecimal} with
 * {@link RoundingMode#HALF_UP} to match COBOL rounding semantics.
 *
 * <p>Date reformatting by assembler COBDATFT (type=2, outtype=2) converts
 * the reissue date to an ISO-style YYYY-MM-DD representation; this is
 * replicated using {@link DateTimeFormatter}.
 */
@Slf4j
@Service
public class Cbact01cService {

    // ---------------------------------------------------------------
    // Constants matching COBOL hard-coded values
    // ---------------------------------------------------------------

    /**
     * Default value for OUT-ACCT-CURR-CYC-DEBIT when ACCT-CURR-CYC-DEBIT = 0.
     * COBOL: IF ACCT-CURR-CYC-DEBIT EQUAL TO ZERO MOVE 2525.00 TO OUT-ACCT-CURR-CYC-DEBIT
     */
    public static final BigDecimal DEFAULT_CYC_DEBIT = new BigDecimal("2525.00");

    /**
     * Hard-coded debit for array slot 1.
     * COBOL: MOVE 1005.00 TO ARR-ACCT-CURR-CYC-DEBIT(1)
     */
    public static final BigDecimal ARRAY_SLOT1_DEBIT = new BigDecimal("1005.00");

    /**
     * Hard-coded debit for array slot 2.
     * COBOL: MOVE 1525.00 TO ARR-ACCT-CURR-CYC-DEBIT(2)
     */
    public static final BigDecimal ARRAY_SLOT2_DEBIT = new BigDecimal("1525.00");

    /**
     * Hard-coded balance for array slot 3.
     * COBOL: MOVE -1025.00 TO ARR-ACCT-CURR-BAL(3)
     */
    public static final BigDecimal ARRAY_SLOT3_BAL = new BigDecimal("-1025.00");

    /**
     * Hard-coded debit for array slot 3.
     * COBOL: MOVE -2500.00 TO ARR-ACCT-CURR-CYC-DEBIT(3)
     */
    public static final BigDecimal ARRAY_SLOT3_DEBIT = new BigDecimal("-2500.00");

    // Date formatters (mirrors COBDATFT type=2/outtype=2 → ISO date)
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter[] PARSEABLE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    };

    // ---------------------------------------------------------------
    // 1100-DISPLAY-ACCT-RECORD  (DISPLAY statements → SLF4J log)
    // ---------------------------------------------------------------

    /**
     * Replicates 1100-DISPLAY-ACCT-RECORD: logs all account fields to output.
     * COBOL used DISPLAY to write to SYSOUT; here we use SLF4J INFO.
     */
    public void displayAccountRecord(AccountData account) {
        log.info("ACCT-ID                 : {}", formatAcctId(account.getAcctId()));
        log.info("ACCT-ACTIVE-STATUS      : {}", account.getActiveStatus());
        log.info("ACCT-CURR-BAL           : {}", formatDecimal(account.getCurrBal()));
        log.info("ACCT-CREDIT-LIMIT       : {}", formatDecimal(account.getCreditLimit()));
        log.info("ACCT-CASH-CREDIT-LIMIT  : {}", formatDecimal(account.getCashCreditLimit()));
        log.info("ACCT-OPEN-DATE          : {}", account.getOpenDate());
        log.info("ACCT-EXPIRAION-DATE     : {}", account.getExpirationDate()); // typo preserved
        log.info("ACCT-REISSUE-DATE       : {}", account.getReissueDate());
        log.info("ACCT-CURR-CYC-CREDIT    : {}", formatDecimal(account.getCurrCycCredit()));
        log.info("ACCT-CURR-CYC-DEBIT     : {}", formatDecimal(account.getCurrCycDebit()));
        log.info("ACCT-GROUP-ID           : {}", account.getGroupId());
        log.info("-------------------------------------------------");
    }

    // ---------------------------------------------------------------
    // 1300-POPUL-ACCT-RECORD  (build the OUT-FILE record)
    // ---------------------------------------------------------------

    /**
     * Replicates 1300-POPUL-ACCT-RECORD.
     *
     * <p>Key business rules:
     * <ul>
     *   <li>Calls COBDATFT to reformat reissueDate (type=2,outtype=2 → ISO).</li>
     *   <li>If currCycDebit == 0, defaults to 2525.00 (COMP-3 field default).</li>
     * </ul>
     *
     * @param account source account record
     * @return populated {@link AccountOutputRecord} for the OUT-FILE
     */
    public AccountOutputRecord populateOutputRecord(AccountData account) {
        AccountOutputRecord out = new AccountOutputRecord();

        // Direct field moves
        out.setAcctId(account.getAcctId());
        out.setActiveStatus(account.getActiveStatus());
        out.setCurrBal(scaleComp3(account.getCurrBal()));
        out.setCreditLimit(scaleComp3(account.getCreditLimit()));
        out.setCashCreditLimit(scaleComp3(account.getCashCreditLimit()));
        out.setOpenDate(account.getOpenDate());
        out.setExpirationDate(account.getExpirationDate()); // sic – typo preserved from COBOL

        // COBDATFT date reformatting: MOVE ACCT-REISSUE-DATE TO CODATECN-INP-DATE
        //   MOVE '2' TO CODATECN-TYPE / MOVE '2' TO CODATECN-OUTTYPE
        //   CALL 'COBDATFT' → MOVE CODATECN-0UT-DATE TO OUT-ACCT-REISSUE-DATE
        out.setReissueDate(reformatDate(account.getReissueDate()));

        out.setCurrCycCredit(scaleComp3(account.getCurrCycCredit()));

        // IF ACCT-CURR-CYC-DEBIT EQUAL TO ZERO
        //     MOVE 2525.00 TO OUT-ACCT-CURR-CYC-DEBIT
        // END-IF
        if (account.getCurrCycDebit() == null ||
                account.getCurrCycDebit().compareTo(BigDecimal.ZERO) == 0) {
            out.setCurrCycDebit(DEFAULT_CYC_DEBIT);
        } else {
            out.setCurrCycDebit(scaleComp3(account.getCurrCycDebit()));
        }

        out.setGroupId(account.getGroupId());
        return out;
    }

    // ---------------------------------------------------------------
    // 1400-POPUL-ARRAY-RECORD
    // ---------------------------------------------------------------

    /**
     * Replicates 1400-POPUL-ARRAY-RECORD.
     *
     * <p>The COBOL ARRY-FILE record has 5 array slots (OCCURS 5 TIMES).
     * Only slots 1–3 are populated; slots 4–5 remain zero (INITIALIZE).
     *
     * <pre>
     * MOVE ACCT-ID         TO ARR-ACCT-ID
     * MOVE ACCT-CURR-BAL   TO ARR-ACCT-CURR-BAL(1)
     * MOVE 1005.00         TO ARR-ACCT-CURR-CYC-DEBIT(1)
     * MOVE ACCT-CURR-BAL   TO ARR-ACCT-CURR-BAL(2)
     * MOVE 1525.00         TO ARR-ACCT-CURR-CYC-DEBIT(2)
     * MOVE -1025.00        TO ARR-ACCT-CURR-BAL(3)
     * MOVE -2500.00        TO ARR-ACCT-CURR-CYC-DEBIT(3)
     * </pre>
     */
    public AccountArrayRecord populateArrayRecord(AccountData account) {
        AccountArrayRecord arr = new AccountArrayRecord();
        arr.setAcctId(account.getAcctId());

        // Slot 1
        arr.getBals()[0] = scaleComp3(account.getCurrBal());
        arr.getDebits()[0] = ARRAY_SLOT1_DEBIT;

        // Slot 2
        arr.getBals()[1] = scaleComp3(account.getCurrBal());
        arr.getDebits()[1] = ARRAY_SLOT2_DEBIT;

        // Slot 3 – hard-coded values
        arr.getBals()[2] = ARRAY_SLOT3_BAL;
        arr.getDebits()[2] = ARRAY_SLOT3_DEBIT;

        // Slots 4 & 5 remain ZERO (INITIALIZE in COBOL)
        arr.getBals()[3] = BigDecimal.ZERO;
        arr.getDebits()[3] = BigDecimal.ZERO;
        arr.getBals()[4] = BigDecimal.ZERO;
        arr.getDebits()[4] = BigDecimal.ZERO;

        return arr;
    }

    // ---------------------------------------------------------------
    // 1500-POPUL-VBRC-RECORD
    // ---------------------------------------------------------------

    /**
     * Replicates 1500-POPUL-VBRC-RECORD.
     *
     * <p>VBRC-REC1 (12 bytes variable): acct-id (11) + active-status (1)
     * <p>VBRC-REC2 (39 bytes variable): acct-id (11) + curr-bal (12) +
     *   credit-limit (12) + reissue-yyyy (4)
     *
     * Returns a {@link VbrcRecord} containing both sub-records plus
     * their DISPLAY output (mirrors COBOL DISPLAY statements).
     */
    public VbrcRecord populateVbrcRecord(AccountData account, String reformattedReissueDate) {
        VbrcRecord vbrc = new VbrcRecord();

        // VBR-REC1 fields
        vbrc.setVb1AcctId(account.getAcctId());
        vbrc.setVb1ActiveStatus(account.getActiveStatus());

        // VBR-REC2 fields
        vbrc.setVb2AcctId(account.getAcctId());
        vbrc.setVb2CurrBal(scaleComp3(account.getCurrBal()));
        vbrc.setVb2CreditLimit(scaleComp3(account.getCreditLimit()));

        // WS-ACCT-REISSUE-YYYY is first 4 chars of the (possibly reformatted) date
        String yyyy = extractYear(reformattedReissueDate != null
                ? reformattedReissueDate
                : account.getReissueDate());
        vbrc.setVb2ReissueYyyy(yyyy);

        // DISPLAY 'VBRC-REC1:' / 'VBRC-REC2:' → log
        log.info("VBRC-REC1: {}{}", formatAcctId(vbrc.getVb1AcctId()), vbrc.getVb1ActiveStatus());
        log.info("VBRC-REC2: {}{}{}{}", formatAcctId(vbrc.getVb2AcctId()),
                formatDecimal(vbrc.getVb2CurrBal()),
                formatDecimal(vbrc.getVb2CreditLimit()),
                vbrc.getVb2ReissueYyyy());

        return vbrc;
    }

    // ---------------------------------------------------------------
    // Date reformatting – replaces assembler COBDATFT (type=2,outtype=2)
    // ---------------------------------------------------------------

    /**
     * Replicates the COBDATFT assembler call with TYPE=2 / OUTTYPE=2.
     * Attempts to parse common date string formats and returns ISO (yyyy-MM-dd).
     * If unparseable, returns the original string unchanged.
     */
    public String reformatDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return rawDate;
        }
        String trimmed = rawDate.trim();
        for (DateTimeFormatter fmt : PARSEABLE_FORMATS) {
            try {
                LocalDate date = LocalDate.parse(trimmed, fmt);
                return date.format(ISO_DATE);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        // Return as-is if no format matched (graceful degradation)
        log.warn("COBDATFT: unable to reformat date '{}'; returning as-is", rawDate);
        return rawDate;
    }

    // ---------------------------------------------------------------
    // Helper / formatting utilities
    // ---------------------------------------------------------------

    /**
     * Scales a BigDecimal to 2 decimal places with HALF_UP rounding,
     * matching COBOL COMP-3 (PIC S9(10)V99) precision.
     */
    public BigDecimal scaleComp3(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** Format account ID as zero-padded 11-digit string (PIC 9(11)). */
    public String formatAcctId(Long acctId) {
        if (acctId == null) return "00000000000";
        return String.format("%011d", acctId);
    }

    /** Format BigDecimal for DISPLAY output (COBOL numeric picture). */
    public String formatDecimal(BigDecimal value) {
        if (value == null) return "+0000000000.00";
        String sign = value.signum() < 0 ? "-" : "+";
        BigDecimal abs = value.abs().setScale(2, RoundingMode.HALF_UP);
        String[] parts = abs.toPlainString().split("\\.");
        String intPart = String.format("%010d", Long.parseLong(parts[0]));
        return sign + intPart + "." + parts[1];
    }

    /** Extract 4-digit year from a date string (first 4 chars). */
    private String extractYear(String date) {
        if (date == null || date.length() < 4) return "    ";
        return date.substring(0, 4);
    }

    // ---------------------------------------------------------------
    // Inner value-object classes (mirrors COBOL FD record layouts)
    // ---------------------------------------------------------------

    /**
     * OUT-FILE record (FD OUT-FILE / 01 OUT-ACCT-REC).
     * All numeric fields use BigDecimal to preserve COMP-3 precision.
     */
    @lombok.Data
    public static class AccountOutputRecord {
        /** OUT-ACCT-ID PIC 9(11) */
        private Long acctId;
        /** OUT-ACCT-ACTIVE-STATUS PIC X(01) */
        private String activeStatus;
        /** OUT-ACCT-CURR-BAL PIC S9(10)V99 */
        private BigDecimal currBal;
        /** OUT-ACCT-CREDIT-LIMIT PIC S9(10)V99 */
        private BigDecimal creditLimit;
        /** OUT-ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 */
        private BigDecimal cashCreditLimit;
        /** OUT-ACCT-OPEN-DATE PIC X(10) */
        private String openDate;
        /** OUT-ACCT-EXPIRAION-DATE PIC X(10) – typo preserved from COBOL FD */
        private String expirationDate;
        /** OUT-ACCT-REISSUE-DATE PIC X(10) – reformatted by COBDATFT */
        private String reissueDate;
        /** OUT-ACCT-CURR-CYC-CREDIT PIC S9(10)V99 */
        private BigDecimal currCycCredit;
        /** OUT-ACCT-CURR-CYC-DEBIT PIC S9(10)V99 USAGE IS COMP-3 */
        private BigDecimal currCycDebit;
        /** OUT-ACCT-GROUP-ID PIC X(10) */
        private String groupId;
    }

    /**
     * ARRY-FILE record (FD ARRY-FILE / 01 ARR-ARRAY-REC).
     * ARR-ACCT-BAL OCCURS 5 TIMES with two sub-fields each.
     */
    @lombok.Data
    public static class AccountArrayRecord {
        /** ARR-ACCT-ID PIC 9(11) */
        private Long acctId;
        /** ARR-ACCT-CURR-BAL(1..5) PIC S9(10)V99 */
        private BigDecimal[] bals = new BigDecimal[5];
        /** ARR-ACCT-CURR-CYC-DEBIT(1..5) PIC S9(10)V99 COMP-3 */
        private BigDecimal[] debits = new BigDecimal[5];
    }

    /**
     * VBRC-FILE record (variable-length, FD VBRC-FILE RECORDING MODE IS V).
     * Combines VBR-REC1 (12 bytes) and VBR-REC2 (39 bytes).
     */
    @lombok.Data
    public static class VbrcRecord {
        // VBR-REC1 (WS-RECD-LEN = 12)
        /** VB1-ACCT-ID PIC 9(11) */
        private Long vb1AcctId;
        /** VB1-ACCT-ACTIVE-STATUS PIC X(01) */
        private String vb1ActiveStatus;

        // VBR-REC2 (WS-RECD-LEN = 39)
        /** VB2-ACCT-ID PIC 9(11) */
        private Long vb2AcctId;
        /** VB2-ACCT-CURR-BAL PIC S9(10)V99 */
        private BigDecimal vb2CurrBal;
        /** VB2-ACCT-CREDIT-LIMIT PIC S9(10)V99 */
        private BigDecimal vb2CreditLimit;
        /** VB2-ACCT-REISSUE-YYYY PIC X(04) */
        private String vb2ReissueYyyy;
    }
}
