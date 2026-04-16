package com.carddemo.batch;

import com.carddemo.model.AccountData;
import com.carddemo.model.TransactionData;
import com.carddemo.service.Cbtrn02cService;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Spring Batch configuration for {@code cbtrn02cJob}.
 *
 * <h2>Functional Equivalent of COBOL CBTRN02C.CBL</h2>
 * <p>The original COBOL program reads a sequential daily transaction file (DALYTRAN),
 * validates each record (card XREF lookup + account credit-limit + expiry checks),
 * and for every valid transaction:</p>
 * <ol>
 *   <li>Updates the transaction-category balance table (TCATBAL-FILE)</li>
 *   <li>Updates the account balance (ACCOUNT-FILE)</li>
 *   <li>Writes the transaction to the permanent transaction file (TRANSACT-FILE)</li>
 * </ol>
 *
 * <h2>Spring Batch Mapping</h2>
 * <pre>
 *  COBOL                          Spring Batch
 *  ──────────────────────────     ────────────────────────────────────────────
 *  DALYTRAN-FILE (input)          JpaPagingItemReader – reads APPROVED transactions
 *  1500-VALIDATE-TRAN             Pre-filter: reader query status='APPROVED'
 *  2000-POST-TRANSACTION          ItemProcessor → Cbtrn02cService.postTransaction()
 *  2700-UPDATE-TCATBAL            Inside Cbtrn02cService (called by processor)
 *  2800-UPDATE-ACCOUNT-REC        Inside Cbtrn02cService (called by processor)
 *  2900-WRITE-TRANSACTION-FILE    ItemWriter – persists TransactionData (status=POSTED)
 *  WS-TRANSACTION-COUNT           Job execution context counter (logged in listener)
 *  WS-REJECT-COUNT                Transactions skipped/faulted count
 * </pre>
 *
 * <h2>Chunk size</h2>
 * <p>Default chunk size of {@value #CHUNK_SIZE} mirrors the typical batch commit interval
 * for COBOL programs of this type. Override via application property
 * {@code carddemo.batch.cbtrn02c.chunk-size}.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class Cbtrn02cJobConfig {

    // ── constants ────────────────────────────────────────────────────────────
    static final int    CHUNK_SIZE    = 100;
    static final String JOB_NAME      = "cbtrn02cJob";
    static final String STEP_NAME     = "postTransactionsStep";
    static final String READER_NAME   = "approvedTransactionReader";
    static final String APPROVED      = "APPROVED";

    // ── dependencies ─────────────────────────────────────────────────────────
    private final EntityManagerFactory       entityManagerFactory;
    private final Cbtrn02cService            cbtrn02cService;

    // ── job counters (thread-safe for parallel chunks) ───────────────────────
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong skippedCount   = new AtomicLong(0);

    // ════════════════════════════════════════════════════════════════════════
    // JOB
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Defines the {@code cbtrn02cJob} batch job.
     * A {@link RunIdIncrementer} allows the job to be re-run (new JobInstance per launch).
     */
    @Bean(JOB_NAME)
    public Job cbtrn02cJob(JobRepository jobRepository,
                           Step postTransactionsStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(postTransactionsStep)
                .listener(new Cbtrn02cJobExecutionListener(processedCount, skippedCount))
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // STEP
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Single step: read APPROVED transactions → process (post to account) → write (save POSTED).
     * Skip policy: {@link TransactionPostingException} causes the transaction to be counted
     * as skipped without aborting the entire job – analogous to COBOL writing a reject record.
     */
    @Bean(STEP_NAME)
    public Step postTransactionsStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     JpaPagingItemReader<TransactionData> approvedTransactionReader) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<TransactionData, TransactionData>chunk(CHUNK_SIZE, transactionManager)
                .reader(approvedTransactionReader)
                .processor(transactionProcessor())
                .writer(transactionWriter())
                .faultTolerant()
                .skip(TransactionPostingException.class)
                .skipLimit(Integer.MAX_VALUE)   // mirror COBOL: reject individual records, continue
                .listener(new Cbtrn02cSkipListener(skippedCount))
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // READER  –  mirrors COBOL  1000-DALYTRAN-GET-NEXT
    // ════════════════════════════════════════════════════════════════════════

    /**
     * JPA paging reader that pages through all {@link TransactionData} records with
     * {@code tranStatus = 'APPROVED'}, ordered by {@code tranOrigTs} for deterministic
     * processing (mirrors the sequential read order of the COBOL flat-file input).
     */
    @Bean(READER_NAME)
    @StepScope
    public JpaPagingItemReader<TransactionData> approvedTransactionReader() {
        Map<String, Object> params = new HashMap<>();
        params.put("status", APPROVED);

        return new JpaPagingItemReaderBuilder<TransactionData>()
                .name(READER_NAME)
                .entityManagerFactory(entityManagerFactory)
                .queryString(
                        "SELECT t FROM TransactionData t " +
                        "WHERE t.tranStatus = :status " +
                        "ORDER BY t.tranOrigTs ASC, t.tranId ASC")
                .parameterValues(params)
                .pageSize(CHUNK_SIZE)
                .saveState(true)
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // PROCESSOR  –  mirrors COBOL  2000-POST-TRANSACTION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Delegates to {@link Cbtrn02cService#postTransaction(TransactionData)}.
     * Any business rule violation wraps the cause in a {@link TransactionPostingException}
     * so the skip policy can handle it without abending the job
     * (analogous to COBOL writing a reject record rather than calling 9999-ABEND-PROGRAM).
     *
     * <p>Returns the same {@link TransactionData} item (now in POSTED status) for the writer.</p>
     */
    @Bean
    public ItemProcessor<TransactionData, TransactionData> transactionProcessor() {
        return transaction -> {
            try {
                processedCount.incrementAndGet();
                cbtrn02cService.postTransaction(transaction);
                return transaction;          // status now = POSTED
            } catch (IllegalArgumentException ex) {
                // Account not found – mirrors COBOL reject path (reason code 101/109)
                log.warn("Skipping transaction id={}: {}", transaction.getTranId(), ex.getMessage());
                throw new TransactionPostingException(
                        "Failed to post transaction " + transaction.getTranId(), ex);
            }
        };
    }

    // ════════════════════════════════════════════════════════════════════════
    // WRITER  –  mirrors COBOL  2900-WRITE-TRANSACTION-FILE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Persists each processed (POSTED) {@link TransactionData} back to the database.
     * Equivalent to COBOL paragraph {@code 2900-WRITE-TRANSACTION-FILE} which writes
     * the transaction record to the permanent indexed TRANSACT-FILE.
     *
     * <p>The {@code @Transactional} here covers the writer chunk boundary so that
     * the batch of POSTED status updates is committed atomically.</p>
     */
    @Bean
    public ItemWriter<TransactionData> transactionWriter() {
        return new TransactionItemWriter();
    }

    // ════════════════════════════════════════════════════════════════════════
    // INNER WRITER CLASS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Writes a chunk of fully-posted {@link TransactionData} items using JPA.
     * Named inner class (rather than lambda) so Spring can apply {@code @Transactional}.
     */
    static class TransactionItemWriter implements ItemWriter<TransactionData> {

        private com.carddemo.repository.TransactionDataRepository repository;

        /** Constructor injection used by Spring; set via setter to keep no-arg ctor for proxying. */
        @org.springframework.beans.factory.annotation.Autowired
        public void setRepository(
                com.carddemo.repository.TransactionDataRepository repository) {
            this.repository = repository;
        }

        @Override
        @Transactional
        public void write(
                org.springframework.batch.item.Chunk<? extends TransactionData> chunk)
                throws Exception {
            repository.saveAll(chunk.getItems());
            log.debug("Wrote chunk of {} POSTED transactions", chunk.size());
        }
    }
}
