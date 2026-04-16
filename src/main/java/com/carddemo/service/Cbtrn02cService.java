package com.carddemo.service;

import com.carddemo.model.AccountData;
import com.carddemo.model.TranCatBal;
import com.carddemo.model.TransactionData;
import com.carddemo.repository.AccountDataRepository;
import com.carddemo.repository.TranCatBalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Service implementing the core business logic of COBOL program CBTRN02C.CBL.
 *
 * <h2>COBOL Paragraph Mapping</h2>
 * <pre>
 *  COBOL paragraph              Java method
 *  ─────────────────────────    ────────────────────────────────────────
 *  2000-POST-TRANSACTION        postTransaction(TransactionData)
 *  2700-UPDATE-TCATBAL          updateTranCatBal(TransactionData, Long)
 *  2700-A-CREATE-TCATBAL-REC    (inline inside updateTranCatBal)
 *  2700-B-UPDATE-TCATBAL-REC    (inline inside updateTranCatBal)
 *  2800-UPDATE-ACCOUNT-REC      updateAccountBalance(TransactionData, AccountData)
 *  1500-B-LOOKUP-ACCT           validateAndLoadAccount(TransactionData)
 * </pre>
 *
 * <h2>Sign conventions preserved from COBOL</h2>
 * <ul>
 *   <li>Positive amount  = charge/debit   → adds to {@code acctCurrCycCredit} and {@code acctCurrBal}</li>
 *   <li>Negative amount  = refund/payment → adds to {@code acctCurrCycDebit}  and {@code acctCurrBal}</li>
 *   <li>All arithmetic uses {@link RoundingMode#HALF_UP} to match COBOL ROUNDED behaviour</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cbtrn02cService {

    /** Scale used for all monetary arithmetic – mirrors COBOL S9(09)V99 / S9(10)V99 */
    private static final int MONETARY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final AccountDataRepository accountDataRepository;
    private final TranCatBalRepository  tranCatBalRepository;

    // ────────────────────────────────────────────────────────────────────────
    // PUBLIC API – called by the Spring Batch processor/writer
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Apply a single APPROVED transaction to the account balances and category balance.
     * Mirrors COBOL paragraph {@code 2000-POST-TRANSACTION}.
     *
     * @param transaction the APPROVED transaction to post
     * @return the updated AccountData after balance update
     * @throws IllegalArgumentException if the account referenced by the transaction does not exist
     */
    @Transactional
    public AccountData postTransaction(TransactionData transaction) {
        log.debug("Posting transaction id={} acct={} amt={}",
                transaction.getTranId(), transaction.getTranAcctId(), transaction.getTranAmt());

        // ── 2700-UPDATE-TCATBAL ──────────────────────────────────────────────
        updateTranCatBal(transaction);

        // ── 2800-UPDATE-ACCOUNT-REC ──────────────────────────────────────────
        AccountData account = accountDataRepository.findById(transaction.getTranAcctId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found for id=" + transaction.getTranAcctId()
                        + " tranId=" + transaction.getTranId()));

        updateAccountBalance(transaction, account);
        AccountData saved = accountDataRepository.save(account);

        // Mark transaction as POSTED and stamp processing timestamp
        transaction.setTranStatus("POSTED");
        transaction.setTranProcTs(LocalDateTime.now());

        log.info("Posted transaction id={} acct={} amt={} newBal={}",
                transaction.getTranId(), account.getAcctId(),
                transaction.getTranAmt(), saved.getAcctCurrBal());

        return saved;
    }

    // ────────────────────────────────────────────────────────────────────────
    // PUBLIC HELPERS (also called directly by unit tests for paragraph-level testing)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Update (or create) the transaction category balance record.
     * Mirrors COBOL paragraphs {@code 2700-UPDATE-TCATBAL},
     * {@code 2700-A-CREATE-TCATBAL-REC}, {@code 2700-B-UPDATE-TCATBAL-REC}.
     *
     * <pre>COBOL:
     *   READ TCATBAL-FILE BY KEY  → status 00 (found) or 23 (not found)
     *   IF NOT FOUND → INITIALIZE + WRITE
     *   ELSE         → ADD DALYTRAN-AMT TO TRAN-CAT-BAL + REWRITE
     * </pre>
     */
    public void updateTranCatBal(TransactionData transaction) {
        Long   acctId = transaction.getTranAcctId();
        String typeCd = transaction.getTranTypeCd();
        Integer catCd = transaction.getTranCatCd();

        BigDecimal amt = roundMonetary(transaction.getTranAmt());

        TranCatBal catBal = tranCatBalRepository
                .findByTrancatAcctIdAndTrancatTypeCdAndTrancatCd(acctId, typeCd, catCd)
                .orElseGet(() -> {
                    // 2700-A-CREATE-TCATBAL-REC: INITIALIZE → zero balance
                    log.debug("TranCatBal not found for acct={} type={} cat={} – creating",
                            acctId, typeCd, catCd);
                    return TranCatBal.builder()
                            .trancatAcctId(acctId)
                            .trancatTypeCd(typeCd)
                            .trancatCd(catCd)
                            .tranCatBal(BigDecimal.ZERO.setScale(MONETARY_SCALE, ROUNDING))
                            .build();
                });

        // ADD DALYTRAN-AMT TO TRAN-CAT-BAL  (both create and update paths)
        BigDecimal newCatBal = roundMonetary(catBal.getTranCatBal().add(amt));
        catBal.setTranCatBal(newCatBal);
        tranCatBalRepository.save(catBal);
    }

    /**
     * Apply the transaction amount to account balance fields.
     * Mirrors COBOL paragraph {@code 2800-UPDATE-ACCOUNT-REC}.
     *
     * <pre>COBOL:
     *   ADD DALYTRAN-AMT TO ACCT-CURR-BAL
     *   IF DALYTRAN-AMT >= 0
     *      ADD DALYTRAN-AMT TO ACCT-CURR-CYC-CREDIT
     *   ELSE
     *      ADD DALYTRAN-AMT TO ACCT-CURR-CYC-DEBIT
     * </pre>
     *
     * Note: The COBOL adds the raw (possibly negative) amount to ACCT-CURR-CYC-DEBIT,
     * making DEBIT a signed accumulator. The Java implementation preserves this exactly.
     */
    public void updateAccountBalance(TransactionData transaction, AccountData account) {
        BigDecimal amt = roundMonetary(transaction.getTranAmt());

        // ADD DALYTRAN-AMT TO ACCT-CURR-BAL
        BigDecimal newBal = roundMonetary(
                nullSafe(account.getAcctCurrBal()).add(amt));
        account.setAcctCurrBal(newBal);

        if (amt.compareTo(BigDecimal.ZERO) >= 0) {
            // Positive amount: charge / debit → update credit cycle accumulator
            BigDecimal newCredit = roundMonetary(
                    nullSafe(account.getAcctCurrCycCredit()).add(amt));
            account.setAcctCurrCycCredit(newCredit);
        } else {
            // Negative amount: refund / payment → update debit cycle accumulator
            BigDecimal newDebit = roundMonetary(
                    nullSafe(account.getAcctCurrCycDebit()).add(amt));
            account.setAcctCurrCycDebit(newDebit);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ────────────────────────────────────────────────────────────────────────

    /** Apply HALF_UP rounding to 2 decimal places, matching COBOL ROUNDED. */
    public BigDecimal roundMonetary(BigDecimal value) {
        return value.setScale(MONETARY_SCALE, ROUNDING);
    }

    /** Treat null balance fields as ZERO (COBOL INITIALIZE sets numeric to 0). */
    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
