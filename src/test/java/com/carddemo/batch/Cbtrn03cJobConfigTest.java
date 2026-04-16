package com.carddemo.batch;

import com.carddemo.model.TransactionData;
import com.carddemo.service.Cbtrn03cService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cbtrn03cJobConfigTest – Unit and integration tests for the CBTRN03C migration.
 *
 * <p>Covers:
 * <ol>
 *   <li>Spring Batch job wiring ({@code cbtrn03cJob} bean, step, reader, processor, writer)</li>
 *   <li>{@link Cbtrn03cService} rejection-reason logic for every rejection code</li>
 *   <li>Happy-path: valid transactions pass without rejection</li>
 *   <li>Edge cases: null/empty fields, boundary dates, zero amounts</li>
 *   <li>Report-line formatting</li>
 *   <li>Sample data builder correctness</li>
 * </ol>
 *
 * <p>Uses an in-memory H2 datasource (see {@code src/test/resources/application.yml})
 * so no external infrastructure is required.
 */
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class Cbtrn03cJobConfigTest {

    // -----------------------------------------------------------------------
    // Spring Batch test utilities (auto-configured by @SpringBatchTest)
    // -----------------------------------------------------------------------

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private Job cbtrn03cJob;

    @Autowired
    private Cbtrn03cService cbtrn03cService;

    @Autowired
    private Cbtrn03cJobConfig cbtrn03cJobConfig;

    @BeforeEach
    void cleanJobRepository() throws Exception {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    // -----------------------------------------------------------------------
    // 1. Job / Step wiring tests
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("1. Spring Batch job wiring")
    class JobWiringTests {

        @Test
        @DisplayName("cbtrn03cJob bean is present and named correctly")
        void jobBeanIsConfigured() {
            assertThat(cbtrn03cJob).isNotNull();
            assertThat(cbtrn03cJob.getName()).isEqualTo("cbtrn03cJob");
        }

        @Test
        @DisplayName("Job completes successfully with default parameters")
        void jobCompletesSuccessfully() throws Exception {
            JobParameters params = new JobParametersBuilder()
                    .addString("startDate", "2024-01-01")
                    .addString("endDate",   "2024-12-31")
                    .toJobParameters();

            JobExecution execution = jobLauncherTestUtils.launchJob(params);

            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        }

        @Test
        @DisplayName("Job reads all sample transactions (read count matches sample data size)")
        void jobReadCountMatchesSampleData() throws Exception {
            int expectedRecords = Cbtrn03cJobConfig.buildSampleTransactions().size();

            JobParameters params = new JobParametersBuilder()
                    .addString("startDate", "2020-01-01")
                    .addString("endDate",   "2030-12-31")
                    .toJobParameters();

            JobExecution execution = jobLauncherTestUtils.launchJob(params);

            long totalRead = execution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getReadCount)
                    .sum();

            assertThat(totalRead).isEqualTo(expectedRecords);
        }

        @Test
        @DisplayName("Job completes with COMPLETED status when no date parameters provided")
        void jobCompletesWithoutDateParameters() throws Exception {
            JobExecution execution = jobLauncherTestUtils.launchJob(
                    new JobParametersBuilder().toJobParameters());

            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        }

        @Test
        @DisplayName("cbtrn03cStep bean is wired and named correctly")
        void stepBeanIsConfigured() {
            Step step = cbtrn03cJobConfig.cbtrn03cStep();
            assertThat(step).isNotNull();
            assertThat(step.getName()).isEqualTo("cbtrn03cStep");
        }
    }

    // -----------------------------------------------------------------------
    // 2. Service – happy path
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("2. Cbtrn03cService – valid transactions pass without rejection")
    class HappyPathTests {

        @Test
        @DisplayName("Valid transaction is not rejected")
        void validTransactionIsNotRejected() {
            TransactionData tx = validTransaction("TXN001");

            TransactionData result = cbtrn03cService.applyRejectionReasonCode(tx);

            assertThat(result.isRejected()).isFalse();
            assertThat(result.getRejectionReasonCode()).isNull();
            assertThat(result.getRejectionReasonDescription()).isNull();
        }

        @Test
        @DisplayName("Valid transaction within date range is not rejected")
        void validTransactionWithinDateRangeIsNotRejected() {
            TransactionData tx = validTransaction("TXN002");
            tx.setTransactionDate(LocalDate.of(2024, 6, 15));

            TransactionData result = cbtrn03cService.applyRejectionReasonCode(
                    tx,
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31));

            assertThat(result.isRejected()).isFalse();
        }

        @ParameterizedTest(name = "Valid type code [{0}] is accepted")
        @ValueSource(strings = {"PR", "DB", "CR", "RF", "FE", "IN"})
        @DisplayName("All known transaction type codes are accepted")
        void allKnownTypeCodesAccepted(String typeCode) {
            TransactionData tx = validTransaction("TXN-TYPE-" + typeCode);
            tx.setTransactionTypeCode(typeCode);

            TransactionData result = cbtrn03cService.applyRejectionReasonCode(tx);

            assertThat(result.isRejected())
                    .as("Type code [%s] should be valid", typeCode)
                    .isFalse();
        }

        @Test
        @DisplayName("Transaction type description is populated from reference data")
        void transactionTypeDescriptionPopulated() {
            TransactionData tx = validTransaction("TXN003");
            tx.setTransactionTypeCode("PR");
            tx.setTransactionTypeDescription(null); // not pre-set

            cbtrn03cService.applyRejectionReasonCode(tx);

            assertThat(tx.getTransactionTypeDescription()).isEqualTo("Purchase");
        }

        @Test
        @DisplayName("Transaction category description is populated from reference data")
        void transactionCategoryDescriptionPopulated() {
            TransactionData tx = validTransaction("TXN004");
            tx.setTransactionCategoryDescription(null); // not pre-set

            cbtrn03cService.applyRejectionReasonCode(tx);

            assertThat(tx.getTransactionCategoryDescription()).isEqualTo("Retail");
        }
    }

    // -----------------------------------------------------------------------
    // 3. Service – rejection reason codes
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("3. Cbtrn03cService – rejection reason codes")
    class RejectionReasonCodeTests {

        @Test
        @DisplayName("R001: null card number triggers rejection")
        void nullCardNumberTriggersR001() {
            TransactionData tx = validTransaction("TXN-R001-NULL");
            tx.setCardNumber(null);

            TransactionData result = cbtrn03cService.applyRejectionReasonCode(tx);

            assertThat(result.isRejected()).isTrue();
            assertThat(result.getRejectionReasonCode()).isEqualTo(Cbtrn03cService.REJECTION_CODE_INVALID_CARD);
            assertThat(result.getRejectionReasonDescription()).contains("Invalid card number");
        }

        @Test
        @DisplayName("R001: blank card number triggers rejection")
        void blankCardNumberTriggersR001() {
            TransactionData tx = validTransaction("TXN-R001-BLANK");
            tx.setCardNumber("   ");

            TransactionData result = cbtrn03cService.applyRejectionReasonCode(tx);

            assertThat(result.isRejected()).isTrue();
            assertThat(result.getRejectionReasonCode()).isEqualTo(Cbtrn03cService.REJECTION_CODE_INVALID_CARD);
        }

        @Test
        @DisplayName("R002: unknown transaction type code triggers rejection")
        void unknownTypeCodeTriggersR002() {
            TransactionData tx = validTransaction("TXN-R002");
            tx.setTransactionTypeCode("ZZ"); // not in reference data

            TransactionData result = cbtrn03cService.applyRejectionReasonCode(tx);

            assertThat(result.isRejected()).isTrue();
            assertThat(result.getRejectionReasonCode()).isEqualTo(Cbtrn03cService.REJECTION_CODE_INVALID_TYPE);
            assertThat(result.getRejectionReasonDescription()).contains("ZZ");
        }

        @Test
        @DisplayName("R002: null transaction type code triggers rejection")
        void nullTypeCodeTriggersR002() {
            TransactionData tx = validTransaction("TXN-R002-NULL");
            tx.setTransactionTypeCode(null);

            TransactionData result = cbtrn03cService.applyRejectionReasonCode(tx);

            assertThat(result.isRejected()).isTrue();
            assertThat(result.getRejectionReasonCode()).isEqualTo(Cbtrn03cService.REJECTION_CODE_INVALID_TYPE);
        }

        @Test
        @DisplayName("R003: unknown category code triggers rejection")
        void unknownCategoryTriggersR003() {
            TransactionData tx = validTransaction("TXN-R003");
            tx.setTransactionCategoryCode(9999); // not in reference data

            TransactionData result = cbtrn03cService.applyRejectionReasonCode(tx);

            assertThat(result.isRejected()).isTrue();
            assertThat(result.getRejectionReasonCode()).isEqualTo(Cbtrn03cService.REJECTION_CODE_INVALID_CATG);
        }

        @Test
        @DisplayName("R003: null category code triggers rejection")
        void nullCategoryTriggersR003() {
            TransactionData tx = validTransaction("TXN-R003-NULL");
            tx.setTransactionCategoryCode(null);

            TransactionData result = cbtrn03cService.applyRejectionReasonCode(tx);

            assertThat(result.isRejected()).isTrue();
            assertThat(result.getRejectionReasonCode()).isEqualTo(Cbtrn03cService.REJECTION_CODE_INVALID_CATG);
        }

        @Test
        @DisplayName("R004: transaction before start date triggers rejection")
        void transactionBeforeStartDateTriggersR004() {
            TransactionData tx = validTransaction("TXN-R004-BEFORE");
            tx.setTransactionDate(LocalDate.of(2023, 12, 31));

            TransactionData result = cbtrn03cService.applyRejectionReasonCode(
                    tx,
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31));

            assertThat(result.isRejected()).isTrue();
            assertThat(result.getRejectionReasonCode()).isEqualTo(Cbtrn03cService.REJECTION_CODE_DATE_RANGE);
        }

        @Test
        @DisplayName("R004: transaction after end date triggers rejection")
        void transactionAfterEndDateTriggersR004() {
            TransactionData tx = validTransaction("TXN-R004-AFTER");
            tx.setTransactionDate(LocalDate.of(2025, 1, 1));

            TransactionData result = cbtrn03cService.applyRejectionReasonCode(
                    tx,
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31));

            assertThat(result.isRejected()).isTrue();
            assertThat(result.getRejectionReasonCode()).isEqualTo(Cbtrn03cService.REJECTION_CODE_DATE_RANGE);
        }

        @Test
        @DisplayName("R004: transaction on exact start date boundary is accepted")
        void transactionOnStartDateBoundaryIsAccepted() {
            TransactionData tx = validTransaction("TXN-BOUNDARY-START");
            tx.setTransactionDate(LocalDate.of(2024, 1, 1));

            TransactionData result = cbtrn03cService.applyRejectionReasonCode(
                    tx,
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31));

            assertThat(result.isRejected()).isFalse();
        }

        @Test
        @DisplayName("R004: transaction on exact end date boundary is accepted")
        void transactionOnEndDateBoundaryIsAccepted() {
            TransactionData tx = validTransaction("TXN-BOUNDARY-END");
            tx.setTransactionDate(LocalDate.of(2024, 12, 31));

            TransactionData result = cbtrn03cService.applyRejectionReasonCode(
                    tx,
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31));

            assertThat(result.isRejected()).isFalse();
        }

        @Test
        @DisplayName("R005: zero amount triggers rejection")
        void zeroAmountTriggersR005() {
            TransactionData tx = validTransaction("TXN-R005-ZERO");
            tx.setTransactionAmount(BigDecimal.ZERO);

            TransactionData result = cbtrn03cService.applyRejectionReasonCode(tx);

            assertThat(result.isRejected()).isTrue();
            assertThat(result.getRejectionReasonCode()).isEqualTo(Cbtrn03cService.REJECTION_CODE_INVALID_AMOUNT);
        }

        @Test
        @DisplayName("R005: negative amount triggers rejection")
        void negativeAmountTriggersR005() {
            TransactionData tx = validTransaction("TXN-R005-NEG");
            tx.setTransactionAmount(new BigDecimal("-1.00"));

            TransactionData result = cbtrn03cService.applyRejectionReasonCode(tx);

            assertThat(result.isRejected()).isTrue();
            assertThat(result.getRejectionReasonCode()).isEqualTo(Cbtrn03cService.REJECTION_CODE_INVALID_AMOUNT);
        }

        @Test
        @DisplayName("R005: null amount triggers rejection")
        void nullAmountTriggersR005() {
            TransactionData tx = validTransaction("TXN-R005-NULL");
            tx.setTransactionAmount(null);

            TransactionData result = cbtrn03cService.applyRejectionReasonCode(tx);

            assertThat(result.isRejected()).isTrue();
            assertThat(result.getRejectionReasonCode()).isEqualTo(Cbtrn03cService.REJECTION_CODE_INVALID_AMOUNT);
        }

        @Test
        @DisplayName("Rejection precedence: R001 is checked before R002 (card before type)")
        void rejectionPrecedenceCardBeforeType() {
            TransactionData tx = validTransaction("TXN-PRECEDENCE");
            tx.setCardNumber(null);
            tx.setTransactionTypeCode("ZZ");

            TransactionData result = cbtrn03cService.applyRejectionReasonCode(tx);

            // Card check is first, so R001 should win
            assertThat(result.getRejectionReasonCode()).isEqualTo(Cbtrn03cService.REJECTION_CODE_INVALID_CARD);
        }
    }

    // -----------------------------------------------------------------------
    // 4. Service – report line formatting
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("4. Cbtrn03cService – report line formatting")
    class ReportLineFormattingTests {

        @Test
        @DisplayName("formatRejectionReportLine produces non-null output for valid transaction")
        void formatValidTransaction() {
            TransactionData tx = validTransaction("TXN-FMT-001");
            cbtrn03cService.applyRejectionReasonCode(tx);

            String line = cbtrn03cService.formatRejectionReportLine(tx);

            assertThat(line).isNotNull().isNotEmpty();
            assertThat(line).contains("TXN-FMT-001");
        }

        @Test
        @DisplayName("formatRejectionReportLine includes rejection code for rejected transaction")
        void formatRejectedTransactionIncludesCode() {
            TransactionData tx = validTransaction("TXN-FMT-002");
            tx.setCardNumber(null);
            cbtrn03cService.applyRejectionReasonCode(tx);

            String line = cbtrn03cService.formatRejectionReportLine(tx);

            assertThat(line).contains(Cbtrn03cService.REJECTION_CODE_INVALID_CARD);
        }

        @Test
        @DisplayName("formatRejectionReportLine handles null amount gracefully")
        void formatNullAmountGracefully() {
            TransactionData tx = validTransaction("TXN-FMT-003");
            tx.setTransactionAmount(null);
            cbtrn03cService.applyRejectionReasonCode(tx);

            // Should not throw
            String line = cbtrn03cService.formatRejectionReportLine(tx);
            assertThat(line).isNotNull();
        }
    }

    // -----------------------------------------------------------------------
    // 5. Reference-data lookup tests
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("5. Cbtrn03cService – reference data lookups")
    class ReferenceDataTests {

        @ParameterizedTest(name = "Type [{0}] → [{1}]")
        @CsvSource({
            "PR, Purchase",
            "DB, Debit Adjustment",
            "CR, Credit Adjustment",
            "RF, Refund",
            "FE, Fee",
            "IN, Interest"
        })
        @DisplayName("lookupTransactionTypeDescription returns correct descriptions")
        void transactionTypeDescriptionLookup(String code, String expectedDesc) {
            assertThat(cbtrn03cService.lookupTransactionTypeDescription(code))
                    .isEqualTo(expectedDesc);
        }

        @Test
        @DisplayName("lookupTransactionTypeDescription returns null for unknown code")
        void unknownTypeDescriptionIsNull() {
            assertThat(cbtrn03cService.lookupTransactionTypeDescription("XX")).isNull();
        }

        @ParameterizedTest(name = "Category key [{0}] → [{1}]")
        @CsvSource({
            "PR0001, Retail",
            "PR0002, Online Retail",
            "DB0002, Online Retail"
        })
        @DisplayName("lookupCategoryDescription returns correct descriptions")
        void categoryDescriptionLookup(String key, String expectedDesc) {
            assertThat(cbtrn03cService.lookupCategoryDescription(key))
                    .isEqualTo(expectedDesc);
        }

        @Test
        @DisplayName("lookupCategoryDescription returns null for unknown key")
        void unknownCategoryDescriptionIsNull() {
            assertThat(cbtrn03cService.lookupCategoryDescription("ZZ9999")).isNull();
        }    }

    // -----------------------------------------------------------------------
    // 6. Sample data builder tests
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("6. Sample data builder")
    class SampleDataTests {

        @Test
        @DisplayName("buildSampleTransactions returns non-empty list")
        void sampleDataIsNotEmpty() {
            List<TransactionData> data = Cbtrn03cJobConfig.buildSampleTransactions();
            assertThat(data).isNotEmpty();
        }

        @Test
        @DisplayName("Sample data contains at least one valid and one rejected transaction")
        void sampleDataContainsMixedTransactions() {
            List<TransactionData> data = Cbtrn03cJobConfig.buildSampleTransactions();

            // Process through service
            long validCount   = data.stream()
                    .map(cbtrn03cService::applyRejectionReasonCode)
                    .filter(t -> !t.isRejected())
                    .count();
            long rejectedCount = data.stream()
                    .filter(TransactionData::isRejected)
                    .count();

            assertThat(validCount).isGreaterThan(0);
            assertThat(rejectedCount).isGreaterThan(0);
        }

        @Test
        @DisplayName("All sample transactions have non-null IDs")
        void sampleTransactionIdsAreNonNull() {
            List<TransactionData> data = Cbtrn03cJobConfig.buildSampleTransactions();
            assertThat(data).allSatisfy(tx ->
                    assertThat(tx.getTransactionId()).isNotNull().isNotEmpty());
        }
    }

    // -----------------------------------------------------------------------
    // 7. TransactionData model tests
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("7. TransactionData model")
    class TransactionDataModelTests {

        @Test
        @DisplayName("TransactionData can be instantiated and all fields set/get")
        void transactionDataFieldsWork() {
            TransactionData tx = new TransactionData();
            tx.setTransactionId("T001");
            tx.setCardNumber("4111111111111111");
            tx.setAccountId("ACC001");
            tx.setTransactionTypeCode("PR");
            tx.setTransactionCategoryCode(1);
            tx.setTransactionAmount(new BigDecimal("100.00"));
            tx.setTransactionDate(LocalDate.of(2024, 6, 1));
            tx.setRejected(true);
            tx.setRejectionReasonCode("R001");
            tx.setRejectionReasonDescription("Test rejection");

            assertThat(tx.getTransactionId()).isEqualTo("T001");
            assertThat(tx.getCardNumber()).isEqualTo("4111111111111111");
            assertThat(tx.getAccountId()).isEqualTo("ACC001");
            assertThat(tx.getTransactionTypeCode()).isEqualTo("PR");
            assertThat(tx.getTransactionCategoryCode()).isEqualTo(1);
            assertThat(tx.getTransactionAmount()).isEqualByComparingTo("100.00");
            assertThat(tx.getTransactionDate()).isEqualTo(LocalDate.of(2024, 6, 1));
            assertThat(tx.isRejected()).isTrue();
            assertThat(tx.getRejectionReasonCode()).isEqualTo("R001");
            assertThat(tx.getRejectionReasonDescription()).isEqualTo("Test rejection");
        }

        @Test
        @DisplayName("TransactionData toString includes key fields")
        void transactionDataToStringIsInformative() {
            TransactionData tx = validTransaction("TXN-STR-001");
            String str = tx.toString();
            assertThat(str).contains("TXN-STR-001");
            assertThat(str).contains("transactionId");
        }
    }

    // -----------------------------------------------------------------------
    // Helper factory methods
    // -----------------------------------------------------------------------

    /**
     * Builds a fully-valid {@link TransactionData} for use in tests.
     * All reference-data lookups (type=PR, category=1) will succeed.
     */
    private static TransactionData validTransaction(String id) {
        TransactionData tx = new TransactionData();
        tx.setTransactionId(id);
        tx.setCardNumber("4111111111111111");
        tx.setAccountId("ACCT001");
        tx.setTransactionTypeCode("PR");        // "Purchase" – in reference data
        tx.setTransactionCategoryCode(1);       // "PR0001" = "Retail" – in reference data
        tx.setTransactionAmount(new BigDecimal("100.00"));
        tx.setTransactionDate(LocalDate.of(2024, 6, 15));
        tx.setTransactionSource("BATCH");
        return tx;
    }
}
