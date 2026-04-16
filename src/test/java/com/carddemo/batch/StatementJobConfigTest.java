package com.carddemo.batch;

import com.carddemo.batch.StatementJobConfig.AccountDataProvider;
import com.carddemo.batch.StatementJobConfig.CardXrefDataProvider;
import com.carddemo.batch.StatementJobConfig.CustomerDataProvider;
import com.carddemo.batch.StatementJobConfig.StatementHeaderHolder;
import com.carddemo.batch.StatementJobConfig.TransactionDataProvider;
import com.carddemo.model.AccountData;
import com.carddemo.model.CardXref;
import com.carddemo.model.CustomerData;
import com.carddemo.model.StatementRecord;
import com.carddemo.model.TransactionData;
import com.carddemo.service.StatementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StatementJobConfig, StatementService, and model classes.
 *
 * Test coverage mirrors CBSTM03A/CBSTM03B logic:
 *  - Statement header building (5000-CREATE-STATEMENT)
 *  - Transaction enrichment and subtotaling (4000-TRNXFILE-GET, ADD TRNX-AMT TO WS-TOTAL-AMT)
 *  - Currency formatting (PIC Z(9).99- picture clause)
 *  - Transaction line formatting (ST-LINE14, 6000-WRITE-TRANS)
 *  - Total line formatting (ST-LINE14A)
 *  - Full statement text generation (buildPlainTextStatement)
 *  - StatementRecord BigDecimal scale-2 enforcement
 *  - StatementHeaderHolder lifecycle
 *  - Processor abend logic (9999-ABEND-PROGRAM equivalent)
 */
class StatementJobConfigTest {

    // ── shared fixtures ────────────────────────────────────────────────

    private StatementService service;
    private StatementJobConfig config;

    private CardXrefDataProvider  xrefProvider;
    private CustomerDataProvider  custProvider;
    private AccountDataProvider   acctProvider;
    private TransactionDataProvider txnProvider;

    @BeforeEach
    void setUp() {
        service      = new StatementService();
        xrefProvider = mock(CardXrefDataProvider.class);
        custProvider = mock(CustomerDataProvider.class);
        acctProvider = mock(AccountDataProvider.class);
        txnProvider  = mock(TransactionDataProvider.class);

        config = new StatementJobConfig(service, xrefProvider, custProvider,
                                        acctProvider, txnProvider);
        StatementHeaderHolder.clear();
    }

    // ──────────────────────────────────────────────────────────────────
    //  1. Model: TransactionData
    // ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("TransactionData model")
    class TransactionDataTests {

        @Test
        @DisplayName("setTransactionAmount enforces scale 2")
        void scaleEnforcedOnAmount() {
            TransactionData txn = new TransactionData();
            txn.setTransactionAmount(new BigDecimal("123.456789"));
            assertThat(txn.getTransactionAmount().scale()).isEqualTo(2);
            assertThat(txn.getTransactionAmount()).isEqualByComparingTo("123.46");
        }

        @Test
        @DisplayName("Constructor sets scale 2 on amount")
        void constructorSetsScale() {
            TransactionData txn = new TransactionData(
                "1234567890123456", "TXN001", "Coffee", new BigDecimal("9.999"));
            assertThat(txn.getTransactionAmount().scale()).isEqualTo(2);
            assertThat(txn.getTransactionAmount()).isEqualByComparingTo("10.00");
        }

        @Test
        @DisplayName("setTransactionAmount with null is handled")
        void nullAmountHandled() {
            TransactionData txn = new TransactionData();
            txn.setTransactionAmount(null);
            assertThat(txn.getTransactionAmount()).isNull();
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  2. Model: AccountData
    // ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AccountData model")
    class AccountDataTests {

        @Test
        @DisplayName("setCurrentBalance enforces scale 2")
        void balanceScale() {
            AccountData acct = new AccountData();
            acct.setCurrentBalance(new BigDecimal("500.123"));
            assertThat(acct.getCurrentBalance().scale()).isEqualTo(2);
            assertThat(acct.getCurrentBalance()).isEqualByComparingTo("500.12");
        }

        @Test
        @DisplayName("Negative balance preserved with scale 2")
        void negativeBalance() {
            AccountData acct = new AccountData("00000000001",
                new BigDecimal("-1234.50"), new BigDecimal("5000.00"));
            assertThat(acct.getCurrentBalance()).isEqualByComparingTo("-1234.50");
            assertThat(acct.getCurrentBalance().signum()).isEqualTo(-1);
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  3. Model: StatementRecord – subtotal accumulation
    // ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("StatementRecord subtotaling")
    class StatementRecordTests {

        @Test
        @DisplayName("addTransaction accumulates totalTransactionAmount (mirrors ADD TRNX-AMT TO WS-TOTAL-AMT)")
        void subtotalAccumulation() {
            StatementRecord stmt = new StatementRecord("CARD01", "ACCT01", "John Doe");
            stmt.addTransaction(makeTxn("T001", new BigDecimal("100.00")));
            stmt.addTransaction(makeTxn("T002", new BigDecimal("50.25")));
            stmt.addTransaction(makeTxn("T003", new BigDecimal("0.01")));

            assertThat(stmt.getTotalTransactionAmount())
                .isEqualByComparingTo("150.26");
            assertThat(stmt.getTotalTransactionAmount().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("Negative transaction decrements total (credit refund scenario)")
        void negativeTransactionDecrement() {
            StatementRecord stmt = new StatementRecord();
            stmt.addTransaction(makeTxn("T001", new BigDecimal("200.00")));
            stmt.addTransaction(makeTxn("T002", new BigDecimal("-50.00")));

            assertThat(stmt.getTotalTransactionAmount())
                .isEqualByComparingTo("150.00");
        }

        @Test
        @DisplayName("setTotalTransactionAmount null resets to zero scale 2")
        void nullResetsToZero() {
            StatementRecord stmt = new StatementRecord();
            stmt.setTotalTransactionAmount(null);
            assertThat(stmt.getTotalTransactionAmount())
                .isEqualByComparingTo(BigDecimal.ZERO)
                .matches(v -> v.scale() == 2);
        }

        @Test
        @DisplayName("Transactions list is populated in order")
        void transactionOrder() {
            StatementRecord stmt = new StatementRecord();
            stmt.addTransaction(makeTxn("T001", BigDecimal.ONE));
            stmt.addTransaction(makeTxn("T002", BigDecimal.TEN));
            assertThat(stmt.getTransactions())
                .extracting(TransactionData::getTransactionId)
                .containsExactly("T001", "T002");
        }

        private TransactionData makeTxn(String id, BigDecimal amount) {
            TransactionData t = new TransactionData();
            t.setTransactionId(id);
            t.setTransactionAmount(amount);
            t.setTransactionDesc("Test transaction " + id);
            return t;
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  4. StatementService – formatting
    // ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("StatementService formatting")
    class StatementServiceFormattingTests {

        @Test
        @DisplayName("formatCurrency: positive amount → no trailing minus")
        void formatCurrencyPositive() {
            String result = service.formatCurrency(new BigDecimal("123.45"));
            assertThat(result).endsWith(" ");
            assertThat(result).doesNotContain("-");
            assertThat(result).contains("123.45");
        }

        @Test
        @DisplayName("formatCurrency: negative amount → trailing minus (mirrors PIC Z(9).99-)")
        void formatCurrencyNegative() {
            String result = service.formatCurrency(new BigDecimal("-123.45"));
            assertThat(result).endsWith("-");
            assertThat(result).contains("123.45");
        }

        @Test
        @DisplayName("formatCurrency: zero → spaces then 0.00 no minus")
        void formatCurrencyZero() {
            String result = service.formatCurrency(BigDecimal.ZERO.setScale(2));
            assertThat(result).doesNotContain("-");
        }

        @Test
        @DisplayName("formatCurrency: null → 13-space string")
        void formatCurrencyNull() {
            String result = service.formatCurrency(null);
            assertThat(result).hasSize(13);
            assertThat(result).isBlank();
        }

        @Test
        @DisplayName("formatTransactionLine: correct width 80 chars (ST-LINE14)")
        void formatTransactionLineWidth() {
            TransactionData txn = new TransactionData(
                "CARD0000000000001", "TXN0000000000001",
                "Coffee shop purchase", new BigDecimal("4.99"));
            String line = service.formatTransactionLine(txn);
            // tranId(16) + space(1) + desc(49) + $(1) + currency(13) = 80
            assertThat(line).hasSize(80);
        }

        @Test
        @DisplayName("formatTotalLine: contains 'Total EXP:' and formatted amount (ST-LINE14A)")
        void formatTotalLine() {
            String line = service.formatTotalLine(new BigDecimal("1234.56"));
            assertThat(line).contains("Total EXP:");
            assertThat(line).contains("1234.56");
        }

        @Test
        @DisplayName("padRight: pads to exact width")
        void padRight() {
            assertThat(service.padRight("ABC", 10)).isEqualTo("ABC       ");
            assertThat(service.padRight("ABCDEFGHIJKLM", 10)).isEqualTo("ABCDEFGHIJ");
            assertThat(service.padRight(null, 5)).isEqualTo("     ");
        }

        @Test
        @DisplayName("buildStatementHeader: maps AccountData + CustomerData correctly")
        void buildStatementHeader() {
            AccountData  acct = new AccountData("00000000001",
                new BigDecimal("500.00"), new BigDecimal("5000.00"));
            CustomerData cust = new CustomerData("123456789",
                "John", "M", "Doe",
                "123 Main St", "Apt 4B", "Seattle", "WA", "USA", "98101", 750);

            StatementRecord stmt = service.buildStatementHeader("CARD0000000000001", acct, cust);

            assertThat(stmt.getAccountId()).isEqualTo("00000000001");
            assertThat(stmt.getCurrentBalance()).isEqualByComparingTo("500.00");
            assertThat(stmt.getCustomerName()).isEqualTo("John M Doe");
            assertThat(stmt.getAddressLine1()).isEqualTo("123 Main St");
            assertThat(stmt.getFicoScore()).isEqualTo("750");
        }

        @Test
        @DisplayName("buildPlainTextStatement: contains START and END markers")
        void buildPlainTextStatementMarkers() {
            StatementRecord stmt = buildSampleStatement();
            List<String> lines = service.buildPlainTextStatement(stmt);

            assertThat(lines).anySatisfy(l -> assertThat(l).contains("START OF STATEMENT"));
            assertThat(lines).anySatisfy(l -> assertThat(l).contains("END OF STATEMENT"));
        }

        @Test
        @DisplayName("buildPlainTextStatement: contains TRANSACTION SUMMARY header")
        void buildPlainTextStatementTxnHeader() {
            StatementRecord stmt = buildSampleStatement();
            List<String> lines = service.buildPlainTextStatement(stmt);
            assertThat(lines).anySatisfy(l -> assertThat(l).contains("TRANSACTION SUMMARY"));
        }

        @Test
        @DisplayName("buildPlainTextStatement: contains Total EXP line (ST-LINE14A)")
        void buildPlainTextStatementTotalLine() {
            StatementRecord stmt = buildSampleStatement();
            List<String> lines = service.buildPlainTextStatement(stmt);
            assertThat(lines).anySatisfy(l -> assertThat(l).contains("Total EXP:"));
        }

        @Test
        @DisplayName("buildPlainTextStatement: total matches sum of transactions (grand total logic)")
        void grandTotalMatchesSum() {
            StatementRecord stmt = buildSampleStatement();
            // Manually verify total is sum of all txn amounts
            BigDecimal expected = stmt.getTransactions().stream()
                .map(TransactionData::getTransactionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, java.math.RoundingMode.HALF_UP);
            assertThat(stmt.getTotalTransactionAmount())
                .isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("enrichWithTransactions: resets total before accumulating")
        void enrichResetsTotal() {
            StatementRecord stmt = new StatementRecord();
            stmt.setTotalTransactionAmount(new BigDecimal("999.99"));

            TransactionData t = new TransactionData();
            t.setTransactionId("T1");
            t.setTransactionAmount(new BigDecimal("10.00"));
            t.setTransactionDesc("Test");

            service.enrichWithTransactions(stmt, List.of(t));

            assertThat(stmt.getTotalTransactionAmount()).isEqualByComparingTo("10.00");
            assertThat(stmt.getTransactions()).hasSize(1);
        }

        private StatementRecord buildSampleStatement() {
            StatementRecord stmt = new StatementRecord("CARD0000000000001",
                "00000000001", "John M Doe");
            stmt.setAddressLine1("123 Main St");
            stmt.setAddressLine2("Apt 4B");
            stmt.setAddressLine3("Seattle WA USA 98101");
            stmt.setCurrentBalance(new BigDecimal("500.00"));
            stmt.setFicoScore("750");

            service.enrichWithTransactions(stmt, List.of(
                txn("T001", "Coffee shop",   new BigDecimal("4.50")),
                txn("T002", "Grocery store", new BigDecimal("87.30")),
                txn("T003", "Gas station",   new BigDecimal("45.00"))
            ));
            return stmt;
        }

        private TransactionData txn(String id, String desc, BigDecimal amount) {
            TransactionData t = new TransactionData();
            t.setTransactionId(id);
            t.setTransactionDesc(desc);
            t.setTransactionAmount(amount);
            t.setCardNumber("CARD0000000000001");
            return t;
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  5. StatementJobConfig – processor and header holder
    // ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("StatementJobConfig processor logic")
    class ProcessorTests {

        @Test
        @DisplayName("statementHeaderProcessor: builds header when customer + account found")
        void processorBuildsHeader() throws Exception {
            CardXref xref = new CardXref("CARD0000000000001", "CUST00001", "00000000001");
            CustomerData cust = new CustomerData("CUST00001", "Alice", null, "Smith",
                "1 Pike Place", "", "Seattle", "WA", "USA", "98101", 780);
            AccountData acct = new AccountData("00000000001",
                new BigDecimal("1200.00"), new BigDecimal("10000.00"));

            when(custProvider.findByCustomerId("CUST00001")).thenReturn(cust);
            when(acctProvider.findByAccountId("00000000001")).thenReturn(acct);

            StatementRecord result = config.statementHeaderProcessor().process(xref);

            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo("00000000001");
            assertThat(result.getCustomerName()).isEqualTo("Alice Smith");
            assertThat(result.getCurrentBalance()).isEqualByComparingTo("1200.00");
        }

        @Test
        @DisplayName("statementHeaderProcessor: throws when customer is missing (mirrors 9999-ABEND)")
        void processorAbendsOnMissingCustomer() {
            CardXref xref = new CardXref("CARD0000000000001", "CUST_MISSING", "00000000001");
            when(custProvider.findByCustomerId("CUST_MISSING")).thenReturn(null);
            when(acctProvider.findByAccountId("00000000001"))
                .thenReturn(new AccountData("00000000001", BigDecimal.ZERO, BigDecimal.ZERO));

            assertThatThrownBy(() -> config.statementHeaderProcessor().process(xref))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing data");
        }

        @Test
        @DisplayName("statementHeaderProcessor: throws when account is missing (mirrors 9999-ABEND)")
        void processorAbendsOnMissingAccount() {
            CardXref xref = new CardXref("CARD0000000000001", "CUST00001", "ACCT_MISSING");
            when(custProvider.findByCustomerId("CUST00001"))
                .thenReturn(new CustomerData("CUST00001", "Bob", null, "Jones",
                    "2 Elm St", "", "Portland", "OR", "USA", "97201", 700));
            when(acctProvider.findByAccountId("ACCT_MISSING")).thenReturn(null);

            assertThatThrownBy(() -> config.statementHeaderProcessor().process(xref))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing data");
        }

        @Test
        @DisplayName("StatementHeaderHolder: clear resets list between job runs")
        void headerHolderClear() {
            StatementHeaderHolder.getHeaders().add(
                new StatementRecord("C", "A", "Name"));
            assertThat(StatementHeaderHolder.getHeaders()).hasSize(1);

            StatementHeaderHolder.clear();
            assertThat(StatementHeaderHolder.getHeaders()).isEmpty();
        }

        @Test
        @DisplayName("statementLineItemProcessor: enriches header with transactions and builds lines")
        void lineItemProcessorEnriches() throws Exception {
            StatementRecord header = new StatementRecord("CARD0000000000001",
                "00000000001", "Alice Smith");
            header.setAddressLine1("1 Pike Place");
            header.setAddressLine2("");
            header.setAddressLine3("Seattle WA USA 98101");
            header.setCurrentBalance(new BigDecimal("500.00"));
            header.setFicoScore("780");

            TransactionData t1 = new TransactionData("CARD0000000000001",
                "TXN001", "Espresso", new BigDecimal("3.50"));
            TransactionData t2 = new TransactionData("CARD0000000000001",
                "TXN002", "Lunch",    new BigDecimal("12.75"));

            when(txnProvider.findByCardNumber("CARD0000000000001"))
                .thenReturn(List.of(t1, t2));

            List<String> lines = config.statementLineItemProcessor().process(header);

            assertThat(lines).isNotNull();
            assertThat(lines).anySatisfy(l -> assertThat(l).contains("START OF STATEMENT"));
            assertThat(lines).anySatisfy(l -> assertThat(l).contains("TXN001"));
            assertThat(lines).anySatisfy(l -> assertThat(l).contains("TXN002"));
            assertThat(lines).anySatisfy(l -> assertThat(l).contains("Total EXP:"));

            // Total = 3.50 + 12.75 = 16.25
            assertThat(header.getTotalTransactionAmount())
                .isEqualByComparingTo("16.25");
        }
    }
}
