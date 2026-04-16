package com.carddemo.batch;

import com.carddemo.model.AccountData;
import com.carddemo.service.Cbact03cService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Spring Batch {@link ItemProcessor} for the CBACT03C interest calculation job.
 *
 * <h2>COBOL origin</h2>
 * Migrated from {@code CBACT04C.CBL} — the interest calculator batch that processes
 * account cross-reference data read by CBACT03C.
 *
 * <h2>Processing logic</h2>
 * For each {@link AccountData} item the processor:
 * <ol>
 *   <li>Looks up the monthly interest rate for the account's group using
 *       {@link Cbact03cService#lookupInterestRate} (mirrors 1200-GET-INTEREST-RATE).</li>
 *   <li>Computes monthly interest on the current balance with
 *       {@link Cbact03cService#computeMonthlyInterest} (mirrors 1300-COMPUTE-INTEREST).</li>
 *   <li>Adds the computed interest to {@code acctCurrBal}
 *       (mirrors {@code ADD WS-TOTAL-INT TO ACCT-CURR-BAL} in 1050-UPDATE-ACCOUNT).</li>
 *   <li>Resets cycle credit/debit accumulators to 0
 *       (mirrors {@code MOVE 0 TO ACCT-CURR-CYC-CREDIT / ACCT-CURR-CYC-DEBIT}).</li>
 *   <li>Returns {@code null} for inactive accounts (skips the write).</li>
 * </ol>
 *
 * <h2>COBOL formula</h2>
 * <pre>
 *   COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
 *
 * Java equivalent (in Cbact03cService):
 *   monthlyInterest = balance.multiply(annualRate)
 *                            .divide(1200, 2, RoundingMode.HALF_UP)
 * </pre>
 *
 * <h2>Transaction type / category defaults</h2>
 * The processor uses default values consistent with the COBOL interest transaction record:
 * <ul>
 *   <li>{@code tranTypeCd  = "01"} — matches {@code MOVE '01' TO TRAN-TYPE-CD}</li>
 *   <li>{@code tranCatCd   = 5}    — matches {@code MOVE '05' TO TRAN-CAT-CD}</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Cbact03cItemProcessor implements ItemProcessor<AccountData, AccountData> {

    /**
     * Default transaction type code for interest transactions.
     * COBOL: MOVE '01' TO TRAN-TYPE-CD in 1300-B-WRITE-TX.
     */
    static final String INTEREST_TRAN_TYPE_CD = "01";

    /**
     * Default transaction category code for interest transactions.
     * COBOL: MOVE '05' TO TRAN-CAT-CD in 1300-B-WRITE-TX.
     */
    static final int INTEREST_TRAN_CAT_CD = 5;

    private final Cbact03cService cbact03cService;

    /**
     * Process one account record: look up interest rate, compute monthly interest,
     * update balance, and reset cycle accumulators.
     *
     * @param account the account record to process (never null — Spring Batch guarantee)
     * @return modified account (to be written by JpaItemWriter), or {@code null} to skip
     */
    @Override
    @Nullable
    public AccountData process(@NonNull AccountData account) {

        // Skip inactive accounts (no COBOL equivalent — defensive guard)
        if (!"Y".equalsIgnoreCase(account.getAcctActiveStatus())) {
            log.debug("Skipping inactive account {}", account.getAcctId());
            return null;
        }

        BigDecimal balance = account.getAcctCurrBal();
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }

        // 1200-GET-INTEREST-RATE: look up annual rate from disclosure group table
        BigDecimal annualRate = cbact03cService.lookupInterestRate(
                account.getAcctGroupId(),
                INTEREST_TRAN_TYPE_CD,
                INTEREST_TRAN_CAT_CD);

        // Guard: IF DIS-INT-RATE NOT = 0 (skip compute when rate is zero)
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Zero interest rate for account {} group='{}' — no interest charged",
                    account.getAcctId(), account.getAcctGroupId());
            // Still reset cycle accumulators (1050-UPDATE-ACCOUNT always resets them)
            return resetCycleAccumulators(account);
        }

        // 1300-COMPUTE-INTEREST:
        //   COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
        // Here TRAN-CAT-BAL maps to ACCT-CURR-BAL for the account-level computation.
        BigDecimal monthlyInterest = cbact03cService.computeMonthlyInterest(balance, annualRate);

        // 1050-UPDATE-ACCOUNT:
        //   ADD WS-TOTAL-INT TO ACCT-CURR-BAL
        BigDecimal updatedBalance = balance.add(monthlyInterest);
        account.setAcctCurrBal(updatedBalance);

        log.info("Account {}: balance {} + interest {} (rate {}%/yr) = {}",
                account.getAcctId(), balance, monthlyInterest, annualRate, updatedBalance);

        // MOVE 0 TO ACCT-CURR-CYC-CREDIT / ACCT-CURR-CYC-DEBIT
        return resetCycleAccumulators(account);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Reset cycle credit/debit accumulators to zero.
     * Mirrors:
     * <pre>
     *   MOVE 0 TO ACCT-CURR-CYC-CREDIT
     *   MOVE 0 TO ACCT-CURR-CYC-DEBIT
     * </pre>
     */
    private AccountData resetCycleAccumulators(AccountData account) {
        account.setAcctCurrCycCredit(BigDecimal.ZERO);
        account.setAcctCurrCycDebit(BigDecimal.ZERO);
        return account;
    }
}
