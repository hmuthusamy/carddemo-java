package com.carddemo.batch;

import com.carddemo.model.AccountData;
import com.carddemo.model.TranCatBal;
import com.carddemo.model.TransactionData;
import com.carddemo.repository.AccountDataRepository;
import com.carddemo.repository.TranCatBalRepository;
import com.carddemo.service.Cbtrn02cService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link Cbtrn02cService} – verifying balance update logic from CBTRN02C.CBL.
 *
 * <p>All test scenarios are derived directly from the COBOL source paragraphs:</p>
 * <ul>
 *   <li>{@code 2800-UPDATE-ACCOUNT-REC} – account balance fields</li>
 *   <li>{@code 2700-UPDATE-TCATBAL} / {@code 2700-A} / {@code 2700-B} – category balance</li>
 *   <li>{@code 2000-POST-TRANSACTION} – end-to-end posting</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Cbtrn02cService – COBOL CBTRN02C.CBL migration tests")
class Cbtrn02cJobConfigTest {

    // ── mocks ────────────────────────────────────────────────────────────────
    @Mock private AccountDataRepository  accountDataRepository;
    @Mock private TranCatBalRepository   tranCatBalRepository;

    @InjectMocks
    private Cbtrn02cService service;

    // ── fixture helpers ──────────────────────────────────────────────────────
    private static final Long   ACCT_ID      = 12345678901L;
    private static final String TYPE_CD      = "SA";
    private static final int    CAT_CD       = 1001;

    private AccountData buildAccount(BigDecimal currBal,
                                     BigDecimal cycCredit,
                                     BigDecimal cycDebit,
                                     BigDecimal creditLimit) {
        return AccountData.builder()
                .acctId(ACCT_ID)
                .acctActiveStatus("Y")
                .acctCurrBal(currBal)
                .acctCurrCycCredit(cycCredit)
                .acctCurrCycDebit(cycDebit)
                .acctCreditLimit(creditLimit)
                .acctExpirationDate(LocalDate.now().plusYears(2))
                .build();
    }

    private TransactionData buildTran(BigDecimal amt) {
        return TransactionData.builder()
                .tranId("TRAN0000000000001")
                .tranCardNum("4111111111111111")
                .tranAcctId(ACCT_ID)
                .tranTypeCd(TYPE_CD)
                .tranCatCd(CAT_CD)
                .tranAmt(amt)
                .tranStatus("APPROVED")
                .tranOrigTs(LocalDateTime.now())
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2800-UPDATE-ACCOUNT-REC  – account balance tests
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("2800-UPDATE-ACCOUNT-REC – account balance update")
    class UpdateAccountBalanceTests {

        @Test
        @DisplayName("Positive amount: ACCT-CURR-BAL increases and ACCT-CURR-CYC-CREDIT increases")
        void positiveTransaction_increasesCurrBalAndCycCredit() {
            // Mirrors COBOL:
            //   ADD DALYTRAN-AMT TO ACCT-CURR-BAL          (100 + 50 = 150)
            //   IF DALYTRAN-AMT >= 0
            //      ADD DALYTRAN-AMT TO ACCT-CURR-CYC-CREDIT (30 + 50 = 80)
            AccountData account = buildAccount(
                    bd("100.00"), bd("30.00"), bd("0.00"), bd("5000.00"));
            TransactionData tran = buildTran(bd("50.00"));

            service.updateAccountBalance(tran, account);

            assertThat(account.getAcctCurrBal())
                    .as("acctCurrBal should be 100 + 50 = 150")
                    .isEqualByComparingTo(bd("150.00"));
            assertThat(account.getAcctCurrCycCredit())
                    .as("acctCurrCycCredit should be 30 + 50 = 80")
                    .isEqualByComparingTo(bd("80.00"));
            assertThat(account.getAcctCurrCycDebit())
                    .as("acctCurrCycDebit should be unchanged")
                    .isEqualByComparingTo(bd("0.00"));
        }

        @Test
        @DisplayName("Negative amount: ACCT-CURR-BAL decreases and ACCT-CURR-CYC-DEBIT decreases")
        void negativeTransaction_decreasesCurrBalAndCycDebit() {
            // Mirrors COBOL:
            //   ADD DALYTRAN-AMT TO ACCT-CURR-BAL       (100 + (-25) = 75)
            //   ELSE
            //      ADD DALYTRAN-AMT TO ACCT-CURR-CYC-DEBIT  (0 + (-25) = -25)
            AccountData account = buildAccount(
                    bd("100.00"), bd("50.00"), bd("0.00"), bd("5000.00"));
            TransactionData tran = buildTran(bd("-25.00"));

            service.updateAccountBalance(tran, account);

            assertThat(account.getAcctCurrBal())
                    .as("acctCurrBal should be 100 - 25 = 75")
                    .isEqualByComparingTo(bd("75.00"));
            assertThat(account.getAcctCurrCycDebit())
                    .as("acctCurrCycDebit should be 0 + (-25) = -25")
                    .isEqualByComparingTo(bd("-25.00"));
            assertThat(account.getAcctCurrCycCredit())
                    .as("acctCurrCycCredit should be unchanged")
                    .isEqualByComparingTo(bd("50.00"));
        }

        @Test
        @DisplayName("Zero amount: ACCT-CURR-BAL and ACCT-CURR-CYC-CREDIT unchanged (>= 0 branch)")
        void zeroAmount_treatedAsPositiveBranch_noChange() {
            AccountData account = buildAccount(
                    bd("200.00"), bd("100.00"), bd("0.00"), bd("5000.00"));
            TransactionData tran = buildTran(BigDecimal.ZERO);

            service.updateAccountBalance(tran, account);

            assertThat(account.getAcctCurrBal())
                    .isEqualByComparingTo(bd("200.00"));
            assertThat(account.getAcctCurrCycCredit())
                    .isEqualByComparingTo(bd("100.00"));
        }

        @Test
        @DisplayName("HALF_UP rounding preserved: 0.005 rounds to 0.01")
        void rounding_halfUp() {
            // COBOL ROUNDED clause uses HALF_UP
            AccountData account = buildAccount(
                    bd("100.000"), bd("0.00"), bd("0.00"), bd("5000.00"));
            TransactionData tran = buildTran(new BigDecimal("0.005"));

            service.updateAccountBalance(tran, account);

            assertThat(account.getAcctCurrBal())
                    .isEqualByComparingTo(bd("100.01"));
        }

        @Test
        @DisplayName("Null balance fields treated as ZERO (COBOL INITIALIZE)")
        void nullBalanceFields_treatedAsZero() {
            AccountData account = AccountData.builder()
                    .acctId(ACCT_ID)
                    .acctCurrBal(null)
                    .acctCurrCycCredit(null)
                    .acctCurrCycDebit(null)
                    .acctCreditLimit(bd("5000.00"))
                    .build();
            TransactionData tran = buildTran(bd("75.00"));

            assertThatCode(() -> service.updateAccountBalance(tran, account))
                    .doesNotThrowAnyException();
            assertThat(account.getAcctCurrBal()).isEqualByComparingTo(bd("75.00"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2700-UPDATE-TCATBAL – category balance tests
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("2700-UPDATE-TCATBAL – transaction category balance update")
    class UpdateTranCatBalTests {

        @Test
        @DisplayName("Existing record: ADD DALYTRAN-AMT TO TRAN-CAT-BAL (REWRITE path)")
        void existingRecord_addsAmountToBalance() {
            // 2700-B-UPDATE-TCATBAL-REC: ADD DALYTRAN-AMT TO TRAN-CAT-BAL + REWRITE
            TranCatBal existing = TranCatBal.builder()
                    .id(1L)
                    .trancatAcctId(ACCT_ID)
                    .trancatTypeCd(TYPE_CD)
                    .trancatCd(CAT_CD)
                    .tranCatBal(bd("500.00"))
                    .build();
            when(tranCatBalRepository.findByTrancatAcctIdAndTrancatTypeCdAndTrancatCd(
                    ACCT_ID, TYPE_CD, CAT_CD))
                    .thenReturn(Optional.of(existing));
            when(tranCatBalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TransactionData tran = buildTran(bd("75.50"));
            service.updateTranCatBal(tran);

            ArgumentCaptor<TranCatBal> captor = ArgumentCaptor.forClass(TranCatBal.class);
            verify(tranCatBalRepository).save(captor.capture());
            assertThat(captor.getValue().getTranCatBal())
                    .as("500.00 + 75.50 = 575.50")
                    .isEqualByComparingTo(bd("575.50"));
        }

        @Test
        @DisplayName("Missing record: INITIALIZE + WRITE creates new with amount (CREATE path)")
        void missingRecord_createsNewRecord() {
            // 2700-A-CREATE-TCATBAL-REC: INITIALIZE (zero) then ADD DALYTRAN-AMT
            when(tranCatBalRepository.findByTrancatAcctIdAndTrancatTypeCdAndTrancatCd(
                    ACCT_ID, TYPE_CD, CAT_CD))
                    .thenReturn(Optional.empty());
            when(tranCatBalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TransactionData tran = buildTran(bd("123.45"));
            service.updateTranCatBal(tran);

            ArgumentCaptor<TranCatBal> captor = ArgumentCaptor.forClass(TranCatBal.class);
            verify(tranCatBalRepository).save(captor.capture());
            TranCatBal saved = captor.getValue();
            assertThat(saved.getTranCatBal())
                    .as("New record: 0.00 + 123.45 = 123.45")
                    .isEqualByComparingTo(bd("123.45"));
            assertThat(saved.getTrancatAcctId()).isEqualTo(ACCT_ID);
            assertThat(saved.getTrancatTypeCd()).isEqualTo(TYPE_CD);
            assertThat(saved.getTrancatCd()).isEqualTo(CAT_CD);
        }

        @Test
        @DisplayName("Negative amount decreases category balance (e.g. refund)")
        void negativeAmount_decreasesCategoryBalance() {
            TranCatBal existing = TranCatBal.builder()
                    .id(2L)
                    .trancatAcctId(ACCT_ID)
                    .trancatTypeCd("CR").trancatCd(2001)
                    .tranCatBal(bd("1000.00"))
                    .build();
            when(tranCatBalRepository.findByTrancatAcctIdAndTrancatTypeCdAndTrancatCd(
                    ACCT_ID, "CR", 2001))
                    .thenReturn(Optional.of(existing));
            when(tranCatBalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TransactionData tran = buildTran(bd("-200.00"));
            tran.setTranTypeCd("CR");
            tran.setTranCatCd(2001);
            service.updateTranCatBal(tran);

            ArgumentCaptor<TranCatBal> captor = ArgumentCaptor.forClass(TranCatBal.class);
            verify(tranCatBalRepository).save(captor.capture());
            assertThat(captor.getValue().getTranCatBal())
                    .as("1000.00 + (-200.00) = 800.00")
                    .isEqualByComparingTo(bd("800.00"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2000-POST-TRANSACTION – end-to-end tests
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("2000-POST-TRANSACTION – end-to-end posting")
    class PostTransactionTests {

        @BeforeEach
        void stubSaves() {
            lenient().when(accountDataRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            lenient().when(tranCatBalRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("Full posting: account balance updated, status set to POSTED, proc timestamp set")
        void fullPosting_updatesAccountAndSetsPostedStatus() {
            AccountData account = buildAccount(
                    bd("1000.00"), bd("200.00"), bd("0.00"), bd("5000.00"));
            when(accountDataRepository.findById(ACCT_ID)).thenReturn(Optional.of(account));
            when(tranCatBalRepository.findByTrancatAcctIdAndTrancatTypeCdAndTrancatCd(
                    any(), any(), any())).thenReturn(Optional.empty());

            TransactionData tran = buildTran(bd("150.00"));
            AccountData result = service.postTransaction(tran);

            // Account balance updated
            assertThat(result.getAcctCurrBal())
                    .as("1000 + 150 = 1150")
                    .isEqualByComparingTo(bd("1150.00"));
            assertThat(result.getAcctCurrCycCredit())
                    .as("200 + 150 = 350")
                    .isEqualByComparingTo(bd("350.00"));

            // Transaction marked POSTED
            assertThat(tran.getTranStatus()).isEqualTo("POSTED");
            assertThat(tran.getTranProcTs()).isNotNull();

            // Both repos called
            verify(accountDataRepository).save(account);
            verify(tranCatBalRepository).save(any(TranCatBal.class));
        }

        @Test
        @DisplayName("Account not found throws TransactionPostingException")
        void accountNotFound_throwsException() {
            when(accountDataRepository.findById(ACCT_ID)).thenReturn(Optional.empty());
            when(tranCatBalRepository.findByTrancatAcctIdAndTrancatTypeCdAndTrancatCd(
                    any(), any(), any())).thenReturn(Optional.empty());

            TransactionData tran = buildTran(bd("50.00"));

            assertThatThrownBy(() -> service.postTransaction(tran))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account not found");
        }

        @Test
        @DisplayName("Negative amount (refund): cycle debit updated instead of credit")
        void refundTransaction_updatesCycleDebit() {
            AccountData account = buildAccount(
                    bd("500.00"), bd("500.00"), bd("0.00"), bd("5000.00"));
            when(accountDataRepository.findById(ACCT_ID)).thenReturn(Optional.of(account));
            when(tranCatBalRepository.findByTrancatAcctIdAndTrancatTypeCdAndTrancatCd(
                    any(), any(), any())).thenReturn(Optional.empty());

            TransactionData tran = buildTran(bd("-100.00"));
            AccountData result = service.postTransaction(tran);

            assertThat(result.getAcctCurrBal())
                    .as("500 - 100 = 400")
                    .isEqualByComparingTo(bd("400.00"));
            assertThat(result.getAcctCurrCycDebit())
                    .as("0 + (-100) = -100")
                    .isEqualByComparingTo(bd("-100.00"));
            assertThat(result.getAcctCurrCycCredit())
                    .as("cycCredit unchanged")
                    .isEqualByComparingTo(bd("500.00"));
        }

        @Test
        @DisplayName("Large transaction with precise HALF_UP rounding")
        void largeTransaction_precisionRounding() {
            // Test that BigDecimal arithmetic stays correct at scale
            AccountData account = buildAccount(
                    bd("9999999.99"), bd("5000000.00"), bd("0.00"), bd("99999999.99"));
            when(accountDataRepository.findById(ACCT_ID)).thenReturn(Optional.of(account));
            when(tranCatBalRepository.findByTrancatAcctIdAndTrancatTypeCdAndTrancatCd(
                    any(), any(), any())).thenReturn(Optional.empty());

            TransactionData tran = buildTran(new BigDecimal("0.005")); // HALF_UP → 0.01
            AccountData result = service.postTransaction(tran);

            assertThat(result.getAcctCurrBal().scale()).isEqualTo(2);
            assertThat(result.getAcctCurrBal())
                    .isEqualByComparingTo(new BigDecimal("10000000.00"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helper
    // ════════════════════════════════════════════════════════════════════════
    private static BigDecimal bd(String val) {
        return new BigDecimal(val).setScale(2, RoundingMode.HALF_UP);
    }
}
