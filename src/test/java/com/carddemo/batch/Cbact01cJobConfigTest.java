package com.carddemo.batch;

import com.carddemo.CardDemoApplication;
import com.carddemo.model.AccountData;
import com.carddemo.repository.AccountRepository;
import com.carddemo.service.Cbact01cService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cbact01cJobConfigTest – integration test for the CBACT01C Spring Batch job.
 *
 * <p>Uses:
 * <ul>
 *   <li>{@link SpringBatchTest} – auto-configures {@link JobLauncherTestUtils}
 *       and {@link JobRepositoryTestUtils}.</li>
 *   <li>{@link SpringBootTest} with the full application context.</li>
 *   <li>H2 in-memory datasource (configured in test application.properties).</li>
 * </ul>
 *
 * <p>Test scenarios:
 * <ol>
 *   <li>Job completes with {@link BatchStatus#COMPLETED} for empty input.</li>
 *   <li>Job completes with {@link BatchStatus#COMPLETED} for typical account records.</li>
 *   <li>Business rule: zero CYC-DEBIT defaults to 2525.00 (COBOL para 1300).</li>
 *   <li>Business rule: non-zero CYC-DEBIT is preserved as-is.</li>
 *   <li>Business rule: array record slots 1–3 are populated with correct values.</li>
 *   <li>Business rule: VBRC records carry correct acct-id and year extraction.</li>
 *   <li>Date reformatting via COBDATFT equivalent.</li>
 * </ol>
 */
@SpringBatchTest
@SpringBootTest(classes = CardDemoApplication.class)
@ActiveProfiles("test")
class Cbact01cJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private Job cbact01cJob;

    @Autowired
    private Cbact01cService cbact01cService;

    @BeforeEach
    void setUp() {
        // Clean batch metadata between tests
        jobRepositoryTestUtils.removeJobExecutions();
        // Clean account data
        accountRepository.deleteAll();

        // Inject the job under test into the launcher util
        jobLauncherTestUtils.setJob(cbact01cJob);
    }

    // ---------------------------------------------------------------
    // Test 1: Job completes COMPLETED with empty table
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CBACT01C job completes COMPLETED when account table is empty")
    void testJobCompletesWithEmptyInput() throws Exception {
        // Given: no accounts in DB

        // When
        JobExecution execution = jobLauncherTestUtils.launchJob(uniqueParams());

        // Then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    // ---------------------------------------------------------------
    // Test 2: Job completes COMPLETED with typical records
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CBACT01C job completes COMPLETED with multiple account records")
    void testJobCompletesWithAccounts() throws Exception {
        // Given: two accounts in DB
        accountRepository.saveAll(List.of(
                buildAccount(10000000001L, "Y", "1500.00", "5000.00",
                        "2000.00", "2020-01-15", "2025-01-15", "2023-06-01",
                        "100.00", "250.00", "12345", "GROUP001"),
                buildAccount(10000000002L, "N", "-500.50", "3000.00",
                        "1000.00", "2019-03-20", "2024-03-20", "2022-09-15",
                        "0.00", "0.00", "67890", "GROUP002")
        ));

        // When
        JobExecution execution = jobLauncherTestUtils.launchJob(uniqueParams());

        // Then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Verify step-level counts
        execution.getStepExecutions().forEach(step -> {
            assertThat(step.getReadCount()).isEqualTo(2L);
            assertThat(step.getWriteCount()).isEqualTo(2L);
        });
    }

    // ---------------------------------------------------------------
    // Test 3: Business rule – zero CYC-DEBIT defaults to 2525.00
    // COBOL: IF ACCT-CURR-CYC-DEBIT EQUAL TO ZERO MOVE 2525.00 TO OUT-ACCT-CURR-CYC-DEBIT
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Business rule: CYC-DEBIT defaults to 2525.00 when zero (COBOL para 1300)")
    void testZeroCycDebitDefaultsTo2525() {
        // Given: account with zero CYC-DEBIT
        AccountData account = buildAccount(10000000003L, "Y", "1000.00",
                "5000.00", "2000.00", "2020-01-01", "2025-01-01", "2023-01-01",
                "50.00", "0.00", "11111", "GRPTEST");

        // When
        Cbact01cService.AccountOutputRecord out = cbact01cService.populateOutputRecord(account);

        // Then: IF ACCT-CURR-CYC-DEBIT EQUAL TO ZERO → MOVE 2525.00
        assertThat(out.getCurrCycDebit())
                .isEqualByComparingTo(Cbact01cService.DEFAULT_CYC_DEBIT);
    }

    // ---------------------------------------------------------------
    // Test 4: Business rule – non-zero CYC-DEBIT is preserved
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Business rule: non-zero CYC-DEBIT is preserved as-is (COBOL para 1300)")
    void testNonZeroCycDebitPreserved() {
        // Given: account with non-zero CYC-DEBIT
        AccountData account = buildAccount(10000000004L, "Y", "2000.00",
                "8000.00", "3000.00", "2021-05-10", "2026-05-10", "2024-05-01",
                "200.00", "350.75", "22222", "GRPTEST2");

        // When
        Cbact01cService.AccountOutputRecord out = cbact01cService.populateOutputRecord(account);

        // Then: non-zero value preserved (COMP-3 scale applied)
        assertThat(out.getCurrCycDebit())
                .isEqualByComparingTo(new BigDecimal("350.75"));
    }

    // ---------------------------------------------------------------
    // Test 5: Business rule – array record slots
    // COBOL para 1400-POPUL-ARRAY-RECORD
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Business rule: array record slots populated correctly (COBOL para 1400)")
    void testArrayRecordPopulation() {
        // Given
        AccountData account = buildAccount(10000000005L, "Y", "999.99",
                "5000.00", "1500.00", "2020-06-01", "2025-06-01", "2023-06-01",
                "100.00", "200.00", "33333", "GRPTEST3");

        // When
        Cbact01cService.AccountArrayRecord arr = cbact01cService.populateArrayRecord(account);

        // Then – slot 1: MOVE ACCT-CURR-BAL TO ARR-ACCT-CURR-BAL(1)
        //                MOVE 1005.00       TO ARR-ACCT-CURR-CYC-DEBIT(1)
        assertThat(arr.getBals()[0]).isEqualByComparingTo(new BigDecimal("999.99"));
        assertThat(arr.getDebits()[0]).isEqualByComparingTo(Cbact01cService.ARRAY_SLOT1_DEBIT);

        // Slot 2: MOVE ACCT-CURR-BAL TO ARR-ACCT-CURR-BAL(2)
        //         MOVE 1525.00       TO ARR-ACCT-CURR-CYC-DEBIT(2)
        assertThat(arr.getBals()[1]).isEqualByComparingTo(new BigDecimal("999.99"));
        assertThat(arr.getDebits()[1]).isEqualByComparingTo(Cbact01cService.ARRAY_SLOT2_DEBIT);

        // Slot 3: MOVE -1025.00 TO ARR-ACCT-CURR-BAL(3)
        //         MOVE -2500.00 TO ARR-ACCT-CURR-CYC-DEBIT(3)
        assertThat(arr.getBals()[2]).isEqualByComparingTo(Cbact01cService.ARRAY_SLOT3_BAL);
        assertThat(arr.getDebits()[2]).isEqualByComparingTo(Cbact01cService.ARRAY_SLOT3_DEBIT);

        // Slots 4 & 5 remain ZERO (INITIALIZE in COBOL)
        assertThat(arr.getBals()[3]).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(arr.getBals()[4]).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ---------------------------------------------------------------
    // Test 6: Business rule – VBRC record fields
    // COBOL para 1500-POPUL-VBRC-RECORD
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Business rule: VBRC records carry correct account id and year (COBOL para 1500)")
    void testVbrcRecordPopulation() {
        // Given
        AccountData account = buildAccount(10000000006L, "Y", "5000.00",
                "10000.00", "4000.00", "2019-01-01", "2024-01-01", "2025-12-31",
                "500.00", "600.00", "44444", "GRPTEST4");

        String reformattedDate = cbact01cService.reformatDate(account.getReissueDate());

        // When
        Cbact01cService.VbrcRecord vbrc = cbact01cService.populateVbrcRecord(
                account, reformattedDate);

        // Then – VBR-REC1 (MOVE ACCT-ID / MOVE ACCT-ACTIVE-STATUS)
        assertThat(vbrc.getVb1AcctId()).isEqualTo(10000000006L);
        assertThat(vbrc.getVb1ActiveStatus()).isEqualTo("Y");

        // VBR-REC2 – MOVE WS-ACCT-REISSUE-YYYY TO VB2-ACCT-REISSUE-YYYY
        assertThat(vbrc.getVb2AcctId()).isEqualTo(10000000006L);
        assertThat(vbrc.getVb2ReissueYyyy()).isEqualTo("2025");
        assertThat(vbrc.getVb2CurrBal()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(vbrc.getVb2CreditLimit()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    // ---------------------------------------------------------------
    // Test 7: Date reformatting (COBDATFT equivalent)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("COBDATFT equivalent: reformat date to ISO yyyy-MM-dd")
    void testDateReformatting() {
        // ISO input – should pass through unchanged
        assertThat(cbact01cService.reformatDate("2023-06-15"))
                .isEqualTo("2023-06-15");

        // MM/dd/yyyy format
        assertThat(cbact01cService.reformatDate("06/15/2023"))
                .isEqualTo("2023-06-15");

        // yyyyMMdd compact format
        assertThat(cbact01cService.reformatDate("20230615"))
                .isEqualTo("2023-06-15");

        // Null / blank – graceful degradation
        assertThat(cbact01cService.reformatDate(null)).isNull();
        assertThat(cbact01cService.reformatDate("  ")).isBlank();
    }

    // ---------------------------------------------------------------
    // Test 8: COMP-3 BigDecimal scaling
    // ---------------------------------------------------------------

    @Test
    @DisplayName("COMP-3 BigDecimal: scale to 2 decimal places with HALF_UP rounding")
    void testComp3Scaling() {
        // PIC S9(10)V99 USAGE IS COMP-3 → exactly 2 decimal places
        assertThat(cbact01cService.scaleComp3(new BigDecimal("1234.567")))
                .isEqualByComparingTo(new BigDecimal("1234.57")); // HALF_UP

        assertThat(cbact01cService.scaleComp3(new BigDecimal("-999.994")))
                .isEqualByComparingTo(new BigDecimal("-999.99"));

        assertThat(cbact01cService.scaleComp3(null))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ---------------------------------------------------------------
    // Test 9: Job handles large batch of accounts
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CBACT01C job handles 150 accounts across multiple chunks (chunk size=100)")
    void testJobWithLargeBatch() throws Exception {
        // Given: 150 accounts (spans 2 chunks)
        List<AccountData> accounts = new java.util.ArrayList<>();
        for (int i = 1; i <= 150; i++) {
            accounts.add(buildAccount(
                    10000000000L + i, "Y",
                    String.valueOf(100 * i + ".00"),
                    "50000.00", "20000.00",
                    "2020-01-01", "2025-01-01", "2023-01-01",
                    "100.00", i % 5 == 0 ? "0.00" : "250.00",
                    "ZIP" + i, "GROUP" + (i % 10)));
        }
        accountRepository.saveAll(accounts);

        // When
        JobExecution execution = jobLauncherTestUtils.launchJob(uniqueParams());

        // Then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        long totalRead = execution.getStepExecutions().stream()
                .mapToLong(s -> s.getReadCount()).sum();
        assertThat(totalRead).isEqualTo(150L);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private AccountData buildAccount(Long acctId, String activeStatus,
            String currBal, String creditLimit, String cashCreditLimit,
            String openDate, String expirationDate, String reissueDate,
            String currCycCredit, String currCycDebit,
            String addrZip, String groupId) {

        return AccountData.builder()
                .acctId(acctId)
                .activeStatus(activeStatus)
                .currBal(new BigDecimal(currBal))
                .creditLimit(new BigDecimal(creditLimit))
                .cashCreditLimit(new BigDecimal(cashCreditLimit))
                .openDate(openDate)
                .expirationDate(expirationDate)
                .reissueDate(reissueDate)
                .currCycCredit(new BigDecimal(currCycCredit))
                .currCycDebit(new BigDecimal(currCycDebit))
                .addrZip(addrZip)
                .groupId(groupId)
                .build();
    }

    /** Generates unique job parameters to avoid JobInstanceAlreadyCompleteException. */
    private JobParameters uniqueParams() {
        return new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
    }
}
