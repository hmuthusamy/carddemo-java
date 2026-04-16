package com.carddemo.service;

import com.carddemo.model.TransactionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Cbtrn03cService – Transaction Rejection Handler Service.
 *
 * <p>Migrated from COBOL batch program {@code CBTRN03C.CBL} (CardDemo).
 *
 * <p>The COBOL program performed:
 * <ol>
 *   <li>Sequential read of TRANSACT-FILE filtered by date range (DATEPARM file).</li>
 *   <li>XREF lookup (CARDXREF) to resolve card→account mapping.</li>
 *   <li>TRANTYPE lookup to validate/describe the transaction type code.</li>
 *   <li>TRANCATG lookup to validate/describe the transaction category.</li>
 *   <li>Write of a detail report (REPORT-FILE) with page/account/grand totals.</li>
 * </ol>
 *
 * <p>This service encapsulates the validation and rejection-reason logic, mapping
 * each failure path to a structured rejection reason code, mirroring COBOL error
 * handling (ABEND paths replaced with logged rejections):
 * <ul>
 *   <li>{@code R001} – Invalid card number / XREF lookup failure</li>
 *   <li>{@code R002} – Unknown transaction type code</li>
 *   <li>{@code R003} – Unknown transaction category</li>
 *   <li>{@code R004} – Transaction date outside allowed range</li>
 *   <li>{@code R005} – Zero or negative transaction amount</li>
 *   <li>{@code R999} – General / unclassified rejection</li>
 * </ul>
 */
@Service
public class Cbtrn03cService {

    private static final Logger log = LoggerFactory.getLogger(Cbtrn03cService.class);

    // -----------------------------------------------------------------------
    // Rejection reason codes (mirrors COBOL error paths)
    // -----------------------------------------------------------------------

    public static final String REJECTION_CODE_INVALID_CARD    = "R001";
    public static final String REJECTION_CODE_INVALID_TYPE    = "R002";
    public static final String REJECTION_CODE_INVALID_CATG    = "R003";
    public static final String REJECTION_CODE_DATE_RANGE      = "R004";
    public static final String REJECTION_CODE_INVALID_AMOUNT  = "R005";
    public static final String REJECTION_CODE_GENERAL         = "R999";

    // -----------------------------------------------------------------------
    // Reference data maps (replaces VSAM XREF / TRANTYPE / TRANCATG lookups)
    // In a production system these would be injected from a repository or cache.
    // -----------------------------------------------------------------------

    private final Map<String, String> tranTypeDescriptions  = new HashMap<>();
    private final Map<String, String> tranCatgDescriptions  = new HashMap<>();

    public Cbtrn03cService() {
        // Seed known transaction type descriptions  (CVTRA03Y copybook equivalents)
        tranTypeDescriptions.put("PR", "Purchase");
        tranTypeDescriptions.put("DB", "Debit Adjustment");
        tranTypeDescriptions.put("CR", "Credit Adjustment");
        tranTypeDescriptions.put("RF", "Refund");
        tranTypeDescriptions.put("FE", "Fee");
        tranTypeDescriptions.put("IN", "Interest");

        // Seed known transaction category descriptions  (CVTRA04Y copybook equivalents)
        // Key format: typeCode (2 chars) + categoryCode (4 digits zero-padded)
        // e.g. "PR0001" = Purchase / Retail
        tranCatgDescriptions.put("PR0001", "Retail");
        tranCatgDescriptions.put("PR0002", "Online Retail");
        tranCatgDescriptions.put("PR0003", "Restaurant");
        tranCatgDescriptions.put("PR0004", "Fast Food");
        tranCatgDescriptions.put("DB0001", "Debit - Retail");
        tranCatgDescriptions.put("DB0002", "Online Retail");
        tranCatgDescriptions.put("CR0001", "Credit - Retail");
        tranCatgDescriptions.put("CR0002", "Online Retail");
        tranCatgDescriptions.put("RF0001", "Refund - Retail");
        tranCatgDescriptions.put("RF0002", "Refund - Online");
        tranCatgDescriptions.put("FE0001", "Fee - Annual");
        tranCatgDescriptions.put("FE0002", "Fee - Late Payment");
        tranCatgDescriptions.put("IN0001", "Interest - Purchase");
        tranCatgDescriptions.put("IN0002", "Interest - Cash Advance");
        tranCatgDescriptions.put("PR0005", "Travel - Airlines");
        tranCatgDescriptions.put("PR0006", "Travel - Hotels");
        tranCatgDescriptions.put("PR0007", "Fuel");
        tranCatgDescriptions.put("PR0008", "Healthcare");
        tranCatgDescriptions.put("PR0009", "Entertainment");
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Applies rejection reason codes to a {@link TransactionData} record.
     *
     * <p>Mirrors the COBOL validation sequence:
     * <ol>
     *   <li>Card number validation (1500-A-LOOKUP-XREF).</li>
     *   <li>Transaction type validation (1500-B-LOOKUP-TRANTYPE).</li>
     *   <li>Transaction category validation (1500-C-LOOKUP-TRANCATG).</li>
     *   <li>Date-range check (main loop condition on WS-START-DATE / WS-END-DATE).</li>
     *   <li>Amount sanity check (TRAN-AMT used in accumulator).</li>
     * </ol>
     *
     * @param transaction the transaction record to evaluate
     * @param startDate   inclusive start of the allowed date range (WS-START-DATE)
     * @param endDate     inclusive end of the allowed date range (WS-END-DATE)
     * @return the same {@code transaction} object, potentially updated with
     *         {@link TransactionData#setRejected(boolean)} and
     *         {@link TransactionData#setRejectionReasonCode(String)}
     */
    public TransactionData applyRejectionReasonCode(
            TransactionData transaction,
            LocalDate startDate,
            LocalDate endDate) {

        log.debug("Evaluating transaction {} for rejection", transaction.getTransactionId());

        // --- 1. Card number validation (mirrors 1500-A-LOOKUP-XREF INVALID KEY path) ---
        if (isBlankOrNull(transaction.getCardNumber())) {
            return reject(transaction, REJECTION_CODE_INVALID_CARD,
                    "Invalid card number: blank or null");
        }

        // --- 2. Transaction type validation (mirrors 1500-B-LOOKUP-TRANTYPE INVALID KEY path) ---
        String typeCode = transaction.getTransactionTypeCode();
        if (isBlankOrNull(typeCode) || !tranTypeDescriptions.containsKey(typeCode.trim().toUpperCase())) {
            return reject(transaction, REJECTION_CODE_INVALID_TYPE,
                    "Unknown transaction type code: [" + typeCode + "]");
        }
        // Populate description if not already set (mirrors TRAN-TYPE-DESC move)
        if (isBlankOrNull(transaction.getTransactionTypeDescription())) {
            transaction.setTransactionTypeDescription(
                    tranTypeDescriptions.get(typeCode.trim().toUpperCase()));
        }

        // --- 3. Category validation (mirrors 1500-C-LOOKUP-TRANCATG INVALID KEY path) ---
        String catgKey = buildCategoryKey(transaction);
        if (catgKey == null || !tranCatgDescriptions.containsKey(catgKey)) {
            return reject(transaction, REJECTION_CODE_INVALID_CATG,
                    "Unknown transaction category key: [" + catgKey + "]");
        }
        if (isBlankOrNull(transaction.getTransactionCategoryDescription())) {
            transaction.setTransactionCategoryDescription(tranCatgDescriptions.get(catgKey));
        }

        // --- 4. Date-range check (mirrors date-range filter in main loop) ---
        if (startDate != null && endDate != null && transaction.getTransactionDate() != null) {
            LocalDate txDate = transaction.getTransactionDate();
            if (txDate.isBefore(startDate) || txDate.isAfter(endDate)) {
                return reject(transaction, REJECTION_CODE_DATE_RANGE,
                        "Transaction date " + txDate
                                + " is outside reporting range [" + startDate + "," + endDate + "]");
            }
        }

        // --- 5. Amount sanity check (mirrors ADD TRAN-AMT TO accumulators – zero/negative = reject) ---
        if (transaction.getTransactionAmount() == null
                || transaction.getTransactionAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return reject(transaction, REJECTION_CODE_INVALID_AMOUNT,
                    "Transaction amount is zero or negative: "
                            + transaction.getTransactionAmount());
        }

        // All checks passed – transaction is valid
        transaction.setRejected(false);
        transaction.setRejectionReasonCode(null);
        transaction.setRejectionReasonDescription(null);
        log.debug("Transaction {} passed all validation checks", transaction.getTransactionId());
        return transaction;
    }

    /**
     * Validates a transaction without date-range filtering.
     * Convenience overload for cases where no date parameters are provided.
     *
     * @param transaction the transaction record to evaluate
     * @return the evaluated transaction
     */
    public TransactionData applyRejectionReasonCode(TransactionData transaction) {
        return applyRejectionReasonCode(transaction, null, null);
    }

    /**
     * Logs full rejection details for a rejected transaction, mirroring the COBOL
     * DISPLAY statements emitted on error paths (e.g. "INVALID CARD NUMBER :").
     *
     * @param transaction a rejected transaction
     */
    public void logRejectionDetails(TransactionData transaction) {
        if (!transaction.isRejected()) {
            return;
        }
        log.warn("TRANSACTION REJECTED | id={} | card={} | code={} | reason={}",
                transaction.getTransactionId(),
                transaction.getCardNumber(),
                transaction.getRejectionReasonCode(),
                transaction.getRejectionReasonDescription());
    }

    /**
     * Generates a one-line report entry for a rejected transaction, equivalent to
     * the detail line written to REPORT-FILE by CBTRN03C's 1120-WRITE-DETAIL paragraph.
     *
     * @param transaction a rejected transaction
     * @return formatted rejection report line (max 133 chars, matching COBOL FD-REPTFILE-REC)
     */
    public String formatRejectionReportLine(TransactionData transaction) {
        // Mirrors TRANSACTION-DETAIL-REPORT layout in CVTRA07Y copybook
        return String.format("%-16s %-10s %-2s %-30s %4d %-30s %-10s %12.2f [%s] %s",
                nvl(transaction.getTransactionId()),
                nvl(transaction.getAccountId()),
                nvl(transaction.getTransactionTypeCode()),
                truncate(nvl(transaction.getTransactionTypeDescription()), 30),
                transaction.getTransactionCategoryCode() != null
                        ? transaction.getTransactionCategoryCode() : 0,
                truncate(nvl(transaction.getTransactionCategoryDescription()), 30),
                nvl(transaction.getTransactionSource()),
                transaction.getTransactionAmount() != null
                        ? transaction.getTransactionAmount() : BigDecimal.ZERO,
                nvl(transaction.getRejectionReasonCode()),
                truncate(nvl(transaction.getRejectionReasonDescription()), 20));
    }

    // -----------------------------------------------------------------------
    // Reference-data accessors (public for testing and external use)
    // -----------------------------------------------------------------------

    /** Returns the description for a transaction type code, or {@code null} if unknown. */
    public String lookupTransactionTypeDescription(String typeCode) {
        if (isBlankOrNull(typeCode)) return null;
        return tranTypeDescriptions.get(typeCode.trim().toUpperCase());
    }

    /** Returns the description for a type+category composite key, or {@code null} if unknown. */
    public String lookupCategoryDescription(String compositeKey) {
        if (isBlankOrNull(compositeKey)) return null;
        return tranCatgDescriptions.get(compositeKey);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private TransactionData reject(TransactionData tx, String code, String description) {
        tx.setRejected(true);
        tx.setRejectionReasonCode(code);
        tx.setRejectionReasonDescription(description);
        log.warn("Rejecting transaction {} – [{}] {}", tx.getTransactionId(), code, description);
        return tx;
    }

    private String buildCategoryKey(TransactionData tx) {
        if (isBlankOrNull(tx.getTransactionTypeCode())
                || tx.getTransactionCategoryCode() == null) {
            return null;
        }
        return String.format("%s%04d",
                tx.getTransactionTypeCode().trim().toUpperCase(),
                tx.getTransactionCategoryCode());
    }

    private static boolean isBlankOrNull(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
