package com.carddemo.batch;

import com.carddemo.model.TransactionData;
import com.carddemo.service.Cbtrn03cService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Cbtrn03cJobConfig – Spring Batch configuration for the CBTRN03C Transaction
 * Rejection Handler job.
 *
 * <p>Migrated from COBOL batch program {@code CBTRN03C.CBL} (CardDemo).
 *
 * <h2>COBOL-to-Java mapping</h2>
 * <pre>
 * COBOL paragraph / file             → Spring Batch component
 * ─────────────────────────────────────────────────────────────
 * TRANSACT-FILE (sequential read)    → ItemReader  (ListItemReader / DB reader)
 * Date-range filter (main loop)      → ItemProcessor (Cbtrn03cService)
 * XREF / TRANTYPE / TRANCATG lookups → ItemProcessor (Cbtrn03cService)
 * REPORT-FILE (FD-REPTFILE-REC)      → ItemWriter  (FlatFile / logged output)
 * Page / account / grand totals      → Step-level listener (Cbtrn03cStepListener)
 * </pre>
 *
 * <h2>Job parameters</h2>
 * <ul>
 *   <li>{@code startDate} – inclusive start date (yyyy-MM-dd), maps to WS-START-DATE</li>
 *   <li>{@code endDate}   – inclusive end date   (yyyy-MM-dd), maps to WS-END-DATE</li>
 * </ul>
 */
@Configuration
public class Cbtrn03cJobConfig {

    private static final Logger log = LoggerFactory.getLogger(Cbtrn03cJobConfig.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int CHUNK_SIZE = 50; // mirrors COBOL page-size of 20; larger for DB efficiency

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final Cbtrn03cService cbtrn03cService;

    @Autowired
    public Cbtrn03cJobConfig(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              Cbtrn03cService cbtrn03cService) {
        this.jobRepository        = jobRepository;
        this.transactionManager   = transactionManager;
        this.cbtrn03cService      = cbtrn03cService;
    }

    // -----------------------------------------------------------------------
    // Job definition
    // -----------------------------------------------------------------------

    /**
     * The top-level Spring Batch {@link Job} named {@code cbtrn03cJob}.
     *
     * <p>Equivalent to the main PROCEDURE DIVISION of CBTRN03C.CBL which opens
     * files, iterates through transactions, writes the report, then closes files.
     */
    @Bean
    public Job cbtrn03cJob() {
        return new JobBuilder("cbtrn03cJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(cbtrn03cStep())
                .build();
    }

    // -----------------------------------------------------------------------
    // Step definition
    // -----------------------------------------------------------------------

    /**
     * The single processing {@link Step}.
     *
     * <p>Chunk-oriented processing replaces the COBOL PERFORM-UNTIL loop:
     * read → process → write, with automatic transaction management.
     */
    @Bean
    public Step cbtrn03cStep() {
        return new StepBuilder("cbtrn03cStep", jobRepository)
                .<TransactionData, TransactionData>chunk(CHUNK_SIZE, transactionManager)
                .reader(cbtrn03cReader())
                .processor(cbtrn03cProcessor(null, null))
                .writer(cbtrn03cWriter())
                .listener(new Cbtrn03cStepListener())
                .build();
    }

    // -----------------------------------------------------------------------
    // Reader – COBOL: 1000-TRANFILE-GET-NEXT
    // -----------------------------------------------------------------------

    /**
     * Provides rejected (or candidate) {@link TransactionData} records for processing.
     *
     * <p>In production this bean would be replaced with a
     * {@link org.springframework.batch.item.database.JdbcCursorItemReader} or
     * {@link org.springframework.batch.item.file.FlatFileItemReader} reading from
     * the actual TRANSACT-FILE equivalent.  The in-memory list supports unit-test
     * execution and CI pipelines where no database is available.
     */
    @Bean
    public ListItemReader<TransactionData> cbtrn03cReader() {
        log.info("CBTRN03C: Initialising transaction reader (TRANSACT-FILE equivalent)");
        List<TransactionData> transactions = buildSampleTransactions();
        return new ListItemReader<>(transactions);
    }

    // -----------------------------------------------------------------------
    // Processor – COBOL: validation + lookup paragraphs 1500-A/B/C
    // -----------------------------------------------------------------------

    /**
     * Applies rejection reason codes to each transaction.
     *
     * <p>Late-binding job parameters (WS-START-DATE, WS-END-DATE) are injected via
     * {@code @StepScope} and {@code @Value("#{jobParameters[…]}")} so each job
     * execution can specify its own date range, exactly as the COBOL DATEPARM file
     * provided a configurable date window at runtime.
     *
     * @param startDateParam job parameter {@code startDate} (nullable)
     * @param endDateParam   job parameter {@code endDate}   (nullable)
     */
    @Bean
    @StepScope
    public ItemProcessor<TransactionData, TransactionData> cbtrn03cProcessor(
            @Value("#{jobParameters['startDate']}") String startDateParam,
            @Value("#{jobParameters['endDate']}")   String endDateParam) {

        final LocalDate startDate = startDateParam != null
                ? LocalDate.parse(startDateParam, DATE_FMT) : null;
        final LocalDate endDate   = endDateParam   != null
                ? LocalDate.parse(endDateParam,   DATE_FMT) : null;

        if (startDate != null) {
            log.info("CBTRN03C: Reporting from {} to {} (WS-START-DATE / WS-END-DATE)", startDate, endDate);
        }

        return transaction -> {
            TransactionData processed =
                    cbtrn03cService.applyRejectionReasonCode(transaction, startDate, endDate);
            cbtrn03cService.logRejectionDetails(processed);
            return processed;
        };
    }

    // -----------------------------------------------------------------------
    // Writer – COBOL: 1111-WRITE-REPORT-REC / 1120-WRITE-DETAIL
    // -----------------------------------------------------------------------

    /**
     * Writes processed (rejected + valid) transactions to the report output.
     *
     * <p>Mirrors REPORT-FILE (FD-REPTFILE-REC, 133 chars) from CBTRN03C.CBL.
     * Production deployments should replace the logger-based writer with:
     * <ul>
     *   <li>A {@link org.springframework.batch.item.file.FlatFileItemWriter} for
     *       file-based report output, or</li>
     *   <li>A {@link org.springframework.batch.item.database.JdbcBatchItemWriter}
     *       to persist rejections to a database table.</li>
     * </ul>
     */
    @Bean
    public ItemWriter<TransactionData> cbtrn03cWriter() {
        return items -> {
            for (TransactionData tx : items) {
                String reportLine = cbtrn03cService.formatRejectionReportLine(tx);
                if (tx.isRejected()) {
                    log.warn("REJECTED  | {}", reportLine);
                } else {
                    log.info("ACCEPTED  | {}", reportLine);
                }
            }
        };
    }

    // -----------------------------------------------------------------------
    // Sample data builder (replaces TRANSACT-FILE in test/demo mode)
    // -----------------------------------------------------------------------

    /**
     * Builds a representative set of sample transactions for demonstration and
     * test purposes.  These mirror the kinds of records CBTRN03C would read from
     * the VSAM TRANSACT-FILE on the mainframe.
     */
    static List<TransactionData> buildSampleTransactions() {
        List<TransactionData> list = new ArrayList<>();

        // --- Valid transactions ---
        list.add(tx("TXN0000001", "4111111111111001", "ACCT001",
                "PR", 1, new BigDecimal("125.50"), LocalDate.of(2024, 1, 15)));
        list.add(tx("TXN0000002", "4111111111111002", "ACCT002",
                "DB", 1, new BigDecimal("50.00"),  LocalDate.of(2024, 2, 20)));
        list.add(tx("TXN0000003", "4111111111111003", "ACCT003",
                "CR", 2, new BigDecimal("200.00"), LocalDate.of(2024, 3, 10)));

        // --- Rejection scenario: blank card number (R001) ---
        list.add(tx("TXN0000004", "",               "ACCT004",
                "PR", 1, new BigDecimal("75.00"),  LocalDate.of(2024, 1, 5)));

        // --- Rejection scenario: unknown transaction type (R002) ---
        list.add(tx("TXN0000005", "4111111111111005", "ACCT005",
                "ZZ", 1, new BigDecimal("30.00"),  LocalDate.of(2024, 1, 8)));

        // --- Rejection scenario: unknown category (R003) ---
        TransactionData badCatg = tx("TXN0000006", "4111111111111006", "ACCT006",
                "PR", 9999, new BigDecimal("45.00"), LocalDate.of(2024, 1, 12));
        list.add(badCatg);

        // --- Rejection scenario: zero amount (R005) ---
        // Card, type, category all valid – only amount is zero
        list.add(tx("TXN0000007", "4111111111111007", "ACCT007",
                "PR", 1, BigDecimal.ZERO,            LocalDate.of(2024, 1, 20)));

        // --- Additional valid transactions ---
        list.add(tx("TXN0000008", "4111111111111008", "ACCT008",
                "RF", 1, new BigDecimal("99.99"),  LocalDate.of(2024, 4, 1)));
        list.add(tx("TXN0000009", "4111111111111009", "ACCT009",
                "FE", 2, new BigDecimal("10.00"),  LocalDate.of(2024, 4, 15)));
        list.add(tx("TXN0000010", "4111111111111010", "ACCT010",
                "IN", 2, new BigDecimal("5.75"),   LocalDate.of(2024, 5, 1)));

        return list;
    }

    /** Factory helper to reduce boilerplate in {@link #buildSampleTransactions()}. */
    private static TransactionData tx(String id, String card, String acct,
                                       String typeCode, int catCode,
                                       BigDecimal amount, LocalDate date) {
        TransactionData td = new TransactionData();
        td.setTransactionId(id);
        td.setCardNumber(card);
        td.setAccountId(acct);
        td.setTransactionTypeCode(typeCode);
        td.setTransactionCategoryCode(catCode);
        td.setTransactionAmount(amount);
        td.setTransactionDate(date);
        td.setTransactionSource("BATCH");
        return td;
    }

    // -----------------------------------------------------------------------
    // Inner listener class – COBOL: totals paragraphs 1110/1120
    // -----------------------------------------------------------------------

    /**
     * Step-level listener that accumulates and logs totals at the end of the step,
     * mirroring the COBOL paragraphs:
     * <ul>
     *   <li>1110-WRITE-PAGE-TOTALS</li>
     *   <li>1120-WRITE-ACCOUNT-TOTALS</li>
     *   <li>1110-WRITE-GRAND-TOTALS</li>
     * </ul>
     */
    static class Cbtrn03cStepListener
            implements org.springframework.batch.core.StepExecutionListener {

        private static final Logger log = LoggerFactory.getLogger(Cbtrn03cStepListener.class);

        @Override
        public void beforeStep(org.springframework.batch.core.StepExecution stepExecution) {
            log.info("CBTRN03C: START OF EXECUTION – step {}", stepExecution.getStepName());
        }

        @Override
        public org.springframework.batch.core.ExitStatus afterStep(
                org.springframework.batch.core.StepExecution stepExecution) {
            long readCount    = stepExecution.getReadCount();
            long writeCount   = stepExecution.getWriteCount();
            long filterCount  = stepExecution.getFilterCount();
            long skipCount    = stepExecution.getSkipCount();

            log.info("CBTRN03C: END OF EXECUTION – step={} read={} write={} filter={} skip={}",
                    stepExecution.getStepName(), readCount, writeCount, filterCount, skipCount);
            // Mirrors: DISPLAY 'END OF EXECUTION OF PROGRAM CBTRN03C'
            log.info("CBTRN03C: GRAND TOTAL TRANSACTIONS PROCESSED = {}", readCount);
            return stepExecution.getExitStatus();
        }
    }
}
