package com.carddemo.batch;

import com.carddemo.model.CardData;
import com.carddemo.model.CardUpdateRequest;
import com.carddemo.repository.CardDataRepository;
import com.carddemo.service.Cbact02cService;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the CBACT02C Spring Batch job.
 *
 * <ul>
 *   <li>Uses H2 in-memory database (configured in {@code application-test.yml}).</li>
 *   <li>Verifies that the job completes with {@link BatchStatus#COMPLETED}.</li>
 *   <li>Verifies that records are persisted/updated correctly in {@code card_data}.</li>
 *   <li>Tests the service-level business logic in isolation.</li>
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
    private CardDataRepository cardDataRepository;

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
        cardDataRepository.deleteAll();
        jobLauncherTestUtils.setJob(cbact02cJob);
    }

    // -------------------------------------------------------------------------
    // Job-level tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("cbact02cJob – completes with COMPLETED status for valid input file")
    void givenValidInputFile_whenJobRuns_thenStatusIsCompleted() throws Exception {
        // Arrange: point the job at the test flat file
        JobParameters params = new JobParametersBuilder()
                .addString("inputFile", "classpath:test-cardfile-input.txt")
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        // Act
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // Assert – mirrors COBOL END-OF-FILE = 'Y' path
        assertThat(execution.getStatus())
                .as("Job should complete successfully (COBOL: GOBACK with normal exit)")
                .isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    @DisplayName("cbact02cJob – step writes card records to the database")
    void givenValidInputFile_whenJobRuns_thenCardRecordsArePersisted() throws Exception {
        // Arrange
        JobParameters params = new JobParametersBuilder()
                .addString("inputFile", "classpath:test-cardfile-input.txt")
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        // Act
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // Assert – 3 records in the test file → 3 rows in card_data
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<CardData> all = cardDataRepository.findAll();
        assertThat(all).hasSize(3);

        // Verify first record fields
        CardData first = cardDataRepository.findById("4000002000000001").orElseThrow();
        assertThat(first.getCardAcctId()).isEqualTo(50L);
        assertThat(first.getCardCvvCd()).isEqualTo(10);
        assertThat(first.getCardEmbossedName()).isEqualToIgnoringWhitespace("JOHN DOE");
        assertThat(first.getCardExpirationDate()).isEqualTo("2025-12-31");
        assertThat(first.getCardActiveStatus()).isEqualTo("Y");
    }

    @Test
    @DisplayName("cbact02cJob – step counters reflect records read and written")
    void givenValidInputFile_whenJobRuns_thenStepCountersMatch() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("inputFile", "classpath:test-cardfile-input.txt")
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        StepExecution stepExecution = execution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount())
                .as("Should have read all 3 input records")
                .isEqualTo(3);
        assertThat(stepExecution.getWriteCount())
                .as("Should have written all 3 processed records")
                .isEqualTo(3);
        assertThat(stepExecution.getSkipCount())
                .as("No skips expected for clean input")
                .isEqualTo(0);
    }

    @Test
    @DisplayName("cbact02cJob – idempotent: re-running updates existing records")
    void givenExistingRecord_whenJobRunsTwice_thenRecordIsUpdated() throws Exception {
        // Pre-seed an existing card record (simulates VSAM record already on file)
        cardDataRepository.save(CardData.builder()
                .cardNum("4000002000000001")
                .cardAcctId(1L)
                .cardCvvCd(999)
                .cardEmbossedName("OLD NAME")
                .cardExpirationDate("2020-01-01")
                .cardActiveStatus("N")
                .build());

        JobParameters params = new JobParametersBuilder()
                .addString("inputFile", "classpath:test-cardfile-input.txt")
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncherTestUtils.launchJob(params);
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Verify the record was updated (COBOL REWRITE behaviour)
        CardData updated = cardDataRepository.findById("4000002000000001").orElseThrow();
        assertThat(updated.getCardEmbossedName())
                .as("Embossed name should be updated from input")
                .isEqualToIgnoringWhitespace("JOHN DOE");
        assertThat(updated.getCardActiveStatus())
                .as("Active status should be updated to Y")
                .isEqualTo("Y");
    }

    // -------------------------------------------------------------------------
    // Service-level unit tests  (business logic / COBOL IF-EVALUATE parity)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Cbact02cService – processCardUpdate inserts new card record")
    void givenNewCard_whenProcessCardUpdate_thenRecordIsInserted() {
        CardUpdateRequest request = CardUpdateRequest.builder()
                .cardNum("1234567890123456")
                .cardAcctId(42L)
                .cardCvvCd(123)
                .cardEmbossedName("TEST HOLDER")
                .cardExpirationDate("2027-09-30")
                .cardActiveStatus("Y")
                .build();

        CardData result = cbact02cService.processCardUpdate(request);

        assertThat(result.getCardNum()).isEqualTo("1234567890123456");
        assertThat(result.getCardAcctId()).isEqualTo(42L);
        assertThat(result.getCardActiveStatus()).isEqualTo("Y");

        assertThat(cardDataRepository.findById("1234567890123456")).isPresent();
    }

    @Test
    @DisplayName("Cbact02cService – EVALUATE CARD-ACTIVE-STATUS WHEN OTHER → 'N'")
    void givenInvalidActiveStatus_whenProcessCardUpdate_thenStatusDefaultsToN() {
        CardUpdateRequest request = CardUpdateRequest.builder()
                .cardNum("9999888877776666")
                .cardAcctId(99L)
                .cardCvvCd(321)
                .cardEmbossedName("EVALUATE TEST")
                .cardExpirationDate("2026-03-15")
                .cardActiveStatus("X")   // WHEN OTHER in COBOL EVALUATE
                .build();

        CardData result = cbact02cService.processCardUpdate(request);

        assertThat(result.getCardActiveStatus())
                .as("EVALUATE WHEN OTHER should map to 'N'")
                .isEqualTo("N");
    }

    @Test
    @DisplayName("Cbact02cService – ABEND path: blank CARD-NUM throws CardFileProcessingException")
    void givenBlankCardNum_whenProcessCardUpdate_thenThrowsAbendException() {
        CardUpdateRequest badRequest = CardUpdateRequest.builder()
                .cardNum("   ")           // blank CARD-NUM → ABEND path
                .cardAcctId(1L)
                .build();

        assertThatThrownBy(() -> cbact02cService.processCardUpdate(badRequest))
                .isInstanceOf(Cbact02cService.CardFileProcessingException.class)
                .hasMessageContaining("CARD-NUM is required")
                .extracting(ex -> ((Cbact02cService.CardFileProcessingException) ex).getApplResult())
                .isEqualTo(12);  // APPL-RESULT = 12 (error / ABEND)
    }

    @Test
    @DisplayName("Cbact02cService – null request throws CardFileProcessingException")
    void givenNullRequest_whenProcessCardUpdate_thenThrowsAbendException() {
        assertThatThrownBy(() -> cbact02cService.processCardUpdate(null))
                .isInstanceOf(Cbact02cService.CardFileProcessingException.class)
                .extracting(ex -> ((Cbact02cService.CardFileProcessingException) ex).getApplResult())
                .isEqualTo(12);
    }

    @Test
    @DisplayName("Cbact02cService – partial update preserves unmodified fields")
    void givenPartialUpdate_whenProcessCardUpdate_thenUnmodifiedFieldsUnchanged() {
        // Pre-seed
        cardDataRepository.save(CardData.builder()
                .cardNum("1111222233334444")
                .cardAcctId(77L)
                .cardCvvCd(777)
                .cardEmbossedName("KEEP THIS NAME")
                .cardExpirationDate("2030-01-01")
                .cardActiveStatus("Y")
                .build());

        // Partial update: only change activeStatus
        CardUpdateRequest partial = CardUpdateRequest.builder()
                .cardNum("1111222233334444")
                .cardActiveStatus("N")
                // no embossedName, no cvv, no expiryDate
                .build();

        CardData result = cbact02cService.processCardUpdate(partial);

        assertThat(result.getCardActiveStatus()).isEqualTo("N");
        assertThat(result.getCardEmbossedName())
                .as("Unmodified field should be preserved")
                .isEqualTo("KEEP THIS NAME");
        assertThat(result.getCardCvvCd())
                .as("CVV should remain unchanged")
                .isEqualTo(777);
    }

    @Test
    @DisplayName("Cbact02cService – embossed name is truncated to 50 chars (PIC X(50))")
    void givenLongEmbossedName_whenProcessCardUpdate_thenNameTruncatedTo50Chars() {
        String longName = "A".repeat(60);  // exceeds PIC X(50)
        CardUpdateRequest request = CardUpdateRequest.builder()
                .cardNum("5555666677778888")
                .cardAcctId(55L)
                .cardEmbossedName(longName)
                .cardActiveStatus("Y")
                .build();

        CardData result = cbact02cService.processCardUpdate(request);

        assertThat(result.getCardEmbossedName())
                .as("Name must be truncated to 50 chars like COBOL PIC X(50)")
                .hasSize(50);
    }

    @Test
    @DisplayName("Cbact02cService – getCard throws when card not found (APPL-RESULT=12)")
    void givenNonExistentCard_whenGetCard_thenThrowsException() {
        assertThatThrownBy(() -> cbact02cService.getCard("9999999999999999"))
                .isInstanceOf(Cbact02cService.CardFileProcessingException.class)
                .hasMessageContaining("card not found")
                .extracting(ex -> ((Cbact02cService.CardFileProcessingException) ex).getApplResult())
                .isEqualTo(12);
    }
}
