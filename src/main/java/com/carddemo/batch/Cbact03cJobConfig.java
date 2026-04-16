package com.carddemo.batch;

import com.carddemo.model.AccountData;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch job configuration for the CBACT03C interest calculation batch.
 *
 * <h2>COBOL origin</h2>
 * <pre>
 * CBACT03C.CBL  — Account cross-reference data reader (reads XREFFILE sequentially,
 *                 displays CARD-XREF-RECORD, copybook CVACT03Y).
 * CBACT04C.CBL  — Interest calculator batch: reads transaction-category balances,
 *                 looks up rates from DISCGRP-FILE, computes interest, updates
 *                 ACCOUNT-FILE, and writes interest transactions to TRANSACT-FILE.
 * </pre>
 *
 * <h2>Job design</h2>
 * <pre>
 * cbact03cJob
 *  └─ step: calculateInterestStep
 *      ├─ Reader    : JpaPagingItemReader&lt;AccountData&gt;
 *      │              – reads all active accounts in pages (replaces VSAM ACCOUNT-FILE)
 *      ├─ Processor : Cbact03cItemProcessor
 *      │              – looks up interest rate, computes monthly interest,
 *      │                updates balance, resets cycle accumulators
 *      └─ Writer    : JpaItemWriter&lt;AccountData&gt;
 *                     – merges updated accounts back (replaces VSAM REWRITE)
 * </pre>
 *
 * <h2>Key formula (from 1300-COMPUTE-INTEREST in CBACT04C)</h2>
 * <pre>
 *   COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL × DIS-INT-RATE) / 1200
 * </pre>
 *
 * <h2>JPQL query</h2>
 * Only active accounts are processed, matching the COBOL assumption that the
 * account file contains records that should receive interest.
 * JPQL: {@code SELECT a FROM AccountData a WHERE a.acctActiveStatus = 'Y'}
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class Cbact03cJobConfig {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Spring Batch job name — used for JobLauncher and monitoring. */
    public static final String JOB_NAME  = "cbact03cJob";

    /** Step name. */
    public static final String STEP_NAME = "calculateInterestStep";

    /**
     * Default chunk size: how many accounts are read, processed, and written per transaction.
     * Tune via {@code cbact03c.chunk-size} property (default: 100).
     */
    private static final int DEFAULT_CHUNK_SIZE = 100;

    /** Default JPA page size for the reader (should equal or be a multiple of chunk size). */
    private static final int DEFAULT_PAGE_SIZE  = 100;

    /** JPQL to read only active accounts — matches COBOL file content assumption. */
    private static final String ACTIVE_ACCOUNTS_JPQL =
            "SELECT a FROM AccountData a WHERE a.acctActiveStatus = 'Y' ORDER BY a.acctId";

    // -----------------------------------------------------------------------
    // Spring-injected beans
    // -----------------------------------------------------------------------

    private final EntityManagerFactory entityManagerFactory;
    private final Cbact03cItemProcessor cbact03cItemProcessor;

    @Value("${cbact03c.chunk-size:" + DEFAULT_CHUNK_SIZE + "}")
    private int chunkSize;

    @Value("${cbact03c.page-size:" + DEFAULT_PAGE_SIZE + "}")
    private int pageSize;

    // -----------------------------------------------------------------------
    // Job bean
    // -----------------------------------------------------------------------

    /**
     * Defines the {@code cbact03cJob} Spring Batch Job.
     *
     * <p>A {@link RunIdIncrementer} is attached so that the job can be re-run on demand
     * (mirroring repeated JCL batch submissions of CBACT03C/CBACT04C).
     */
    @Bean
    public Job cbact03cJob(JobRepository jobRepository, Step calculateInterestStep) {
        log.info("Configuring Spring Batch job '{}'", JOB_NAME);
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(calculateInterestStep)
                .build();
    }

    // -----------------------------------------------------------------------
    // Step bean
    // -----------------------------------------------------------------------

    /**
     * Single-step job: read → process → write, all within a transaction per chunk.
     *
     * <p>Fault tolerance: skip limit of 10 allows individual bad records to be logged
     * and skipped without aborting the entire run (the COBOL program would ABEND on any error).
     */
    @Bean
    public Step calculateInterestStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<AccountData, AccountData>chunk(chunkSize, transactionManager)
                .reader(accountDataReader())
                .processor(cbact03cItemProcessor)
                .writer(accountDataWriter())
                .faultTolerant()
                .skipLimit(10)
                .skip(Exception.class)
                .build();
    }

    // -----------------------------------------------------------------------
    // Reader bean
    // -----------------------------------------------------------------------

    /**
     * JPA paging reader for {@link AccountData}.
     *
     * <p>Replaces sequential VSAM ACCOUNT-FILE reads. Reading in pages prevents
     * out-of-memory issues with large account datasets — no equivalent concern in batch COBOL
     * because VSAM is always sequential/record-at-a-time.
     *
     * <p>JPQL: {@code SELECT a FROM AccountData a WHERE a.acctActiveStatus = 'Y'}
     */
    @Bean
    public JpaPagingItemReader<AccountData> accountDataReader() {
        return new JpaPagingItemReaderBuilder<AccountData>()
                .name("accountDataReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(ACTIVE_ACCOUNTS_JPQL)
                .pageSize(pageSize)
                .saveState(true)
                .build();
    }

    // -----------------------------------------------------------------------
    // Writer bean
    // -----------------------------------------------------------------------

    /**
     * JPA item writer for {@link AccountData}.
     *
     * <p>Performs a JPA {@code merge()} on each processed account, which translates to
     * an SQL UPDATE on the {@code account_data} table.
     *
     * <p>Mirrors COBOL:
     * <pre>
     *   REWRITE FD-ACCTFILE-REC FROM ACCOUNT-RECORD
     * </pre>
     */
    @Bean
    public JpaItemWriter<AccountData> accountDataWriter() {
        return new JpaItemWriterBuilder<AccountData>()
                .entityManagerFactory(entityManagerFactory)
                .usePersist(false)   // use merge() — records already exist (REWRITE semantics)
                .build();
    }
}
