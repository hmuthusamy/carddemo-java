package com.carddemo.service;

import com.carddemo.model.AccountData;
import com.carddemo.model.AccountUpdateRequest;
import com.carddemo.repository.AccountDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Business-logic service for the CBACT02C account-file batch migration.
 *
 * <h2>COBOL Mapping</h2>
 * The original {@code CBACT02C.CBL} performs a sequential READ of the
 * account VSAM KSDS (indexed by ACCT-ID) and DISPLAYs each record.
 * In the modernised architecture the READ-and-update pattern is expressed
 * here: each {@link AccountUpdateRequest} item (read from a flat file or
 * DB staging table) is validated, looked up by its primary key, and all
 * non-null fields from the request are applied to the persisted entity.
 *
 * <h2>Error Handling (mirrors COBOL ABEND logic)</h2>
 * <ul>
 *   <li>FILE STATUS '00' → {@code APPL-RESULT = 0} → continue (OK)</li>
 *   <li>FILE STATUS '10' → {@code APPL-RESULT = 16} → end-of-file
 *       (handled by batch framework exhaustion)</li>
 *   <li>Any other status → {@code APPL-RESULT = 12} → ABEND →
 *       throw {@link AccountFileProcessingException}</li>
 * </ul>
 *
 * <h2>COMP-3 Arithmetic</h2>
 * All COBOL COMP-3 (packed decimal) fields — {@code ACCT-CURR-BAL},
 * {@code ACCT-CREDIT-LIMIT}, {@code ACCT-CASH-CREDIT-LIMIT},
 * {@code ACCT-CURR-CYC-CREDIT}, {@code ACCT-CURR-CYC-DEBIT} — are
 * represented as {@link BigDecimal} with scale 2 and
 * {@link RoundingMode#HALF_UP}, which is the standard COBOL rounding mode
 * for packed-decimal arithmetic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cbact02cService {

    private final AccountDataRepository accountDataRepository;

    // -----------------------------------------------------------------------
    // Constants mirroring COBOL condition-names (88-levels)
    // -----------------------------------------------------------------------
    /** 88 APPL-AOK VALUE 0 */
    private static final int APPL_AOK = 0;
    /** 88 APPL-EOF VALUE 16 */
    private static final int APPL_EOF = 16;
    /** ABEND / error result code */
    private static final int APPL_ERR = 12;

    /** Scale for all COBOL COMP-3 monetary fields (PIC S9(10)V99 → 2 decimals). */
    private static final int MONETARY_SCALE = 2;

    /** COBOL rounding mode for all COMP-3 arithmetic. */
    private static final RoundingMode COBOL_ROUNDING = RoundingMode.HALF_UP;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Process a single {@link AccountUpdateRequest}.
     *
     * <p>Logic mirrors the COBOL 1000-CARDFILE-GET-NEXT / REWRITE pattern
     * applied to the account VSAM dataset:
     * <ol>
     *   <li>Validate the primary key (ACCT-ID) – ABEND if blank.</li>
     *   <li>Look up the account record by ACCT-ID.</li>
     *   <li>If not found, treat as a new insert (upsert semantics).</li>
     *   <li>Apply field updates following the EVALUATE / IF logic.</li>
     *   <li>Apply COMP-3 arithmetic with HALF_UP rounding.</li>
     *   <li>Persist and return the updated {@link AccountData} entity.</li>
     * </ol>
     *
     * @param request update request derived from input file record
     * @return the persisted/updated {@link AccountData} entity
     * @throws AccountFileProcessingException if mandatory key is absent (ABEND path)
     */
    @Transactional
    public AccountData processAccountUpdate(AccountUpdateRequest request) {
        validateRequest(request);                         // ABEND on bad input

        Optional<AccountData> existing =
                accountDataRepository.findById(request.getAcctId());

        AccountData account = existing.orElseGet(() -> {
            log.info("CBACT02C – new account record, inserting: acctId={}",
                     request.getAcctId());
            return AccountData.builder()
                              .acctId(request.getAcctId())
                              .build();
        });

        applyUpdates(account, request);   // COBOL MOVE / EVALUATE / COMPUTE logic

        AccountData saved = accountDataRepository.save(account);
        log.debug("CBACT02C – saved acctId={} status={}",
                  saved.getAcctId(), saved.getAcctActiveStatus());
        return saved;
    }

    /**
     * Retrieve an account record by account ID.
     * Corresponds to a random-access READ of the account VSAM KSDS.
     *
     * @param acctId account identifier (ACCT-ID PIC 9(11))
     * @return the {@link AccountData} entity
     * @throws AccountFileProcessingException when account not found (APPL-RESULT=12)
     */
    @Transactional(readOnly = true)
    public AccountData getAccount(Long acctId) {
        return accountDataRepository.findById(acctId)
                .orElseThrow(() -> new AccountFileProcessingException(
                        "ERROR READING ACCOUNT FILE – account not found: " + acctId,
                        APPL_ERR));
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Validate that mandatory fields are present.
     *
     * <p>Maps to the COBOL ABEND path (APPL-RESULT = 12) when ACCT-ID is zero/blank.
     * Equivalent to:
     * <pre>
     *   IF ACCT-ID = ZERO
     *       DISPLAY 'ERROR READING ACCOUNT FILE'
     *       PERFORM 9999-ABEND-PROGRAM
     *   END-IF
     * </pre>
     */
    private void validateRequest(AccountUpdateRequest request) {
        if (request == null || request.getAcctId() == null
                || request.getAcctId() <= 0) {
            throw new AccountFileProcessingException(
                    "ERROR READING ACCOUNT FILE – ACCT-ID is required", APPL_ERR);
        }
    }

    /**
     * Apply field-level updates from the request to the {@link AccountData} entity.
     *
     * <h3>COBOL EVALUATE / IF logic preserved</h3>
     * <ul>
     *   <li>Non-null / non-blank values overwrite the existing field
     *       (COBOL: {@code MOVE ws-field TO ACCOUNT-RECORD}).</li>
     *   <li>Null values in the request leave the existing field unchanged
     *       (idempotent partial-update, mirrors COBOL 88-level guard conditions).</li>
     *   <li>{@code ACCT-ACTIVE-STATUS} – EVALUATE: only 'Y' or 'N' accepted;
     *       any other value defaults to 'N' (COBOL WHEN OTHER clause).</li>
     *   <li>All monetary fields are parsed and stored as
     *       {@link BigDecimal} (scale 2, HALF_UP), representing COMP-3
     *       packed-decimal arithmetic.</li>
     * </ul>
     *
     * @param account the JPA entity to update
     * @param request the inbound update request
     */
    private void applyUpdates(AccountData account, AccountUpdateRequest request) {

        // ACCT-ACTIVE-STATUS EVALUATE:
        //   WHEN 'Y'     MOVE 'Y' TO ACCT-ACTIVE-STATUS
        //   WHEN 'N'     MOVE 'N' TO ACCT-ACTIVE-STATUS
        //   WHEN OTHER   MOVE 'N' TO ACCT-ACTIVE-STATUS  (default/INITIALIZE)
        if (request.getAcctActiveStatus() != null) {
            String status = request.getAcctActiveStatus().toUpperCase().trim();
            account.setAcctActiveStatus("Y".equals(status) ? "Y" : "N");
        }

        // ACCT-CURR-BAL – COMP-3 PIC S9(10)V99 → BigDecimal HALF_UP
        if (request.getAcctCurrBal() != null && !request.getAcctCurrBal().isBlank()) {
            account.setAcctCurrBal(parseMonetary(request.getAcctCurrBal(), "ACCT-CURR-BAL"));
        }

        // ACCT-CREDIT-LIMIT – COMP-3 PIC S9(10)V99 → BigDecimal HALF_UP
        if (request.getAcctCreditLimit() != null
                && !request.getAcctCreditLimit().isBlank()) {
            account.setAcctCreditLimit(
                    parseMonetary(request.getAcctCreditLimit(), "ACCT-CREDIT-LIMIT"));
        }

        // ACCT-CASH-CREDIT-LIMIT – COMP-3 PIC S9(10)V99 → BigDecimal HALF_UP
        if (request.getAcctCashCreditLimit() != null
                && !request.getAcctCashCreditLimit().isBlank()) {
            account.setAcctCashCreditLimit(
                    parseMonetary(request.getAcctCashCreditLimit(), "ACCT-CASH-CREDIT-LIMIT"));
        }

        // ACCT-OPEN-DATE PIC X(10)
        if (request.getAcctOpenDate() != null
                && !request.getAcctOpenDate().isBlank()) {
            account.setAcctOpenDate(truncate(request.getAcctOpenDate(), 10));
        }

        // ACCT-EXPIRAION-DATE PIC X(10)  (COBOL spelling preserved)
        if (request.getAcctExpirationDate() != null
                && !request.getAcctExpirationDate().isBlank()) {
            account.setAcctExpirationDate(truncate(request.getAcctExpirationDate(), 10));
        }

        // ACCT-REISSUE-DATE PIC X(10)
        if (request.getAcctReissueDate() != null
                && !request.getAcctReissueDate().isBlank()) {
            account.setAcctReissueDate(truncate(request.getAcctReissueDate(), 10));
        }

        // ACCT-ADDR-ZIP PIC X(10)
        if (request.getAcctAddrZip() != null
                && !request.getAcctAddrZip().isBlank()) {
            account.setAcctAddrZip(truncate(request.getAcctAddrZip(), 10));
        }

        // ACCT-GROUP-ID PIC X(10)
        if (request.getAcctGroupId() != null
                && !request.getAcctGroupId().isBlank()) {
            account.setAcctGroupId(truncate(request.getAcctGroupId(), 10));
        }
    }

    /**
     * Parse a monetary string to {@link BigDecimal} with scale 2 and HALF_UP rounding.
     *
     * <p>Mirrors COBOL COMP-3 packed-decimal arithmetic:
     * <pre>
     *   COMPUTE ACCT-CURR-BAL ROUNDED = input-value
     * </pre>
     * Uses {@link RoundingMode#HALF_UP} — the default COBOL rounding mode.
     *
     * @param value  the string value from the input record
     * @param fieldName COBOL field name for error messages
     * @return BigDecimal with scale=2 and HALF_UP rounding
     * @throws AccountFileProcessingException on parse failure (ABEND path)
     */
    BigDecimal parseMonetary(String value, String fieldName) {
        try {
            return new BigDecimal(value)
                    .setScale(MONETARY_SCALE, COBOL_ROUNDING);
        } catch (NumberFormatException e) {
            throw new AccountFileProcessingException(
                    "ERROR READING ACCOUNT FILE – invalid " + fieldName
                            + " value: [" + value + "]",
                    APPL_ERR);
        }
    }

    /** Truncate a string to maxLen, mirroring fixed-length PIC X(n) fields. */
    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }

    // -----------------------------------------------------------------------
    // Inner exception class (mirrors COBOL ABEND / 9999-ABEND-PROGRAM)
    // -----------------------------------------------------------------------

    /**
     * Unchecked exception thrown when the account-file processing encounters
     * a fatal error, equivalent to the COBOL {@code 9999-ABEND-PROGRAM}
     * routine invoked when APPL-RESULT is neither APPL-AOK nor APPL-EOF.
     */
    public static class AccountFileProcessingException extends RuntimeException {

        private final int applResult;

        public AccountFileProcessingException(String message, int applResult) {
            super(message);
            this.applResult = applResult;
        }

        /**
         * Returns the COBOL APPL-RESULT code:
         * <ul>
         *   <li>0  = APPL-AOK  (success)</li>
         *   <li>12 = error / ABEND</li>
         *   <li>16 = APPL-EOF  (end of file)</li>
         * </ul>
         */
        public int getApplResult() {
            return applResult;
        }
    }
}
