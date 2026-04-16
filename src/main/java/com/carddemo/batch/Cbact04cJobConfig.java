package com.carddemo.batch;

import com.carddemo.model.Transaction;
import com.carddemo.model.TransactionCategoryBalance;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.service.Cbact04cService;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * Cbact04cJobConfig – Spring Batch configuration for the CBACT04C migration.
 *
 * <h2>COBOL → Spring Batch mapping</h2>
 * <pre>
 * COBOL construct                        Spring Batch equivalent
 * ─────────────────────────────────────────────────────────────────────────────
 * Sequential READ TCATBAL-FILE           JpaCursorItemReader&lt;TransactionCategoryBalance&gt;
 * Main loop / processing paragraphs      Cbact04cItemProcessor (delegates to Cbact04cService)
 * WRITE TRANSACT-FILE (interest txns)    Cbact04cItemWriter  → TransactionRepository.saveAll()
 * End-of-file PERFORM 1050-UPDATE-ACCT  StepExecutionListener#afterStep → finalizeLastAccount()
 * PARM-DATE from LINKAGE SECTION         Spring Batch JobParameter  "runDate"
 * </pre>
 *
 * <h2>Entity model</h2>
 * <ul>
 *   <li>{@link com.carddemo.model.CardData} (CVACT02Y) – full card master record
 *       (card number, account ID, CVV, embossed name, expiry, active status).</li>
 *   <li>{@link com.carddemo.model.CardXref} (CVACT03Y) – card/account cross-reference;
 *       the file directly read by COBOL paragraph 1110-GET-XREF-DATA using the
 *       alternate key FD-XREF-ACCT-ID.</li>
 *   <li>{@link com.carddemo.model.AccountData} (CVACT01Y) – account master record.</li>
 * </ul>
 *
 * <h2>Job flow</h2>
 * <ol>
 *   <li>beforeStep  – resets stateful service with the run date.</li>
 *   <li>chunk loop  – reads TCATBAL records, processes each, collects interest transactions.</li>
 *   <li>afterStep   – finalises the last account (mirrors end-of-file PERFORM).</li>
 * </ol>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class Cbact04cJobConfig {

    private final EntityManagerFactory   entityManagerFactory;
    private final Cbact04cService        cbact04cService;
    private final TransactionRepository  transactionRepository;

    // ---------------------------------------------------------------
    // Reader — sequential scan of TCATBAL-FILE (CVTRA01Y)
    // Mirrors: READ TCATBAL-FILE INTO TRAN-CAT-BAL-RECORD (paragraph 1000)
    // ---------------------------------------------------------------

    /**
     * JpaCursorItemReader reads TransactionCategoryBalance rows ordered by
     * account ID then type then category — matching the COBOL VSAM indexed
     * sequential access pattern (TRANCAT-ACCT-ID, TRANCAT-TYPE-CD, TRANCAT-CD).
     */
    @Bean
    @StepScope
    public JpaCursorItemReader<TransactionCategoryBalance> tcatBalReader() {
        return new JpaCursorItemReaderBuilder<TransactionCategoryBalance>()
                .name("tcatBalReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(
                    "SELECT t FROM TransactionCategoryBalance t " +
                    "ORDER BY t.id.transcatAcctId, t.id.transcatTypeCd, t.id.transcatCd")
                .build();
    }

    // ---------------------------------------------------------------
    // Processor — cross-reference lookup, interest calculation
    // Mirrors: paragraphs 1100, 1110, 1200, 1300, 1400
    // ---------------------------------------------------------------

    /**
     * Delegates all COBOL business logic to {@link Cbact04cService}.
     * Returns generated interest Transaction records; an empty list when
     * the interest rate is zero (no transaction written in COBOL either).
     */
    @Bean
    @StepScope
    public ItemProcessor<TransactionCategoryBalance, List<Transaction>> cbact04cProcessor() {
        return cbact04cService::processTransactionCategoryBalance;
    }

    // ---------------------------------------------------------------
    // Writer — persist interest transactions (TRANSACT-FILE output)
    // Mirrors: WRITE FD-TRANFILE-REC FROM TRAN-RECORD (paragraph 1300-B)
    // ---------------------------------------------------------------

    @Bean
    @StepScope
    public ItemWriter<List<Transaction>> cbact04cWriter() {
        return items -> {
            for (List<Transaction> batch : items) {
                if (batch != null && !batch.isEmpty()) {
                    transactionRepository.saveAll(batch);
                    log.debug("Persisted {} interest transaction(s)", batch.size());
                }
            }
        };
    }

    // ---------------------------------------------------------------
    // Step execution listener
    // Mirrors: end-of-file PERFORM 1050-UPDATE-ACCOUNT
    // ---------------------------------------------------------------

    /**
     * StepExecutionListener wires the run-date parameter into the service
     * before the step begins, and calls {@link Cbact04cService#finalizeLastAccount()}
     * after the last item is processed — mirroring the COBOL end-of-file
     * path that updates the final account group.
     */
    @Bean
    public StepExecutionListener cbact04cStepListener(
            @Value("#{jobParameters['runDate'] ?: ''}") String runDate) {
        return new StepExecutionListener() {

            @Override
            public void beforeStep(StepExecution stepExecution) {
                String date = stepExecution.getJobParameters()
                        .getString("runDate", "");
                log.info("CBACT04C step starting – runDate={}", date);
                cbact04cService.resetState(date);
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                log.info("CBACT04C step complete – finalising last account");
                cbact04cService.finalizeLastAccount();
                log.info("CBACT04C step finished – readCount={} writeCount={}",
                        stepExecution.getReadCount(), stepExecution.getWriteCount());
                return stepExecution.getExitStatus();
            }
        };
    }

    // ---------------------------------------------------------------
    // Step
    // ---------------------------------------------------------------

    @Bean
    public Step cbact04cStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager) {
        return new StepBuilder("cbact04cStep", jobRepository)
                .<TransactionCategoryBalance, List<Transaction>>chunk(100, transactionManager)
                .reader(tcatBalReader())
                .processor(cbact04cProcessor())
                .writer(cbact04cWriter())
                .listener(cbact04cStepListener(""))
                .build();
    }

    // ---------------------------------------------------------------
    // Job
    // ---------------------------------------------------------------

    /**
     * Spring Batch Job 'cbact04cJob'.
     *
     * <p>Accepts a JobParameter {@code runDate} (PIC X(10) equivalent of
     * PARM-DATE in the COBOL LINKAGE SECTION).
     */
    @Bean
    public Job cbact04cJob(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager) {
        return new JobBuilder("cbact04cJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(cbact04cStep(jobRepository, transactionManager))
                .build();
    }
}
