package com.carddemo.service;

import com.carddemo.model.Account;
import com.carddemo.model.CardXref;
import com.carddemo.model.TransactionData;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardXrefRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Business-logic service for CBTRN01C transaction processing.
 *
 * <p>This class encapsulates the validation and enrichment rules that were
 * embedded in the COBOL PROCEDURE DIVISION of {@code CBTRN01C.cbl}.
 *
 * <h2>COBOL → Java mapping</h2>
 * <table border="1">
 *   <tr><th>COBOL paragraph</th><th>Java method</th></tr>
 *   <tr><td>2000-LOOKUP-XREF</td><td>{@link #lookupCardXref(String)}</td></tr>
 *   <tr><td>3000-READ-ACCOUNT</td><td>{@link #lookupAccount(Long)}</td></tr>
 *   <tr><td>MAIN-PARA (validation block)</td><td>{@link #validateAndEnrich(TransactionData)}</td></tr>
 * </table>
 *
 * <h2>Validation rules (ported from COBOL)</h2>
 * <ol>
 *   <li>Amount must be non-null and &gt; 0 (no COBOL rule existed but defensive guard added).</li>
 *   <li>Card number must resolve to an XREF record (2000-LOOKUP-XREF: INVALID KEY path).</li>
 *   <li>The resolved account must exist in ACCOUNT-FILE (3000-READ-ACCOUNT: INVALID KEY path).</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cbtrn01cService {

    private final CardXrefRepository cardXrefRepository;
    private final AccountRepository  accountRepository;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Validates and enriches a single {@link TransactionData} item read from the
     * daily-transaction flat file (DALYTRAN-FILE).
     *
     * <p>The method mirrors the COBOL MAIN-PARA control flow:
     * <ol>
     *   <li>Amount guard – reject immediately if amount ≤ 0.</li>
     *   <li>XREF lookup   – reject with {@code REJECTED_CARD} if card unknown.</li>
     *   <li>Account lookup – reject with {@code REJECTED_ACCOUNT} if account missing.</li>
     *   <li>Enrichment    – stamp {@code VALID}, set {@code accountId} and {@code procTimestamp}.</li>
     * </ol>
     *
     * @param transaction the item freshly read by the Spring Batch reader
     * @return the mutated (enriched or rejected) transaction; never {@code null}
     */
    public TransactionData validateAndEnrich(TransactionData transaction) {

        log.debug("Processing transaction id={} card={}", transaction.getTransactionId(),
                transaction.getCardNumber());

        // --- Amount validation (defensive – COBOL assumed input was always valid) ---
        if (!isAmountValid(transaction.getAmount())) {
            log.warn("Transaction {} rejected – invalid amount: {}",
                    transaction.getTransactionId(), transaction.getAmount());
            return reject(transaction, "REJECTED_AMOUNT");
        }

        // --- 2000-LOOKUP-XREF equivalent ---
        Optional<CardXref> xrefOpt = lookupCardXref(transaction.getCardNumber());
        if (xrefOpt.isEmpty()) {
            log.warn("Transaction {} rejected – card number {} could not be verified",
                    transaction.getTransactionId(), transaction.getCardNumber());
            // COBOL: DISPLAY 'CARD NUMBER ' DALYTRAN-CARD-NUM ' COULD NOT BE VERIFIED...'
            return reject(transaction, "REJECTED_CARD");
        }

        CardXref xref = xrefOpt.get();
        log.debug("XREF resolved: card={} → account={} customer={}",
                xref.getCardNumber(), xref.getAccountId(), xref.getCustomerId());

        // --- 3000-READ-ACCOUNT equivalent ---
        Optional<Account> accountOpt = lookupAccount(xref.getAccountId());
        if (accountOpt.isEmpty()) {
            log.warn("Transaction {} rejected – account {} not found",
                    transaction.getTransactionId(), xref.getAccountId());
            // COBOL: DISPLAY 'ACCOUNT ' ACCT-ID ' NOT FOUND'
            return reject(transaction, "REJECTED_ACCOUNT");
        }

        // --- Valid: enrich and mark for posting ---
        transaction.setAccountId(xref.getAccountId());
        transaction.setStatus("VALID");
        transaction.setProcTimestamp(LocalDateTime.now());
        log.info("Transaction {} VALID – posting to account {}",
                transaction.getTransactionId(), xref.getAccountId());
        return transaction;
    }

    /**
     * Returns {@code true} when {@code amount} is non-null and strictly positive.
     * Mirrors the implicit COBOL assumption that an amount of zero or negative
     * would not represent a meaningful daily transaction.
     *
     * @param amount the monetary amount from the transaction record
     * @return {@code true} if the amount passes validation
     */
    public boolean isAmountValid(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    // -------------------------------------------------------------------------
    // Package-private helpers (used directly in unit tests)
    // -------------------------------------------------------------------------

    /**
     * Performs a card-number → XREF lookup (COBOL 2000-LOOKUP-XREF).
     *
     * @param cardNumber the 16-character card number from the daily transaction
     * @return an {@link Optional} containing the {@link CardXref} if found
     */
    Optional<CardXref> lookupCardXref(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank()) {
            return Optional.empty();
        }
        return cardXrefRepository.findById(cardNumber.trim());
    }

    /**
     * Reads the account record for a given account ID (COBOL 3000-READ-ACCOUNT).
     *
     * @param accountId the account ID resolved from the XREF record
     * @return an {@link Optional} containing the {@link Account} if found
     */
    Optional<Account> lookupAccount(Long accountId) {
        if (accountId == null) {
            return Optional.empty();
        }
        return accountRepository.findById(accountId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private TransactionData reject(TransactionData tx, String status) {
        tx.setStatus(status);
        tx.setProcTimestamp(LocalDateTime.now());
        return tx;
    }
}
