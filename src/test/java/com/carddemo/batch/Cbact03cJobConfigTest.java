package com.carddemo.batch;

import com.carddemo.model.AccountData;
import com.carddemo.model.DisclosureGroup;
import com.carddemo.repository.DisclosureGroupRepository;
import com.carddemo.service.Cbact03cService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the CBACT03C Spring Batch migration.
 *
 * <h2>Test coverage</h2>
 * <ul>
 *   <li>{@link Cbact03cService#computeMonthlyInterest} — exact COBOL formula</li>
 *   <li>{@link Cbact03cService#lookupInterestRate} — specific + DEFAULT group fallback</li>
 *   <li>{@link Cbact03cItemProcessor#process} — full processor pipeline</li>
 *   <li>Edge cases: zero rate, null balance, inactive accounts</li>
 * </ul>
 *
 * <h2>Reference COBOL formula (1300-COMPUTE-INTEREST in CBACT04C)</h2>
 * <pre>
 *   COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
 * </pre>
 * The {@code ROUNDED} clause → {@link RoundingMode#HALF_UP} on 2 decimal places.
 */
@ExtendWith(MockitoExtension.class)
class Cbact03cJobConfigTest {

    // -----------------------------------------------------------------------
    // Test fixtures
    // -----------------------------------------------------------------------

    private static final String ACCT_GROUP_ID  = "PREMIUM";
    private static final String TRAN_TYPE_CD   = "01";
    private static final int    TRAN_CAT_CD    = 5;

    // -----------------------------------------------------------------------
    // Mocks & subjects
    // -----------------------------------------------------------------------

    @Mock
    private DisclosureGroupRepository disclosureGroupRepository;

    @InjectMocks
    private Cbact03cService cbact03cService;

    // Processor uses the real service (spied) so we test the full chain
    private Cbact03cItemProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new Cbact03cItemProcessor(cbact03cService);
    }

    // =======================================================================
    // Cbact03cService — computeMonthlyInterest
    // =======================================================================

    @Nested
    @DisplayName("Cbact03cService: computeMonthlyInterest (COBOL formula)")
    class ComputeMonthlyInterestTests {

        /**
         * Canonical COBOL test:
         *   TRAN-CAT-BAL = 1000.00, DIS-INT-RATE = 18.00 (18 % APR)
         *   WS-MONTHLY-INT = (1000.00 × 18.00) / 1200 = 18000.00 / 1200 = 15.00
         */
        @Test
        @DisplayName("Standard: balance=1000, rate=18% APR → monthly interest=15.00")
        void standardInterestCalculation() {
            BigDecimal balance  = new BigDecimal("1000.00");
            BigDecimal rate     = new BigDecimal("18.00");

            BigDecimal result = cbact03cService.computeMonthlyInterest(balance, rate);

            assertThat(result).isEqualByComparingTo(new BigDecimal("15.00"));
        }

        /**
         * COBOL ROUNDED clause test — half-up rounding at 2nd decimal place.
         *   balance = 100.00, rate = 5.99
         *   = (100.00 × 5.99) / 1200 = 599.00 / 1200 = 0.499166... → rounds to 0.50
         */
        @Test
        @DisplayName("ROUNDED clause: half-up rounding at 2 decimal places")
        void roundingHalfUp() {
            BigDecimal balance = new BigDecimal("100.00");
            BigDecimal rate    = new BigDecimal("5.99");

            // exact: 599 / 1200 = 0.49916666... → HALF_UP → 0.50
            BigDecimal expected = new BigDecimal("599.00")
                    .divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP);

            BigDecimal result = cbact03cService.computeMonthlyInterest(balance, rate);

            assertThat(result).isEqualByComparingTo(expected);
        }

        /**
         * Guard condition: IF DIS-INT-RATE NOT = 0 → skip computation → return ZERO.
         */
        @Test
        @DisplayName("Guard: zero rate returns ZERO (no interest charged)")
        void zeroRateReturnsZero() {
            BigDecimal result = cbact03cService.computeMonthlyInterest(
                    new BigDecimal("5000.00"), BigDecimal.ZERO);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        /** Null rate treated as zero — defensive null-safety. */
        @Test
        @DisplayName("Guard: null rate returns ZERO (null-safe)")
        void nullRateReturnsZero() {
            BigDecimal result = cbact03cService.computeMonthlyInterest(
                    new BigDecimal("5000.00"), null);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        /** Negative balance (credit balance) yields negative interest — correct accounting. */
        @Test
        @DisplayName("Negative balance yields negative interest (credit balance case)")
        void negativeBalanceYieldsNegativeInterest() {
            BigDecimal balance = new BigDecimal("-200.00");
            BigDecimal rate    = new BigDecimal("12.00");

            // (-200 × 12) / 1200 = -2400 / 1200 = -2.00
            BigDecimal result = cbact03cService.computeMonthlyInterest(balance, rate);

            assertThat(result).isEqualByComparingTo(new BigDecimal("-2.00"));
        }

        /** Large balance stress test — no overflow with BigDecimal. */
        @Test
        @DisplayName("Large balance: no arithmetic overflow")
        void largeBalance() {
            BigDecimal balance = new BigDecimal("9999999999.99"); // max S9(10)V99
            BigDecimal rate    = new BigDecimal("99.99");

            BigDecimal result = cbact03cService.computeMonthlyInterest(balance, rate);

            // Should not throw; result should be positive and non-zero
            assertThat(result).isGreaterThan(BigDecimal.ZERO);
        }
    }

    // =======================================================================
    // Cbact03cService — lookupInterestRate
    // =======================================================================

    @Nested
    @DisplayName("Cbact03cService: lookupInterestRate (rate table lookup)")
    class LookupInterestRateTests {

        @Test
        @DisplayName("Specific group found — returns DIS-INT-RATE directly")
        void specificGroupFound() {
            DisclosureGroup dg = buildDisclosureGroup(ACCT_GROUP_ID, "18.00");
            when(disclosureGroupRepository.findByIdDisAcctGroupIdAndIdDisTranTypeCdAndIdDisTranCatCd(
                    ACCT_GROUP_ID, TRAN_TYPE_CD, TRAN_CAT_CD))
                    .thenReturn(Optional.of(dg));

            BigDecimal rate = cbact03cService.lookupInterestRate(
                    ACCT_GROUP_ID, TRAN_TYPE_CD, TRAN_CAT_CD);

            assertThat(rate).isEqualByComparingTo("18.00");
            // DEFAULT lookup should NOT be called
            verify(disclosureGroupRepository, times(1))
                    .findByIdDisAcctGroupIdAndIdDisTranTypeCdAndIdDisTranCatCd(
                            anyString(), anyString(), anyInt());
        }

        /**
         * 1200-A-GET-DEFAULT-INT-RATE fallback:
         * When specific group not found (DISCGRP-STATUS='23'), retry with 'DEFAULT'.
         */
        @Test
        @DisplayName("Fallback to DEFAULT group when specific group not found")
        void fallbackToDefaultGroup() {
            DisclosureGroup defaultDg = buildDisclosureGroup(
                    Cbact03cService.DEFAULT_GROUP_ID, "12.00");

            when(disclosureGroupRepository.findByIdDisAcctGroupIdAndIdDisTranTypeCdAndIdDisTranCatCd(
                    ACCT_GROUP_ID, TRAN_TYPE_CD, TRAN_CAT_CD))
                    .thenReturn(Optional.empty());

            when(disclosureGroupRepository.findByIdDisAcctGroupIdAndIdDisTranTypeCdAndIdDisTranCatCd(
                    Cbact03cService.DEFAULT_GROUP_ID, TRAN_TYPE_CD, TRAN_CAT_CD))
                    .thenReturn(Optional.of(defaultDg));

            BigDecimal rate = cbact03cService.lookupInterestRate(
                    ACCT_GROUP_ID, TRAN_TYPE_CD, TRAN_CAT_CD);

            assertThat(rate).isEqualByComparingTo("12.00");
            verify(disclosureGroupRepository, times(2))
                    .findByIdDisAcctGroupIdAndIdDisTranTypeCdAndIdDisTranCatCd(
                            anyString(), anyString(), anyInt());
        }

        /** Neither specific nor DEFAULT group found → return ZERO (no interest). */
        @Test
        @DisplayName("Neither specific nor DEFAULT group found → returns ZERO")
        void neitherGroupFound() {
            when(disclosureGroupRepository.findByIdDisAcctGroupIdAndIdDisTranTypeCdAndIdDisTranCatCd(
                    anyString(), anyString(), anyInt()))
                    .thenReturn(Optional.empty());

            BigDecimal rate = cbact03cService.lookupInterestRate(
                    ACCT_GROUP_ID, TRAN_TYPE_CD, TRAN_CAT_CD);

            assertThat(rate).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // =======================================================================
    // Cbact03cItemProcessor — process()
    // =======================================================================

    @Nested
    @DisplayName("Cbact03cItemProcessor: process() — full pipeline")
    class ItemProcessorTests {

        /**
         * Main happy path: active account, 18% APR, balance 1000.00
         * Expected: balance updated to 1015.00, cycle accumulators reset.
         */
        @Test
        @DisplayName("Active account: interest added to balance, cycle accumulators reset")
        void activeAccountInterestApplied() throws Exception {
            // Setup: disclosure group with 18% APR
            DisclosureGroup dg = buildDisclosureGroup(ACCT_GROUP_ID, "18.00");
            when(disclosureGroupRepository.findByIdDisAcctGroupIdAndIdDisTranTypeCdAndIdDisTranCatCd(
                    ACCT_GROUP_ID, TRAN_TYPE_CD, TRAN_CAT_CD))
                    .thenReturn(Optional.of(dg));

            AccountData account = buildActiveAccount(1L, "1000.00", ACCT_GROUP_ID);
            account.setAcctCurrCycCredit(new BigDecimal("200.00"));
            account.setAcctCurrCycDebit(new BigDecimal("50.00"));

            AccountData result = processor.process(account);

            assertThat(result).isNotNull();
            // 1000 + (1000 × 18) / 1200 = 1000 + 15.00 = 1015.00
            assertThat(result.getAcctCurrBal()).isEqualByComparingTo("1015.00");
            // Cycle accumulators reset to 0 (1050-UPDATE-ACCOUNT)
            assertThat(result.getAcctCurrCycCredit()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getAcctCurrCycDebit()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        /** Zero-rate account: balance unchanged, cycle accumulators still reset. */
        @Test
        @DisplayName("Zero interest rate: balance unchanged, accumulators reset")
        void zeroRateAccountNotCharged() throws Exception {
            when(disclosureGroupRepository.findByIdDisAcctGroupIdAndIdDisTranTypeCdAndIdDisTranCatCd(
                    anyString(), anyString(), anyInt()))
                    .thenReturn(Optional.empty()); // triggers zero-rate path

            AccountData account = buildActiveAccount(2L, "500.00", ACCT_GROUP_ID);
            account.setAcctCurrCycCredit(new BigDecimal("100.00"));
            account.setAcctCurrCycDebit(new BigDecimal("30.00"));

            AccountData result = processor.process(account);

            assertThat(result).isNotNull();
            assertThat(result.getAcctCurrBal()).isEqualByComparingTo("500.00"); // unchanged
            assertThat(result.getAcctCurrCycCredit()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getAcctCurrCycDebit()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        /** Inactive account: processor returns null → JpaItemWriter skips write. */
        @Test
        @DisplayName("Inactive account: processor returns null (skip write)")
        void inactiveAccountSkipped() throws Exception {
            AccountData account = buildActiveAccount(3L, "750.00", ACCT_GROUP_ID);
            account.setAcctActiveStatus("N"); // inactive

            AccountData result = processor.process(account);

            assertThat(result).isNull();
            verifyNoInteractions(disclosureGroupRepository); // no rate lookup performed
        }

        /**
         * Regression test: high-precision balance with HALF_UP rounding.
         * balance=333.33, rate=6.00
         * = (333.33 × 6) / 1200 = 1999.98 / 1200 = 1.66665 → HALF_UP → 1.67
         * updated balance = 333.33 + 1.67 = 335.00
         */
        @Test
        @DisplayName("Rounding regression: balance=333.33 rate=6% → interest=1.67 HALF_UP")
        void roundingRegression() throws Exception {
            DisclosureGroup dg = buildDisclosureGroup(ACCT_GROUP_ID, "6.00");
            when(disclosureGroupRepository.findByIdDisAcctGroupIdAndIdDisTranTypeCdAndIdDisTranCatCd(
                    ACCT_GROUP_ID, TRAN_TYPE_CD, TRAN_CAT_CD))
                    .thenReturn(Optional.of(dg));

            AccountData account = buildActiveAccount(4L, "333.33", ACCT_GROUP_ID);

            AccountData result = processor.process(account);

            assertThat(result).isNotNull();
            // (333.33 × 6) / 1200 = 1999.98 / 1200 = 1.66665 → 1.67
            assertThat(result.getAcctCurrBal()).isEqualByComparingTo("335.00");
        }

        /** Null balance treated as ZERO — no NPE. */
        @Test
        @DisplayName("Null balance treated as ZERO — no NullPointerException")
        void nullBalanceTreatedAsZero() throws Exception {
            DisclosureGroup dg = buildDisclosureGroup(ACCT_GROUP_ID, "18.00");
            when(disclosureGroupRepository.findByIdDisAcctGroupIdAndIdDisTranTypeCdAndIdDisTranCatCd(
                    ACCT_GROUP_ID, TRAN_TYPE_CD, TRAN_CAT_CD))
                    .thenReturn(Optional.of(dg));

            AccountData account = buildActiveAccount(5L, null, ACCT_GROUP_ID);

            AccountData result = processor.process(account);

            assertThat(result).isNotNull();
            // (0.00 × 18) / 1200 = 0.00; updated balance = 0.00
            assertThat(result.getAcctCurrBal()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // =======================================================================
    // Helper builders
    // =======================================================================

    private static AccountData buildActiveAccount(Long id,
                                                   String balance,
                                                   String groupId) {
        return AccountData.builder()
                .acctId(id)
                .acctActiveStatus("Y")
                .acctCurrBal(balance != null ? new BigDecimal(balance) : null)
                .acctCreditLimit(new BigDecimal("10000.00"))
                .acctCashCreditLimit(new BigDecimal("5000.00"))
                .acctOpenDate(LocalDate.of(2020, 1, 1))
                .acctGroupId(groupId)
                .acctCurrCycCredit(BigDecimal.ZERO)
                .acctCurrCycDebit(BigDecimal.ZERO)
                .build();
    }

    private static DisclosureGroup buildDisclosureGroup(String groupId, String rate) {
        DisclosureGroup.DisclosureGroupKey key = DisclosureGroup.DisclosureGroupKey.builder()
                .disAcctGroupId(groupId)
                .disTranTypeCd(TRAN_TYPE_CD)
                .disTranCatCd(TRAN_CAT_CD)
                .build();
        return DisclosureGroup.builder()
                .id(key)
                .disIntRate(new BigDecimal(rate))
                .build();
    }
}
