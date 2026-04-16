package com.carddemo.service;

import com.carddemo.model.AccountData;
import com.carddemo.model.CustomerData;
import com.carddemo.model.StatementRecord;
import com.carddemo.model.TransactionData;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * StatementService – business logic for statement generation.
 *
 * Migrated from CBSTM03A.CBL:
 *   5000-CREATE-STATEMENT  → buildStatementHeader()
 *   4000-TRNXFILE-GET      → enrichWithTransactions()
 *   6000-WRITE-TRANS       → formatTransactionLine()
 *   formatCurrency()       → mirrors PIC 9(9).99- COBOL picture
 *
 * Migrated from CBSTM03B.CBL:
 *   File-handling subroutine logic is replaced by Spring Data / JPA
 *   repositories injected into the batch steps. StatementService
 *   holds only the formatting and aggregation logic that was
 *   previously scattered across CBSTM03A paragraphs.
 */
@Service
public class StatementService {

    // ── ST-LINE constants (mirrors CBSTM03A STATEMENT-LINES group) ──

    private static final String LINE_STARS_80    = "*".repeat(31) + "START OF STATEMENT" + "*".repeat(31);
    private static final String LINE_SEPARATOR   = "-".repeat(80);
    private static final String LINE_BASIC_HDR   = " ".repeat(33) + "Basic Details" + " ".repeat(34);
    private static final String LINE_TRAN_HDR    = "Tran ID         " + "Tran Details    "
                                                   + "                                   " + "  Tran Amount";
    private static final String LINE_END_STMT    = "*".repeat(32) + "END OF STATEMENT" + "*".repeat(32);

    // ── Step 1 (CBSTM03A): Build statement header ────────────────────

    /**
     * Builds a {@link StatementRecord} from account + customer data.
     * Mirrors CBSTM03A paragraphs 5000-CREATE-STATEMENT and STRING logic.
     *
     * @param xrefCardNumber card number from cross-reference (XREF-CARD-NUM)
     * @param account        account record (ACCT-ID, ACCT-CURR-BAL)
     * @param customer       customer record (name, address, FICO)
     * @return populated StatementRecord ready for line-item enrichment
     */
    public StatementRecord buildStatementHeader(String xrefCardNumber,
                                                AccountData account,
                                                CustomerData customer) {
        StatementRecord stmt = new StatementRecord();
        stmt.setCardNumber(xrefCardNumber);

        // Mirrors: MOVE ACCT-ID TO ST-ACCT-ID
        stmt.setAccountId(account.getAccountId());

        // Mirrors: MOVE ACCT-CURR-BAL TO ST-CURR-BAL (scale 2 ensured)
        stmt.setCurrentBalance(
            account.getCurrentBalance() != null
                ? account.getCurrentBalance().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        );

        // Mirrors COBOL STRING for ST-NAME (CUST-FIRST CUST-MIDDLE CUST-LAST)
        stmt.setCustomerName(customer.getFullName());

        // Mirrors: MOVE CUST-ADDR-LINE-1 TO ST-ADD1
        stmt.setAddressLine1(trimToWidth(customer.getAddressLine1(), 50));

        // Mirrors: MOVE CUST-ADDR-LINE-2 TO ST-ADD2
        stmt.setAddressLine2(trimToWidth(customer.getAddressLine2(), 50));

        // Mirrors COBOL STRING: CUST-ADDR-LINE-3 STATE COUNTRY ZIP → ST-ADD3
        stmt.setAddressLine3(trimToWidth(customer.getAddressLine3Full(), 80));

        // Mirrors: MOVE CUST-FICO-CREDIT-SCORE TO ST-FICO-SCORE
        stmt.setFicoScore(String.valueOf(customer.getFicoScore()));

        return stmt;
    }

    // ── Step 2 (CBSTM03B): Enrich with transactions + subtotal ──────

    /**
     * Attaches transaction line items to a header record and computes
     * the running subtotal.
     *
     * Mirrors CBSTM03A paragraphs:
     *   4000-TRNXFILE-GET  – loop over matching card transactions
     *   ADD TRNX-AMT TO WS-TOTAL-AMT
     *   MOVE WS-TOTAL-AMT TO ST-TOTAL-TRAMT
     *
     * @param stmt         statement header (already built by step 1)
     * @param transactions transactions belonging to the same card number
     */
    public void enrichWithTransactions(StatementRecord stmt,
                                       List<TransactionData> transactions) {
        // Reset in case of re-enrichment
        stmt.setTotalTransactionAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        stmt.getTransactions().clear();

        for (TransactionData txn : transactions) {
            // Mirrors: PERFORM 6000-WRITE-TRANS then ADD TRNX-AMT TO WS-TOTAL-AMT
            stmt.addTransaction(txn);   // addTransaction() also accumulates the total
        }
        // After loop: totalTransactionAmount == WS-TOTAL-AMT
        // (MOVE WS-TOTAL-AMT TO ST-TOTAL-TRAMT is handled in the writer)
    }

    // ── Text formatting (mirrors COBOL PICTURE clauses) ──────────────

    /**
     * Formats a BigDecimal as COBOL PIC Z(9).99- (leading-zero-suppressed,
     * trailing-minus for negatives).  Width = 13 characters.
     *
     * Example: -123.45   → "      123.45-"
     *          1234567.89 → " 1234567.89 "
     *
     * Mirrors ST-TRANAMT PIC Z(9).99- and ST-TOTAL-TRAMT PIC Z(9).99-
     */
    public String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return " ".repeat(13);
        }
        BigDecimal abs = amount.abs().setScale(2, RoundingMode.HALF_UP);
        // Format with explicit 9-digit integer + 2-decimal (no locale grouping)
        // PIC Z(9).99- → 9 integer digits + '.' + 2 decimal = 12 chars + sign = 13
        long intPart  = abs.longValue();
        long fracPart = abs.subtract(new java.math.BigDecimal(intPart))
                           .movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
        String intStr  = String.format("%9d", intPart);
        String fracStr = String.format("%02d", fracPart);

        // Z(9) – suppress leading zeros (replace leading zeros with spaces)
        String suppressed = intStr.replaceFirst("^( *)0+(?=\\d)", "$1");
        // Edge case: all zeros → single "0"
        if (suppressed.isBlank()) suppressed = " ".repeat(8) + "0";

        String core = suppressed + "." + fracStr;  // 12 chars
        boolean negative = amount.compareTo(BigDecimal.ZERO) < 0;
        return core + (negative ? "-" : " ");      // 13 chars total
    }

    /**
     * Formats a single transaction line (ST-LINE14, 80 chars).
     *
     * Mirrors CBSTM03A 6000-WRITE-TRANS:
     *   MOVE TRNX-ID TO ST-TRANID        PIC X(16)
     *   MOVE TRNX-DESC TO ST-TRANDT      PIC X(49)
     *   MOVE TRNX-AMT TO ST-TRANAMT      PIC Z(9).99-
     */
    public String formatTransactionLine(TransactionData txn) {
        String tranId   = padRight(txn.getTransactionId(),   16);
        String tranDesc = padRight(txn.getTransactionDesc(), 49);
        String amount   = "$" + formatCurrency(txn.getTransactionAmount());
        return tranId + " " + tranDesc + amount;
    }

    /**
     * Formats the "Total EXP" subtotal line (ST-LINE14A, 80 chars).
     *
     * Mirrors CBSTM03A 4000-TRNXFILE-GET:
     *   MOVE WS-TOTAL-AMT TO ST-TOTAL-TRAMT and WRITE ST-LINE14A.
     */
    public String formatTotalLine(BigDecimal totalAmount) {
        String prefix = padRight("Total EXP:", 67);   // 10 chars + 57 spaces = 67
        return prefix + "$" + formatCurrency(totalAmount);
    }

    /**
     * Builds the full plain-text statement as a list of 80-char lines.
     *
     * Mirrors CBSTM03A 5000-CREATE-STATEMENT, 6000-WRITE-TRANS,
     * and 4000-TRNXFILE-GET output sections.
     */
    public List<String> buildPlainTextStatement(StatementRecord stmt) {
        java.util.List<String> lines = new java.util.ArrayList<>();

        // ST-LINE0: *** START OF STATEMENT ***
        lines.add(LINE_STARS_80);

        // ST-LINE1: customer name
        lines.add(padRight(stmt.getCustomerName(), 80));

        // ST-LINE2: address line 1
        lines.add(padRight(stmt.getAddressLine1() != null ? stmt.getAddressLine1() : "", 80));

        // ST-LINE3: address line 2
        lines.add(padRight(stmt.getAddressLine2() != null ? stmt.getAddressLine2() : "", 80));

        // ST-LINE4: address line 3 (city/state/country/zip)
        lines.add(padRight(stmt.getAddressLine3() != null ? stmt.getAddressLine3() : "", 80));

        // ST-LINE5: separator
        lines.add(LINE_SEPARATOR);

        // ST-LINE6: "Basic Details" centered
        lines.add(LINE_BASIC_HDR);

        // ST-LINE5: separator (written twice per COBOL)
        lines.add(LINE_SEPARATOR);

        // ST-LINE7: Account ID
        lines.add(padRight("Account ID         :" + padRight(stmt.getAccountId(), 20), 80));

        // ST-LINE8: Current Balance (PIC 9(9).99-)
        lines.add(padRight("Current Balance    :" + formatCurrency(stmt.getCurrentBalance()), 80));

        // ST-LINE9: FICO Score
        lines.add(padRight("FICO Score         :" + padRight(stmt.getFicoScore(), 20), 80));

        // ST-LINE10: separator
        lines.add(LINE_SEPARATOR);

        // ST-LINE11: "TRANSACTION SUMMARY"
        lines.add(" ".repeat(30) + "TRANSACTION SUMMARY " + " ".repeat(30));

        // ST-LINE12: separator
        lines.add(LINE_SEPARATOR);

        // ST-LINE13: column headers
        lines.add(LINE_TRAN_HDR);

        // ST-LINE12: separator (written again per COBOL)
        lines.add(LINE_SEPARATOR);

        // Transaction lines (ST-LINE14) – mirrors 6000-WRITE-TRANS
        for (TransactionData txn : stmt.getTransactions()) {
            lines.add(formatTransactionLine(txn));
        }

        // ST-LINE12: closing separator before totals
        lines.add(LINE_SEPARATOR);

        // ST-LINE14A: total expense line – mirrors 4000-TRNXFILE-GET
        lines.add(formatTotalLine(stmt.getTotalTransactionAmount()));

        // ST-LINE15: *** END OF STATEMENT ***
        lines.add(LINE_END_STMT);

        return lines;
    }

    // ── Utility helpers ──────────────────────────────────────────────

    /**
     * Right-pads a string with spaces to the given width (mirrors COBOL PIC X(n)).
     * Truncates if longer than width.
     */
    public String padRight(String value, int width) {
        if (value == null) value = "";
        if (value.length() >= width) return value.substring(0, width);
        return String.format("%-" + width + "s", value);
    }

    /**
     * Trims a string to max width, returning null-safe result.
     */
    private String trimToWidth(String value, int maxWidth) {
        if (value == null) return "";
        return value.length() > maxWidth ? value.substring(0, maxWidth) : value;
    }
}
