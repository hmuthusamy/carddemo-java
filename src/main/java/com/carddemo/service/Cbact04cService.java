package com.carddemo.service;

import com.carddemo.model.*;
import com.carddemo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cbact04cService – business logic migrated from COBOL program CBACT04C.
 *
 * <p>The COBOL program performs the following high-level steps:
 * <ol>
 *   <li>Reads TCATBAL-FILE sequentially (transaction category balances).</li>
 *   <li>For each new account (TRANCAT-ACCT-ID changes), it:
 *       <ul>
 *         <li>Updates the previous account's balance (1050-UPDATE-ACCOUNT)</li>
 *         <li>Reads the account record (1100-GET-ACCT-DATA)</li>
 *         <li>Reads the card cross-reference by account ID (1110-GET-XREF-DATA)</li>
 *       </ul>
 *   </li>
 *   <li>For each TCATBAL record, looks up the interest rate from DISCGRP-FILE
 *       (1200-GET-INTEREST-RATE, with fallback to 'DEFAULT').</li>
 *   <li>If interest rate &gt; 0, computes monthly interest (1300-COMPUTE-INTEREST)
 *       and writes an interest transaction record (1300-B-WRITE-TX).</li>
 *   <li>After the last record, updates the final account.</li>
 * </ol>
 *
 * <p>This service exposes two main methods:
 * <ul>
 *   <li>{@link #processTransactionCategoryBalance} – called by the Spring Batch
 *       ItemProcessor for each TCATBAL record.</li>
 *   <li>{@link #finalizeLastAccount} – called after all items are processed
 *       (via a StepExecutionListener#afterStep) to mirror the COBOL end-of-file
 *       PERFORM 1050-UPDATE-ACCOUNT.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cbact04cService {

    // ---------------------------------------------------------------
    // Constants matching COBOL literals
    // ---------------------------------------------------------------
    private static final String DEFAULT_GROUP_ID    = "DEFAULT";
    private static final String INTEREST_TYPE_CD    = "01";
    private static final String INTEREST_CAT_CD     = "05";
    private static final String TRAN_SOURCE         = "System";
    private static final BigDecimal INTEREST_DIVISOR = new BigDecimal("1200");
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SS0000");

    // ---------------------------------------------------------------
    // Repositories
    // ---------------------------------------------------------------
    private final AccountDataRepository       accountDataRepository;
    private final CardXrefRepository          cardXrefRepository;
    private final DisclosureGroupRepository   disclosureGroupRepository;
    private final TransactionRepository       transactionRepository;

    // ---------------------------------------------------------------
    // Per-job mutable state (reset in #resetState)
    // Mirrors the COBOL WORKING-STORAGE variables.
    // ---------------------------------------------------------------

    /** WS-LAST-ACCT-NUM – tracks the previous account ID to detect group boundary */
    private Long   lastAcctNum    = null;

    /** WS-FIRST-TIME – 'Y' until the first account boundary is crossed */
    private boolean firstTime     = true;

    /** WS-TOTAL-INT – accumulated monthly interest for the current account */
    private BigDecimal totalInterest = BigDecimal.ZERO;

    /** WS-TRANID-SUFFIX – auto-incremented suffix for generated TRAN-IDs */
    private final AtomicLong tranIdSuffix = new AtomicLong(0);

    /** Current account record (set at each account boundary) */
    private AccountData currentAccount = null;

    /** Current card cross-reference record */
    private CardXref currentCardXref = null;

    /** PARM-DATE — set before the job run (mirrors PARM-DATE from LINKAGE SECTION) */
    private String parmDate;

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Reset all stateful variables before a new job execution.
     * Must be called from the Step's beforeStep listener.
     *
     * @param parmDate the run-date parameter (PARM-DATE, PIC X(10) e.g. "2024-01-15")
     */
    public void resetState(String parmDate) {
        this.parmDate       = parmDate;
        this.lastAcctNum    = null;
        this.firstTime      = true;
        this.totalInterest  = BigDecimal.ZERO;
        this.currentAccount = null;
        this.currentCardXref = null;
        this.tranIdSuffix.set(0);
        log.info("CBACT04C state initialised – parmDate={}", parmDate);
    }

    /**
     * Process a single TransactionCategoryBalance record.
     *
     * <p>Mirrors the main PERFORM loop in the PROCEDURE DIVISION:
     * <pre>
     *   IF TRANCAT-ACCT-ID NOT= WS-LAST-ACCT-NUM
     *     IF WS-FIRST-TIME NOT = 'Y'  → 1050-UPDATE-ACCOUNT
     *     ELSE                         → MOVE 'N' TO WS-FIRST-TIME
     *     MOVE 0 TO WS-TOTAL-INT
     *     MOVE TRANCAT-ACCT-ID TO WS-LAST-ACCT-NUM
     *     1100-GET-ACCT-DATA
     *     1110-GET-XREF-DATA
     *   PERFORM 1200-GET-INTEREST-RATE
     *   IF DIS-INT-RATE NOT = 0
     *     1300-COMPUTE-INTEREST → 1300-B-WRITE-TX
     *     1400-COMPUTE-FEES (stub)
     * </pre>
     *
     * @param tcatBal the current TCATBAL record
     * @return list of Transaction records generated for this item (may be empty)
     */
    @Transactional
    public List<Transaction> processTransactionCategoryBalance(TransactionCategoryBalance tcatBal) {

        Long currentAcctId = tcatBal.getId().getTranscatAcctId();
        List<Transaction> generatedTransactions = new ArrayList<>();

        // -----------------------------------------------------------
        // Account boundary detection (mirrors TRANCAT-ACCT-ID NOT= WS-LAST-ACCT-NUM)
        // -----------------------------------------------------------
        if (!currentAcctId.equals(lastAcctNum)) {

            if (!firstTime) {
                // 1050-UPDATE-ACCOUNT for the previous account
                updateAccount();
            } else {
                firstTime = false;
            }

            // Reset total interest accumulator for the new account
            totalInterest = BigDecimal.ZERO;
            lastAcctNum = currentAcctId;

            // 1100-GET-ACCT-DATA
            getAccountData(currentAcctId);

            // 1110-GET-XREF-DATA
            getXrefData(currentAcctId);
        }

        // -----------------------------------------------------------
        // 1200-GET-INTEREST-RATE
        // -----------------------------------------------------------
        BigDecimal intRate = getInterestRate(
                currentAccount != null ? currentAccount.getAcctGroupId() : DEFAULT_GROUP_ID,
                tcatBal.getId().getTranscatTypeCd(),
                tcatBal.getId().getTranscatCd());

        // -----------------------------------------------------------
        // 1300-COMPUTE-INTEREST + 1300-B-WRITE-TX
        // -----------------------------------------------------------
        if (intRate != null && intRate.compareTo(BigDecimal.ZERO) != 0) {

            Transaction tx = computeInterestAndWriteTx(tcatBal, intRate);
            generatedTransactions.add(tx);

            // 1400-COMPUTE-FEES — stub (TO BE IMPLEMENTED per original COBOL comment)
            computeFees(tcatBal);
        }

        return generatedTransactions;
    }

    /**
     * Finalises the last account group after the reader is exhausted.
     *
     * <p>Mirrors the COBOL end-of-file path:
     * <pre>
     *   ELSE  (END-OF-FILE = 'Y')
     *     PERFORM 1050-UPDATE-ACCOUNT
     * </pre>
     */
    @Transactional
    public void finalizeLastAccount() {
        if (!firstTime && currentAccount != null) {
            log.info("CBACT04C finalizeLastAccount – updating account {}", lastAcctNum);
            updateAccount();
        }
    }

    // ---------------------------------------------------------------
    // Internal helpers — each mirrors a COBOL paragraph
    // ---------------------------------------------------------------

    /**
     * 1050-UPDATE-ACCOUNT
     * ADD WS-TOTAL-INT TO ACCT-CURR-BAL
     * MOVE 0 TO ACCT-CURR-CYC-CREDIT
     * MOVE 0 TO ACCT-CURR-CYC-DEBIT
     * REWRITE FD-ACCTFILE-REC FROM ACCOUNT-RECORD
     */
    private void updateAccount() {
        if (currentAccount == null) {
            log.warn("1050-UPDATE-ACCOUNT called but no current account loaded");
            return;
        }
        log.debug("1050-UPDATE-ACCOUNT acct={} totalInt={}", currentAccount.getAcctId(), totalInterest);

        BigDecimal updatedBal = currentAccount.getAcctCurrBal() == null
                ? totalInterest
                : currentAccount.getAcctCurrBal().add(totalInterest);

        currentAccount.setAcctCurrBal(updatedBal);
        currentAccount.setAcctCurrCycCredit(BigDecimal.ZERO);
        currentAccount.setAcctCurrCycDebit(BigDecimal.ZERO);

        accountDataRepository.save(currentAccount);
        log.info("Account {} updated: newBal={}", currentAccount.getAcctId(), updatedBal);
    }

    /**
     * 1100-GET-ACCT-DATA
     * READ ACCOUNT-FILE INTO ACCOUNT-RECORD INVALID KEY DISPLAY ...
     */
    private void getAccountData(Long acctId) {
        Optional<AccountData> opt = accountDataRepository.findById(acctId);
        if (opt.isPresent()) {
            currentAccount = opt.get();
            log.debug("1100-GET-ACCT-DATA found acct={}", acctId);
        } else {
            log.warn("1100-GET-ACCT-DATA ACCOUNT NOT FOUND: {}", acctId);
            currentAccount = null;
        }
    }

    /**
     * 1110-GET-XREF-DATA
     * READ XREF-FILE KEY IS FD-XREF-ACCT-ID INVALID KEY DISPLAY ...
     */
    private void getXrefData(Long acctId) {
        Optional<CardXref> opt = cardXrefRepository.findFirstByXrefAcctId(acctId);
        if (opt.isPresent()) {
            currentCardXref = opt.get();
            log.debug("1110-GET-XREF-DATA found card={} for acct={}", currentCardXref.getXrefCardNum(), acctId);
        } else {
            log.warn("1110-GET-XREF-DATA ACCOUNT NOT FOUND IN XREF: {}", acctId);
            currentCardXref = null;
        }
    }

    /**
     * 1200-GET-INTEREST-RATE (with 1200-A-GET-DEFAULT-INT-RATE fallback).
     *
     * <p>COBOL logic:
     * <pre>
     *   READ DISCGRP-FILE KEY FD-DISCGRP-KEY
     *   IF STATUS = '23' (not found)
     *     MOVE 'DEFAULT' TO FD-DIS-ACCT-GROUP-ID
     *     1200-A-GET-DEFAULT-INT-RATE
     * </pre>
     */
    public BigDecimal getInterestRate(String acctGroupId, String tranTypeCd, Integer tranCatCd) {

        Optional<DisclosureGroup> opt = disclosureGroupRepository
                .findById_DisAcctGroupIdAndId_DisTranTypeCdAndId_DisTranCatCd(
                        acctGroupId, tranTypeCd, tranCatCd);

        if (opt.isPresent()) {
            return opt.get().getDisIntRate();
        }

        // Not found — try DEFAULT group (mirrors DISCGRP-STATUS = '23' fallback)
        log.debug("1200-GET-INTEREST-RATE not found for group={}, trying DEFAULT", acctGroupId);
        Optional<DisclosureGroup> defaultOpt = disclosureGroupRepository
                .findById_DisAcctGroupIdAndId_DisTranTypeCdAndId_DisTranCatCd(
                        DEFAULT_GROUP_ID, tranTypeCd, tranCatCd);

        if (defaultOpt.isPresent()) {
            return defaultOpt.get().getDisIntRate();
        }

        log.warn("1200-GET-INTEREST-RATE DISCLOSURE GROUP RECORD MISSING for group={} type={} cat={}",
                acctGroupId, tranTypeCd, tranCatCd);
        return BigDecimal.ZERO;
    }

    /**
     * 1300-COMPUTE-INTEREST
     * COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
     * ADD WS-MONTHLY-INT TO WS-TOTAL-INT
     * PERFORM 1300-B-WRITE-TX
     */
    private Transaction computeInterestAndWriteTx(TransactionCategoryBalance tcatBal, BigDecimal intRate) {

        BigDecimal balance = tcatBal.getTranCatBal() != null ? tcatBal.getTranCatBal() : BigDecimal.ZERO;
        BigDecimal monthlyInterest = balance.multiply(intRate)
                                            .divide(INTEREST_DIVISOR, 2, RoundingMode.HALF_UP);

        totalInterest = totalInterest.add(monthlyInterest);
        log.debug("1300-COMPUTE-INTEREST acct={} bal={} rate={} monthlyInt={} totalInt={}",
                tcatBal.getId().getTranscatAcctId(), balance, intRate, monthlyInterest, totalInterest);

        return writeTx(tcatBal, monthlyInterest);
    }

    /**
     * 1300-B-WRITE-TX
     * Builds and persists the interest Transaction record.
     *
     * <p>COBOL:
     * <pre>
     *   STRING PARM-DATE, WS-TRANID-SUFFIX INTO TRAN-ID
     *   MOVE '01'  TO TRAN-TYPE-CD
     *   MOVE '05'  TO TRAN-CAT-CD
     *   MOVE 'System' TO TRAN-SOURCE
     *   STRING 'Int. for a/c ', ACCT-ID INTO TRAN-DESC
     *   MOVE WS-MONTHLY-INT TO TRAN-AMT
     *   MOVE XREF-CARD-NUM TO TRAN-CARD-NUM
     *   MOVE DB2-FORMAT-TS  TO TRAN-ORIG-TS, TRAN-PROC-TS
     *   WRITE FD-TRANFILE-REC FROM TRAN-RECORD
     * </pre>
     */
    private Transaction writeTx(TransactionCategoryBalance tcatBal, BigDecimal monthlyInterest) {

        long suffix = tranIdSuffix.incrementAndGet();
        // STRING PARM-DATE, WS-TRANID-SUFFIX DELIMITED BY SIZE INTO TRAN-ID
        String tranId = String.format("%-10s%06d", parmDate != null ? parmDate : "", suffix);
        if (tranId.length() > 16) {
            tranId = tranId.substring(0, 16);
        }

        Long acctId = tcatBal.getId().getTranscatAcctId();
        String cardNum = currentCardXref != null ? currentCardXref.getXrefCardNum() : "";
        LocalDateTime now = LocalDateTime.now();

        Transaction tx = Transaction.builder()
                .tranId(tranId)
                .tranTypeCd(INTEREST_TYPE_CD)
                .tranCatCd(INTEREST_CAT_CD)
                .tranSource(TRAN_SOURCE)
                .tranDesc(String.format("Int. for a/c %011d", acctId))
                .tranAmt(monthlyInterest)
                .tranMerchantId(0L)
                .tranMerchantName("")
                .tranMerchantCity("")
                .tranMerchantZip("")
                .tranCardNum(cardNum)
                .tranOrigTs(now)
                .tranProcTs(now)
                .build();

        log.debug("1300-B-WRITE-TX tranId={} amt={} card={}", tranId, monthlyInterest, cardNum);
        return tx;
    }

    /**
     * 1400-COMPUTE-FEES — stub, to be implemented (mirrors original COBOL comment).
     */
    private void computeFees(TransactionCategoryBalance tcatBal) {
        // TO BE IMPLEMENTED — mirrors "1400-COMPUTE-FEES. EXIT." in CBACT04C.cbl
        log.trace("1400-COMPUTE-FEES stub for acct={}", tcatBal.getId().getTranscatAcctId());
    }

    // ---------------------------------------------------------------
    // Getters for state inspection (used in tests / listeners)
    // ---------------------------------------------------------------

    public Long getLastAcctNum()       { return lastAcctNum; }
    public BigDecimal getTotalInterest() { return totalInterest; }
    public AccountData getCurrentAccount() { return currentAccount; }
    public CardXref getCurrentCardXref()   { return currentCardXref; }
    public boolean isFirstTime()           { return firstTime; }
}
