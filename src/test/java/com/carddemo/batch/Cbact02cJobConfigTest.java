package com.carddemo.batch;

import com.carddemo.model.AccountData;
import com.carddemo.model.AccountUpdateRequest;
import com.carddemo.repository.AccountDataRepository;
import com.carddemo.service.Cbact02cService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the CBACT02C Spring Batch job migrated from COBOL.
 *
 * <h2>Test Strategy</h2>
 * <ul>
 *   <li>Uses H2 in-memory database (configured in {@code application-test.yml});
 *       no external PostgreSQL required.</li>
 *   <li>Verifies that the job completes with {@link BatchStatus#COMPLETED}
 *       (mirrors COBOL normal GOBACK exit).</li>
 *   <li>Verifies that {@link AccountData} records are persisted/updated
 *       in the {@code account_data} table with correct values.</li>
 *   <li>Tests {@link Cbact02cService} business logic in isolation, covering
 *       all COBOL IF/EVALUATE branches and COMP-3 arithmetic.</li>
 * </ul>
 */
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class Cbact02cJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private AccountDataRepository accountDataRepository;

    @Autowired
    private Cbact02cService cbact02cService;

    @Autowired
    private Job cbact02cJob;

    // -------------------------------------------------------------------------
    // Setup / Teardown
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        accountDataRepository.deleteAll();
        jobLauncherTestUtils.setJob(cbact02cJob);
    }

    // =========================================================================
    // Job-level integration tests
    // =========================================================================

    @Test
    @DisplayName("cbact02cJob – completes with COMPLETED status for valid account input file")
    void givenValidInputFile_whenJobRuns_thenStatusIsCompleted() throws Exception {
        // Arrange: point job at the test flat file for account records
        JobParameters params = new JobParametersBuilder()
                .addString("inputFile", "classpath:test-account-input.txt")
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        // Act
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // Assert – mirrors COBOL END-OF-FILE = 'Y' normal exit path
        assertThat(execution.getStatus())
                .as("Job should complete successfully (COBOL: GOBACK with normal exit)")
                .isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    @DisplayName("cbact02cJob – persists AccountData records to account_data table")
    void givenValidInputFile_whenJobRuns_thenAccountDataRecordsArePersisted() throws Exception {
        // Arrange
        JobParameters params = new JobParametersBuilder()
                .addString("inputFile", "classpath:test-account-input.txt")
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        // Act
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // Assert – 3 account records in the test file → 3 rows in account_data
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<AccountData> all = accountDataRepository.findAll();
        assertThat(all)
                .as("Expected 3 AccountData rows written from input file")
                .hasSize(3);

        // Verify first record: acctId=50, status=Y
        AccountData first = accountDataRepository.findById(50L).orElseThrow();
        assertThat(first.getAcctActiveStatus()).isEqualTo("Y");
        assertThat(first.getAcctCurrBal())
                .isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(first.getAcctCreditLimit())
                .isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(first.getAcctOpenDate()).isEqualTo("2020-03-15");
        assertThat(first.getAcctGroupId()).isEqualToIgnoringWhitespace("GROUP001");
    }

    @Test
    @DisplayName("cbact02cJob – step counters reflect records read and written")
    void givenValidInputFile_whenJobRuns_thenStepCountersMatch() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("inputFile", "classpath:test-account-input.txt")
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        StepExecution stepExecution = execution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount())
                .as("Should have read all 3 input account records")
                .isEqualTo(3);
        assertThat(stepExecution.getWriteCount())
                .as("Should have written all 3 processed account records")
                .isEqualTo(3);
        assertThat(stepExecution.getSkipCount())
                .as("No skips expected for clean input")
                .isEqualTo(0);
    }

    @Test
    @DisplayName("cbact02cJob – idempotent: re-running job updates existing AccountData records")
    void givenExistingRecord_whenJobRunsTwice_thenAccountDataIsUpdated() throws Exception {
        // Pre-seed an existing account record (simulates VSAM record already on file)
        accountDataRepository.save(AccountData.builder()
                .acctId(50L)
                .acctActiveStatus("N")
                .acctCurrBal(new BigDecimal("100.00"))
                .acctCreditLimit(new BigDecimal("100.00"))
                .acctGroupId("OLDGROUP")
                .build());

        JobParameters params = new JobParametersBuilder()
                .addString("inputFile", "classpath:test-account-input.txt")
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncherTestUtils.launchJob(params);
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Verify the record was updated (COBOL REWRITE behaviour)
        AccountData updated = accountDataRepository.findById(50L).orElseThrow();
        assertThat(updated.getAcctActiveStatus())
                .as("Active status should be updated to Y from input file")
                .isEqualTo("Y");
        assertThat(updated.getAcctCurrBal())
                .as("Current balance should be updated from input file (COMP-3 REWRITE)")
                .isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(updated.getAcctGroupId())
                .as("Group ID should be updated from input file")
                .isEqualToIgnoringWhitespace("GROUP001");
    }

    // =========================================================================
    // Service-level unit tests (business logic / COBOL IF-EVALUATE parity)
    // =========================================================================

    @Test
    @DisplayName("Cbact02cService – processAccountUpdate inserts new AccountData record")
    void givenNewAccount_whenProcessAccountUpdate_thenRecordIsInserted() {
        AccountUpdateRequest request = AccountUpdateRequest.builder()
                .acctId(42L)
                .acctActiveStatus("Y")
                .acctCurrBal("2500.00")
                .acctCreditLimit("10000.00")
                .acctCashCreditLimit("2000.00")
                .acctOpenDate("2023-01-15")
                .acctExpirationDate("2028-01-15")
                .acctReissueDate("2025-01-15")
                .acctAddrZip("12345")
                .acctGroupId("GROUP001")
                .build();

        AccountData result = cbact02cService.processAccountUpdate(request);

        assertThat(result.getAcctId()).isEqualTo(42L);
        assertThat(result.getAcctActiveStatus()).isEqualTo("Y");
        assertThat(result.getAcctCurrBal())
                .isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(accountDataRepository.findById(42L)).isPresent();
    }

    @Test
    @DisplayName("Cbact02cService – COMP-3 monetary fields use BigDecimal HALF_UP rounding")
    void givenMonetaryValueWithManyDecimals_whenProcessed_thenRoundedHalfUp() {
        // COBOL COMPUTE ROUNDED = HALF_UP: 1234.565 → 1234.57
        AccountUpdateRequest request = AccountUpdateRequest.builder()
                .acctId(99L)
                .acctActiveStatus("Y")
                .acctCurrBal("1234.565")   // third decimal triggers HALF_UP round
                .acctCreditLimit("5000.00")
                .build();

        AccountData result = cbact02cService.processAccountUpdate(request);

        // HALF_UP: 1234.565 rounds to 1234.57 (not 1234.56 as HALF_EVEN would give)
        assertThat(result.getAcctCurrBal())
                .as("COMP-3 HALF_UP rounding: 1234.565 → 1234.57")
                .isEqualByComparingTo(new BigDecimal("1234.57"));
    }

    @Test
    @DisplayName("Cbact02cService – EVALUATE ACCT-ACTIVE-STATUS WHEN OTHER → 'N'")
    void givenInvalidActiveStatus_whenProcessAccountUpdate_thenStatusDefaultsToN() {
        AccountUpdateRequest request = AccountUpdateRequest.builder()
                .acctId(88L)
                .acctActiveStatus("X")   // WHEN OTHER in COBOL EVALUATE
                .acctCurrBal("0.00")
                .build();

        AccountData result = cbact02cService.processAccountUpdate(request);

        assertThat(result.getAcctActiveStatus())
                .as("COBOL EVALUATE WHEN OTHER should map to 'N'")
                .isEqualTo("N");
    }

    @Test
    @DisplayName("Cbact02cService – ABEND path: null ACCT-ID throws AccountFileProcessingException")
    void givenNullAcctId_whenProcessAccountUpdate_thenThrowsAbendException() {
        AccountUpdateRequest badRequest = AccountUpdateRequest.builder()
                .acctId(null)           // missing ACCT-ID → ABEND path
                .acctActiveStatus("Y")
                .build();

        assertThatThrownBy(() -> cbact02cService.processAccountUpdate(badRequest))
                .isInstanceOf(Cbact02cService.AccountFileProcessingException.class)
                .hasMessageContaining("ACCT-ID is required")
                .extracting(ex ->
                    ((Cbact02cService.AccountFileProcessingException) ex).getApplResult())
                .isEqualTo(12);  // APPL-RESULT = 12 (error / ABEND)
    }

    @Test
    @DisplayName("Cbact02cService – ABEND path: zero ACCT-ID throws AccountFileProcessingException")
    void givenZeroAcctId_whenProcessAccountUpdate_thenThrowsAbendException() {
        AccountUpdateRequest badRequest = AccountUpdateRequest.builder()
                .acctId(0L)             // zero ACCT-ID → ABEND path
                .acctActiveStatus("Y")
                .build();

        assertThatThrownBy(() -> cbact02cService.processAccountUpdate(badRequest))
                .isInstanceOf(Cbact02cService.AccountFileProcessingException.class)
                .extracting(ex ->
                    ((Cbact02cService.AccountFileProcessingException) ex).getApplResult())
                .isEqualTo(12);
    }

    @Test
    @DisplayName("Cbact02cService – null request throws AccountFileProcessingException")
    void givenNullRequest_whenProcessAccountUpdate_thenThrowsAbendException() {
        assertThatThrownBy(() -> cbact02cService.processAccountUpdate(null))
                .isInstanceOf(Cbact02cService.AccountFileProcessingException.class)
                .extracting(ex ->
                    ((Cbact02cService.AccountFileProcessingException) ex).getApplResult())
                .isEqualTo(12);
    }

    @Test
    @DisplayName("Cbact02cService – partial update preserves unmodified AccountData fields")
    void givenPartialUpdate_whenProcessAccountUpdate_thenUnmodifiedFieldsPreserved() {
        // Pre-seed existing account
        accountDataRepository.save(AccountData.builder()
                .acctId(77L)
                .acctActiveStatus("Y")
                .acctCurrBal(new BigDecimal("5000.00"))
                .acctCreditLimit(new BigDecimal("15000.00"))
                .acctGroupId("GROUP_ORIG")
                .build());

        // Partial update: only change active status
        AccountUpdateRequest partial = AccountUpdateRequest.builder()
                .acctId(77L)
                .acctActiveStatus("N")
                // no currBal, no creditLimit, no groupId supplied
                .build();

        AccountData result = cbact02cService.processAccountUpdate(partial);

        assertThat(result.getAcctActiveStatus())
                .as("Active status should be updated to N")
                .isEqualTo("N");
        assertThat(result.getAcctCurrBal())
                .as("Unmodified curr balance should remain 5000.00")
                .isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.getAcctGroupId())
                .as("Unmodified group ID should be preserved")
                .isEqualTo("GROUP_ORIG");
    }

    @Test
    @DisplayName("Cbact02cService – group ID is truncated to 10 chars (PIC X(10))")
    void givenLongGroupId_whenProcessAccountUpdate_thenGroupIdTruncatedTo10Chars() {
        String longGroupId = "ABCDEFGHIJKLMNOP";   // exceeds PIC X(10)
        AccountUpdateRequest request = AccountUpdateRequest.builder()
                .acctId(55L)
                .acctActiveStatus("Y")
                .acctGroupId(longGroupId)
                .build();

        AccountData result = cbact02cService.processAccountUpdate(request);

        assertThat(result.getAcctGroupId())
                .as("Group ID must be truncated to 10 chars like COBOL PIC X(10)")
                .hasSize(10)
                .isEqualTo("ABCDEFGHIJ");
    }

    @Test
    @DisplayName("Cbact02cService – getAccount throws when account not found (APPL-RESULT=12)")
    void givenNonExistentAccount_whenGetAccount_thenThrowsException() {
        assertThatThrownBy(() -> cbact02cService.getAccount(99999L))
                .isInstanceOf(Cbact02cService.AccountFileProcessingException.class)
                .hasMessageContaining("account not found")
                .extracting(ex ->
                    ((Cbact02cService.AccountFileProcessingException) ex).getApplResult())
                .isEqualTo(12);
    }

    @Test
    @DisplayName("Cbact02cService – invalid monetary value throws AccountFileProcessingException")
    void givenInvalidMonetaryValue_whenParseMonetary_thenThrowsAbendException() {
        AccountUpdateRequest request = AccountUpdateRequest.builder()
                .acctId(66L)
                .acctActiveStatus("Y")
                .acctCurrBal("NOT_A_NUMBER")  // invalid COMP-3 value → ABEND
                .build();

        assertThatThrownBy(() -> cbact02cService.processAccountUpdate(request))
                .isInstanceOf(Cbact02cService.AccountFileProcessingException.class)
                .hasMessageContaining("ACCT-CURR-BAL")
                .extracting(ex ->
                    ((Cbact02cService.AccountFileProcessingException) ex).getApplResult())
                .isEqualTo(12);
    }
}
