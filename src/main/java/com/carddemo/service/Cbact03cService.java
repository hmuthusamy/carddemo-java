package com.carddemo.service;

import com.carddemo.model.DisclosureGroup;
import com.carddemo.repository.DisclosureGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Interest calculation service migrated from COBOL program CBACT04C (interest calculator batch).
 *
 * <h2>COBOL to Java mapping</h2>
 * <pre>
 * COBOL working-storage:
 *   WS-MONTHLY-INT  PIC S9(09)V99   → BigDecimal (scale=2, HALF_UP rounding)
 *
 * 1300-COMPUTE-INTEREST:
 *   COMPUTE WS-MONTHLY-INT
 *     = ( TRAN-CAT-BAL * DIS-INT-RATE ) / 1200
 *
 * Java equivalent:
 *   monthlyInterest = (tranCatBal × disIntRate) / 1200
 *                     rounded to 2 decimal places with RoundingMode.HALF_UP
 *
 * The divisor 1200 converts an annual percentage rate (APR) stored as a
 * plain number (e.g., 18 = 18 %) into a monthly decimal factor:
 *     APR% / 1200 = APR / 100 / 12  (monthly fraction)
 *
 * Guard condition (IF DIS-INT-RATE NOT = 0):
 *   Interest is only computed when the rate is non-zero.
 * </pre>
 *
 * <h2>Rate table lookup (1200-GET-INTEREST-RATE)</h2>
 * <ol>
 *   <li>Look up by (acctGroupId, tranTypeCd, tranCatCd) in disclosure_group table.</li>
 *   <li>If not found (DISCGRP-STATUS='23'), retry with groupId='DEFAULT'.</li>
 *   <li>If still not found, return ZERO rate — no interest charged.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Cbact03cService {

    /** Divisor that converts annual % rate to monthly fraction (APR% / 1200). */
    static final BigDecimal MONTHLY_DIVISOR = new BigDecimal("1200");

    /** Default group ID — mirrors MOVE 'DEFAULT' TO FD-DIS-ACCT-GROUP-ID in COBOL. */
    public static final String DEFAULT_GROUP_ID = "DEFAULT";

    private final DisclosureGroupRepository disclosureGroupRepository;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Compute monthly interest for a single transaction-category balance.
     *
     * <p>Mirrors COBOL paragraph 1300-COMPUTE-INTEREST:
     * <pre>
     *   COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
     * </pre>
     *
     * COBOL {@code ROUNDED} clause → {@link RoundingMode#HALF_UP} on 2 decimal places.
     *
     * @param tranCatBal transaction-category balance (TRAN-CAT-BAL PIC S9(09)V99)
     * @param annualRatePct annual interest rate percentage (DIS-INT-RATE PIC S9(04)V99)
     * @return monthly interest amount, 2 decimal places, HALF_UP rounded; ZERO if rate is zero
     */
    public BigDecimal computeMonthlyInterest(BigDecimal tranCatBal, BigDecimal annualRatePct) {
        // Guard: IF DIS-INT-RATE NOT = 0
        if (annualRatePct == null || annualRatePct.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Interest rate is zero — skipping computation");
            return BigDecimal.ZERO;
        }

        // COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
        BigDecimal result = tranCatBal
                .multiply(annualRatePct)
                .divide(MONTHLY_DIVISOR, 2, RoundingMode.HALF_UP);

        log.debug("computeMonthlyInterest: bal={} × rate={} / 1200 = {}",
                tranCatBal, annualRatePct, result);
        return result;
    }

    /**
     * Look up the annual interest rate for a given account group / transaction category key.
     *
     * <p>Mirrors COBOL paragraphs 1200-GET-INTEREST-RATE and 1200-A-GET-DEFAULT-INT-RATE:
     * <ol>
     *   <li>Try exact (acctGroupId, tranTypeCd, tranCatCd) key.</li>
     *   <li>If not found, retry with {@code DEFAULT} as the group ID.</li>
     *   <li>If still missing, return {@link BigDecimal#ZERO} — no interest charged.</li>
     * </ol>
     *
     * @param acctGroupId   ACCT-GROUP-ID / DIS-ACCT-GROUP-ID (PIC X(10))
     * @param tranTypeCd    TRANCAT-TYPE-CD / DIS-TRAN-TYPE-CD (PIC X(02))
     * @param tranCatCd     TRANCAT-CD / DIS-TRAN-CAT-CD (PIC 9(04))
     * @return annual interest rate percentage, or ZERO when no rate is defined
     */
    @Transactional(readOnly = true)
    public BigDecimal lookupInterestRate(String acctGroupId, String tranTypeCd, Integer tranCatCd) {

        // 1. Try specific group
        Optional<DisclosureGroup> rate = disclosureGroupRepository
                .findByIdDisAcctGroupIdAndIdDisTranTypeCdAndIdDisTranCatCd(
                        acctGroupId, tranTypeCd, tranCatCd);

        if (rate.isPresent()) {
            log.debug("Interest rate found for group={} type={} cat={}: {}",
                    acctGroupId, tranTypeCd, tranCatCd, rate.get().getDisIntRate());
            return rate.get().getDisIntRate();
        }

        // 2. Fallback: MOVE 'DEFAULT' TO FD-DIS-ACCT-GROUP-ID (1200-A-GET-DEFAULT-INT-RATE)
        log.debug("No rate for group='{}' — trying DEFAULT group", acctGroupId);
        Optional<DisclosureGroup> defaultRate = disclosureGroupRepository
                .findByIdDisAcctGroupIdAndIdDisTranTypeCdAndIdDisTranCatCd(
                        DEFAULT_GROUP_ID, tranTypeCd, tranCatCd);

        if (defaultRate.isPresent()) {
            log.debug("Default interest rate for type={} cat={}: {}",
                    tranTypeCd, tranCatCd, defaultRate.get().getDisIntRate());
            return defaultRate.get().getDisIntRate();
        }

        // 3. No rate found — return zero (no interest charged)
        log.warn("No interest rate (specific or default) for group={} type={} cat={} — using 0",
                acctGroupId, tranTypeCd, tranCatCd);
        return BigDecimal.ZERO;
    }

    /**
     * Compute total monthly interest across all transaction categories for one account.
     *
     * <p>Mirrors the accumulation loop in CBACT04C:
     * <pre>
     *   PERFORM 1300-COMPUTE-INTEREST
     *   ADD WS-MONTHLY-INT TO WS-TOTAL-INT
     * </pre>
     *
     * This overload is provided for convenience when the rate has already been resolved.
     *
     * @param tranCatBal    category balance
     * @param annualRatePct annual rate (already looked up)
     * @param runningTotal  current accumulated interest total
     * @return new running total after adding this category's interest
     */
    public BigDecimal accumulateInterest(BigDecimal tranCatBal,
                                         BigDecimal annualRatePct,
                                         BigDecimal runningTotal) {
        BigDecimal monthlyInterest = computeMonthlyInterest(tranCatBal, annualRatePct);
        return runningTotal.add(monthlyInterest);
    }
}
