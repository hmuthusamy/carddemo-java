package com.carddemo.batch;

import com.carddemo.model.*;
import com.carddemo.repository.*;
import com.carddemo.service.Cbact04cService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Cbact04cJobConfigTest – unit tests for the CBACT04C Spring Batch migration.
 *
 * <p>Tests are structured in two layers:
 * <ol>
 *   <li>Pure unit tests for {@link Cbact04cService} (no Spring context, fast Mockito tests).</li>
 *   <li>Integration smoke tests for the Job / Step wiring.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class Cbact04cJobConfigTest {

    // =========================================================================
    // Fixtures
    // =========================================================================

    private static final Long   ACCT_ID       = 12345678901L;
    private static final Long   ACCT_ID_2     = 98765432100L;
    private static final String CARD_NUM      = "1234567890123456";
    private static final String PARM_DATE     = "2024-01-15";
    private static final String TYPE_CD       = "01";
    private static final int    CAT_CD        = 5;
    private static final String GROUP_ID      = "GRPA      ";
    private static final BigDecimal INT_RATE  = new BigDecimal("12.00");   // 12% annual
    private static final BigDecimal BALANCE   = new BigDecimal("1000.00");
    // monthly = 1000 * 12 / 1200 = 10.00
    private static final BigDecimal MONTHLY_INT = new BigDecimal("10.00");

    // =========================================================================
    // Service unit tests
    // =========================================================================

    @Nested
    @DisplayName("Cbact04cService – unit tests")
    class ServiceTests {

        @Mock AccountDataRepository      accountDataRepository;
        @Mock CardXrefRepository         cardXrefRepository;
        @Mock DisclosureGroupRepository  disclosureGroupRepository;
        @Mock TransactionRepository      transactionRepository;

        @InjectMocks
        Cbact04cService service;

        @BeforeEach
        void setUp() {
            service.resetState(PARM_DATE);
        }

        // -------------------------------------------------------------------
        // resetState
        // -------------------------------------------------------------------

        @Test
        @DisplayName("resetState initialises all working-storage variables")
        void resetState_initialisesVariables() {
            assertThat(service.isFirstTime()).isTrue();
            assertThat(service.getLastAcctNum()).isNull();
            assertThat(service.getTotalInterest()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(service.getCurrentAccount()).isNull();
            assertThat(service.getCurrentCardXref()).isNull();
        }

        // -------------------------------------------------------------------
        // getInterestRate – found in specific group
        // -------------------------------------------------------------------

        @Test
        @DisplayName("getInterestRate returns rate when specific group exists")
        void getInterestRate_specificGroupFound() {
            DisclosureGroup.DisclosureGroupKey key =
                    new DisclosureGroup.DisclosureGroupKey(GROUP_ID, TYPE_CD, CAT_CD);
            DisclosureGroup dg = new DisclosureGroup(key, INT_RATE);
            given(disclosureGroupRepository
                .findById_DisAcctGroupIdAndId_DisTranTypeCdAndId_DisTranCatCd(
                        GROUP_ID, TYPE_CD, CAT_CD))
                .willReturn(Optional.of(dg));

            BigDecimal result = service.getInterestRate(GROUP_ID, TYPE_CD, CAT_CD);

            assertThat(result).isEqualByComparingTo(INT_RATE);
            then(disclosureGroupRepository).should(never())
                    .findById_DisAcctGroupIdAndId_DisTranTypeCdAndId_DisTranCatCd(
                            eq("DEFAULT"), anyString(), anyInt());
        }

        // -------------------------------------------------------------------
        // getInterestRate – fallback to DEFAULT group (mirrors COBOL status 23)
        // -------------------------------------------------------------------

        @Test
        @DisplayName("getInterestRate falls back to DEFAULT when specific group not found")
        void getInterestRate_fallsBackToDefault() {
            DisclosureGroup.DisclosureGroupKey defaultKey =
                    new DisclosureGroup.DisclosureGroupKey("DEFAULT", TYPE_CD, CAT_CD);
            DisclosureGroup defaultDg = new DisclosureGroup(defaultKey, new BigDecimal("8.00"));

            given(disclosureGroupRepository
                .findById_DisAcctGroupIdAndId_DisTranTypeCdAndId_DisTranCatCd(
                        GROUP_ID, TYPE_CD, CAT_CD))
                .willReturn(Optional.empty());
            given(disclosureGroupRepository
                .findById_DisAcctGroupIdAndId_DisTranTypeCdAndId_DisTranCatCd(
                        "DEFAULT", TYPE_CD, CAT_CD))
                .willReturn(Optional.of(defaultDg));

            BigDecimal result = service.getInterestRate(GROUP_ID, TYPE_CD, CAT_CD);

            assertThat(result).isEqualByComparingTo(new BigDecimal("8.00"));
        }

        // -------------------------------------------------------------------
        // getInterestRate – neither group found → returns zero
        // -------------------------------------------------------------------

        @Test
        @DisplayName("getInterestRate returns ZERO when no group record found")
        void getInterestRate_returnsZeroWhenNotFound() {
            given(disclosureGroupRepository
                .findById_DisAcctGroupIdAndId_DisTranTypeCdAndId_DisTranCatCd(
                        anyString(), anyString(), anyInt()))
                .willReturn(Optional.empty());

            BigDecimal result = service.getInterestRate(GROUP_ID, TYPE_CD, CAT_CD);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        // -------------------------------------------------------------------
        // processTransactionCategoryBalance – first record (firstTime='Y' path)
        // -------------------------------------------------------------------

        @Test
        @DisplayName("First record: firstTime flag cleared, account and xref loaded, no prior account updated")
        void process_firstRecord_noUpdateAccount() {
            stubAccountAndXref();
            stubDisclosureGroup(INT_RATE);

            TransactionCategoryBalance tcatBal = buildTcatBal(ACCT_ID, BALANCE);
            List<Transaction> txns = service.processTransactionCategoryBalance(tcatBal);

            // firstTime was true so no save should have been called yet for account
            // (updateAccount is only called on boundary crossing after firstTime=false)
            then(accountDataRepository).should(never()).save(any());

            // One interest transaction generated
            assertThat(txns).hasSize(1);
            Transaction tx = txns.get(0);
            assertThat(tx.getTranTypeCd()).isEqualTo("01");
            assertThat(tx.getTranCatCd()).isEqualTo("05");
            assertThat(tx.getTranSource()).isEqualTo("System");
            assertThat(tx.getTranAmt()).isEqualByComparingTo(MONTHLY_INT);
            assertThat(tx.getTranCardNum()).isEqualTo(CARD_NUM);
            assertThat(tx.getTranDesc()).contains("Int. for a/c");
        }

        // -------------------------------------------------------------------
        // processTransactionCategoryBalance – same account, second record
        // -------------------------------------------------------------------

        @Test
        @DisplayName("Second record on same account: interest accumulates, no extra account load")
        void process_secondRecordSameAccount_accumulatesInterest() {
            stubAccountAndXref();
            stubDisclosureGroup(INT_RATE);

            TransactionCategoryBalance tcatBal1 = buildTcatBal(ACCT_ID, BALANCE);
            TransactionCategoryBalance tcatBal2 = buildTcatBal(ACCT_ID, new BigDecimal("500.00"));

            service.processTransactionCategoryBalance(tcatBal1);
            service.processTransactionCategoryBalance(tcatBal2);

            // Account lookup should have occurred only once (first boundary crossing)
            then(accountDataRepository).should(times(1)).findById(ACCT_ID);
            then(cardXrefRepository).should(times(1)).findFirstByXrefAcctId(ACCT_ID);

            // totalInterest = 10.00 + 5.00 = 15.00
            // 500 * 12 / 1200 = 5.00
            assertThat(service.getTotalInterest()).isEqualByComparingTo(new BigDecimal("15.00"));
        }

        // -------------------------------------------------------------------
        // processTransactionCategoryBalance – account boundary triggers update
        // -------------------------------------------------------------------

        @Test
        @DisplayName("Account boundary: previous account balance updated, new account loaded")
        void process_accountBoundary_updatesAndLoadsNew() {
            // Stub for both accounts
            AccountData acct1 = buildAccount(ACCT_ID, GROUP_ID, new BigDecimal("500.00"));
            AccountData acct2 = buildAccount(ACCT_ID_2, GROUP_ID, new BigDecimal("200.00"));
            CardXref xref1 = new CardXref(CARD_NUM, 111L, ACCT_ID);
            CardXref xref2 = new CardXref("9999999999999999", 222L, ACCT_ID_2);

            given(accountDataRepository.findById(ACCT_ID)).willReturn(Optional.of(acct1));
            given(accountDataRepository.findById(ACCT_ID_2)).willReturn(Optional.of(acct2));
            given(cardXrefRepository.findFirstByXrefAcctId(ACCT_ID)).willReturn(Optional.of(xref1));
            given(cardXrefRepository.findFirstByXrefAcctId(ACCT_ID_2)).willReturn(Optional.of(xref2));
            stubDisclosureGroup(INT_RATE);

            TransactionCategoryBalance tcatBal1 = buildTcatBal(ACCT_ID, BALANCE);
            TransactionCategoryBalance tcatBal2 = buildTcatBal(ACCT_ID_2, BALANCE);

            service.processTransactionCategoryBalance(tcatBal1);  // firstTime → false
            service.processTransactionCategoryBalance(tcatBal2);  // triggers boundary

            // Account 1 should have been saved with updated balance
            ArgumentCaptor<AccountData> captor = ArgumentCaptor.forClass(AccountData.class);
            then(accountDataRepository).should(atLeastOnce()).save(captor.capture());
            AccountData saved = captor.getValue();
            assertThat(saved.getAcctId()).isEqualTo(ACCT_ID);
            // 500.00 + 10.00 = 510.00
            assertThat(saved.getAcctCurrBal()).isEqualByComparingTo(new BigDecimal("510.00"));
            assertThat(saved.getAcctCurrCycCredit()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getAcctCurrCycDebit()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        // -------------------------------------------------------------------
        // processTransactionCategoryBalance – zero interest rate → no transaction
        // -------------------------------------------------------------------

        @Test
        @DisplayName("Zero interest rate: no transaction written (mirrors DIS-INT-RATE = 0 check)")
        void process_zeroInterestRate_noTransaction() {
            stubAccountAndXref();
            stubDisclosureGroup(BigDecimal.ZERO);

            TransactionCategoryBalance tcatBal = buildTcatBal(ACCT_ID, BALANCE);
            List<Transaction> txns = service.processTransactionCategoryBalance(tcatBal);

            assertThat(txns).isEmpty();
            assertThat(service.getTotalInterest()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        // -------------------------------------------------------------------
        // processTransactionCategoryBalance – account not found in XREF
        // -------------------------------------------------------------------

        @Test
        @DisplayName("Xref not found: transaction still generated with empty card number")
        void process_xrefNotFound_emptyCardNum() {
            AccountData acct = buildAccount(ACCT_ID, GROUP_ID, BigDecimal.ZERO);
            given(accountDataRepository.findById(ACCT_ID)).willReturn(Optional.of(acct));
            given(cardXrefRepository.findFirstByXrefAcctId(ACCT_ID)).willReturn(Optional.empty());
            stubDisclosureGroup(INT_RATE);

            TransactionCategoryBalance tcatBal = buildTcatBal(ACCT_ID, BALANCE);
            List<Transaction> txns = service.processTransactionCategoryBalance(tcatBal);

            assertThat(txns).hasSize(1);
            assertThat(txns.get(0).getTranCardNum()).isEmpty();
        }

        // -------------------------------------------------------------------
        // finalizeLastAccount
        // -------------------------------------------------------------------

        @Test
        @DisplayName("finalizeLastAccount updates account after last record")
        void finalizeLastAccount_updatesAccount() {
            stubAccountAndXref();
            stubDisclosureGroup(INT_RATE);

            TransactionCategoryBalance tcatBal = buildTcatBal(ACCT_ID, BALANCE);
            service.processTransactionCategoryBalance(tcatBal);

            // No save yet (firstTime was cleared, but no second account triggered boundary)
            then(accountDataRepository).should(never()).save(any());

            service.finalizeLastAccount();

            then(accountDataRepository).should(times(1)).save(any(AccountData.class));
        }

        @Test
        @DisplayName("finalizeLastAccount does nothing when no records processed")
        void finalizeLastAccount_doesNothingWhenEmpty() {
            // resetState called in @BeforeEach — firstTime=true, currentAccount=null
            service.finalizeLastAccount();
            then(accountDataRepository).should(never()).save(any());
        }

        // -------------------------------------------------------------------
        // TRAN-ID format matches COBOL STRING PARM-DATE, WS-TRANID-SUFFIX
        // -------------------------------------------------------------------

        @Test
        @DisplayName("Generated TRAN-ID starts with parmDate and ends with suffix")
        void tranId_matchesCobolStringPattern() {
            stubAccountAndXref();
            stubDisclosureGroup(INT_RATE);

            TransactionCategoryBalance tcatBal = buildTcatBal(ACCT_ID, BALANCE);
            List<Transaction> txns = service.processTransactionCategoryBalance(tcatBal);

            assertThat(txns).hasSize(1);
            String tranId = txns.get(0).getTranId();
            // Must be exactly 16 chars
            assertThat(tranId).hasSize(16);
            // Must start with the parm date
            assertThat(tranId).startsWith(PARM_DATE);
            // Must end with "000001"
            assertThat(tranId).endsWith("000001");
        }

        // -------------------------------------------------------------------
        // Multiple records — suffix increments correctly
        // -------------------------------------------------------------------

        @Test
        @DisplayName("TRAN-ID suffix increments for each interest transaction")
        void tranIdSuffix_incrementsPerTransaction() {
            stubAccountAndXref();
            stubDisclosureGroup(INT_RATE);

            TransactionCategoryBalance tcatBal1 = buildTcatBal(ACCT_ID, BALANCE);
            TransactionCategoryBalance tcatBal2 = buildTcatBal(ACCT_ID,
                    new BigDecimal("2000.00"),
                    TYPE_CD, CAT_CD + 1);  // different category

            List<Transaction> txns1 = service.processTransactionCategoryBalance(tcatBal1);
            List<Transaction> txns2 = service.processTransactionCategoryBalance(tcatBal2);

            assertThat(txns1.get(0).getTranId()).endsWith("000001");
            assertThat(txns2.get(0).getTranId()).endsWith("000002");
        }

        // -------------------------------------------------------------------
        // TRAN-DESC format matches COBOL: "Int. for a/c <ACCT-ID>"
        // -------------------------------------------------------------------

        @Test
        @DisplayName("TRAN-DESC matches COBOL format 'Int. for a/c <acct-id>'")
        void tranDesc_matchesCobolFormat() {
            stubAccountAndXref();
            stubDisclosureGroup(INT_RATE);

            TransactionCategoryBalance tcatBal = buildTcatBal(ACCT_ID, BALANCE);
            List<Transaction> txns = service.processTransactionCategoryBalance(tcatBal);

            assertThat(txns.get(0).getTranDesc())
                    .isEqualTo(String.format("Int. for a/c %011d", ACCT_ID));
        }

        // -------------------------------------------------------------------
        // Interest computation accuracy
        // -------------------------------------------------------------------

        @Test
        @DisplayName("Monthly interest = (balance * rate) / 1200 rounded HALF_UP")
        void interestComputation_accuracy() {
            stubAccountAndXref();
            // rate = 15.5%, balance = 1234.56
            // monthly = 1234.56 * 15.5 / 1200 = 15.95 (rounded HALF_UP)
            BigDecimal rate    = new BigDecimal("15.50");
            BigDecimal balance = new BigDecimal("1234.56");
            stubDisclosureGroup(rate);

            TransactionCategoryBalance tcatBal = buildTcatBal(ACCT_ID, balance);
            List<Transaction> txns = service.processTransactionCategoryBalance(tcatBal);

            // 1234.56 * 15.50 / 1200 = 19134.68 / 1200 = 15.9455... → 15.95
            assertThat(txns.get(0).getTranAmt()).isEqualByComparingTo(new BigDecimal("15.95"));
        }

        // -------------------------------------------------------------------
        // Helpers
        // -------------------------------------------------------------------

        private void stubAccountAndXref() {
            AccountData acct = buildAccount(ACCT_ID, GROUP_ID, BigDecimal.ZERO);
            CardXref xref    = new CardXref(CARD_NUM, 100L, ACCT_ID);
            given(accountDataRepository.findById(ACCT_ID)).willReturn(Optional.of(acct));
            given(cardXrefRepository.findFirstByXrefAcctId(ACCT_ID)).willReturn(Optional.of(xref));
        }

        private void stubDisclosureGroup(BigDecimal rate) {
            // Return the given rate for any key query
            given(disclosureGroupRepository
                .findById_DisAcctGroupIdAndId_DisTranTypeCdAndId_DisTranCatCd(
                        anyString(), anyString(), anyInt()))
                .willReturn(Optional.of(
                        new DisclosureGroup(
                                new DisclosureGroup.DisclosureGroupKey(GROUP_ID, TYPE_CD, CAT_CD),
                                rate)));
        }

        private AccountData buildAccount(Long acctId, String groupId, BigDecimal balance) {
            AccountData a = new AccountData();
            a.setAcctId(acctId);
            a.setAcctGroupId(groupId);
            a.setAcctCurrBal(balance);
            a.setAcctCurrCycCredit(BigDecimal.ZERO);
            a.setAcctCurrCycDebit(BigDecimal.ZERO);
            return a;
        }

        private TransactionCategoryBalance buildTcatBal(Long acctId, BigDecimal balance) {
            return buildTcatBal(acctId, balance, TYPE_CD, CAT_CD);
        }

        private TransactionCategoryBalance buildTcatBal(Long acctId, BigDecimal balance,
                                                        String typeCd, int catCd) {
            TransactionCategoryBalance.TransactionCategoryKey key =
                    new TransactionCategoryBalance.TransactionCategoryKey(acctId, typeCd, catCd);
            return new TransactionCategoryBalance(key, balance);
        }
    }

    // =========================================================================
    // Job config bean wiring smoke test (no Spring context — pure unit)
    // =========================================================================

    @Nested
    @DisplayName("Cbact04cJobConfig – bean wiring")
    class JobConfigTests {

        @Mock AccountDataRepository      accountDataRepository;
        @Mock CardXrefRepository         cardXrefRepository;
        @Mock DisclosureGroupRepository  disclosureGroupRepository;
        @Mock TransactionRepository      transactionRepository;

        @InjectMocks
        Cbact04cService service;

        @Test
        @DisplayName("Processor delegates to service and returns empty list when rate is zero")
        void processor_zeroRate_returnsEmptyList() throws Exception {
            service.resetState(PARM_DATE);

            AccountData acct = new AccountData();
            acct.setAcctId(ACCT_ID);
            acct.setAcctGroupId("DEFAULT");
            acct.setAcctCurrBal(BigDecimal.ZERO);

            given(accountDataRepository.findById(ACCT_ID)).willReturn(Optional.of(acct));
            given(cardXrefRepository.findFirstByXrefAcctId(ACCT_ID))
                    .willReturn(Optional.of(new CardXref(CARD_NUM, 1L, ACCT_ID)));
            given(disclosureGroupRepository
                    .findById_DisAcctGroupIdAndId_DisTranTypeCdAndId_DisTranCatCd(
                            anyString(), anyString(), anyInt()))
                    .willReturn(Optional.empty());

            TransactionCategoryBalance.TransactionCategoryKey key =
                    new TransactionCategoryBalance.TransactionCategoryKey(ACCT_ID, "01", 5);
            TransactionCategoryBalance item = new TransactionCategoryBalance(key, new BigDecimal("100.00"));

            List<Transaction> result = service.processTransactionCategoryBalance(item);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Processor delegates to service and returns non-empty list when rate > 0")
        void processor_positiveRate_returnsTransactions() throws Exception {
            service.resetState(PARM_DATE);

            AccountData acct = new AccountData();
            acct.setAcctId(ACCT_ID);
            acct.setAcctGroupId("DEFAULT");
            acct.setAcctCurrBal(new BigDecimal("1000.00"));

            given(accountDataRepository.findById(ACCT_ID)).willReturn(Optional.of(acct));
            given(cardXrefRepository.findFirstByXrefAcctId(ACCT_ID))
                    .willReturn(Optional.of(new CardXref(CARD_NUM, 1L, ACCT_ID)));

            DisclosureGroup.DisclosureGroupKey dgKey =
                    new DisclosureGroup.DisclosureGroupKey("DEFAULT", "01", 5);
            given(disclosureGroupRepository
                    .findById_DisAcctGroupIdAndId_DisTranTypeCdAndId_DisTranCatCd(
                            anyString(), anyString(), anyInt()))
                    .willReturn(Optional.of(new DisclosureGroup(dgKey, new BigDecimal("12.00"))));

            TransactionCategoryBalance.TransactionCategoryKey key =
                    new TransactionCategoryBalance.TransactionCategoryKey(ACCT_ID, "01", 5);
            TransactionCategoryBalance item = new TransactionCategoryBalance(key, new BigDecimal("1000.00"));

            List<Transaction> result = service.processTransactionCategoryBalance(item);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTranAmt()).isEqualByComparingTo(new BigDecimal("10.00"));
        }
    }
}
