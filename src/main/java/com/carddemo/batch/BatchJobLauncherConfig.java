package com.carddemo.batch;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.batch.BatchDataSourceScriptDatabaseInitializer;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch Job Launcher Configuration.
 *
 * <p>Maps JCL job streams to Spring Batch jobs:
 * <ul>
 *   <li>POSTTRAN.jcl  → STEP15 EXEC PGM=CBTRN02C → {@code cbtrn02cJob}</li>
 *   <li>INTCALC.jcl   → STEP15 EXEC PGM=CBACT04C → {@code cbact04cJob}</li>
 *   <li>TRANREPT.jcl  → STEP10R EXEC PGM=CBTRN03C → {@code cbtrn03cJob}</li>
 *   <li>CREASTMT.JCL  → STEP040 EXEC PGM=CBSTM03A → {@code cbstm03aJob}</li>
 *   <li>CBEXPORT.jcl  → STEP02 EXEC PGM=CBEXPORT  → {@code cbexportJob}</li>
 *   <li>CBIMPORT.jcl  → STEP01 EXEC PGM=CBIMPORT  → {@code cbimportJob}</li>
 * </ul>
 *
 * <p>JCL DD statements are mapped via {@link com.carddemo.config.BatchConfig}.
 *
 * @see com.carddemo.config.BatchConfig
 * @see com.carddemo.controller.BatchController
 */
@Configuration
@EnableConfigurationProperties(BatchProperties.class)
public class BatchJobLauncherConfig {

    // -----------------------------------------------------------------------
    // DataSource – points at the same PostgreSQL instance as the application.
    // Injected from spring.datasource.* so the batch meta-tables share the DB.
    // -----------------------------------------------------------------------

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/carddemo}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:carddemo}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:changeme}")
    private String datasourcePassword;

    /**
     * Primary DataSource used for both JPA entities and Spring Batch meta-tables.
     * Mapped from the same PostgreSQL connection properties already present in
     * application.yml (which replaced JCL STEPLIB DSN= / DB2 subsystem pointers).
     */
    @Bean
    @Qualifier("batchDataSource")
    public DataSource batchDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(datasourceUrl);
        ds.setUsername(datasourceUsername);
        ds.setPassword(datasourcePassword);
        return ds;
    }

    /**
     * Transaction manager bound to the batch DataSource.
     */
    @Bean
    @Qualifier("batchTransactionManager")
    public PlatformTransactionManager batchTransactionManager(
            @Qualifier("batchDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * JobRepository backed by PostgreSQL.
     * Spring Batch meta-tables (BATCH_JOB_INSTANCE, BATCH_JOB_EXECUTION, etc.)
     * are created via {@link #batchDataSourceInitializer}.
     */
    @Bean
    public JobRepository jobRepository(
            @Qualifier("batchDataSource") DataSource dataSource,
            @Qualifier("batchTransactionManager") PlatformTransactionManager txManager)
            throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(txManager);
        factory.setDatabaseType("POSTGRES");
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    /**
     * Initialises Spring Batch schema in PostgreSQL.
     * Replaces JCL IDCAMS DEFINE CLUSTER / dataset allocation statements used
     * on the mainframe to set up work files.
     *
     * <p>Controlled by {@code spring.batch.jdbc.initialize-schema=always} in
     * application.yml.
     */
    @Bean
    public BatchDataSourceScriptDatabaseInitializer batchDataSourceInitializer(
            @Qualifier("batchDataSource") DataSource dataSource,
            BatchProperties batchProperties) {
        return new BatchDataSourceScriptDatabaseInitializer(dataSource, batchProperties.getJdbc());
    }

    /**
     * Asynchronous JobLauncher.
     *
     * <p>On the mainframe JCL jobs ran under their own address space submitted
     * by the internal reader (INTRDRJ1.JCL / INTRDRJ2.JCL).  Here we replicate
     * that non-blocking behaviour with {@link SimpleAsyncTaskExecutor} so that
     * the REST caller receives a response immediately while the job runs in the
     * background.
     */
    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(new SimpleAsyncTaskExecutor("batch-"));
        launcher.afterPropertiesSet();
        return launcher;
    }

    // -----------------------------------------------------------------------
    // Jobs – one per mainframe JCL job stream / EXEC PGM= invocation.
    // Each job contains one Step that delegates to the corresponding
    // service bean (or is a stub pending service-layer implementation).
    // -----------------------------------------------------------------------

    // ----- CBACT01C (Account file processing) ------------------------------
    // Source JCL: referenced as dependency of POSTTRAN / data initialisation
    @Bean
    public Step cbact01cStep(JobRepository jobRepository,
            PlatformTransactionManager txManager) {
        return new StepBuilder("cbact01cStep", jobRepository)
                .tasklet((contribution, context) -> {
                    // TODO: delegate to Cbact01cService
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    @Bean
    public Job cbact01cJob(JobRepository jobRepository,
            @Qualifier("cbact01cStep") Step step) {
        return new JobBuilder("cbact01cJob", jobRepository)
                .start(step)
                .build();
    }

    // ----- CBACT02C (Account details) --------------------------------------
    @Bean
    public Step cbact02cStep(JobRepository jobRepository,
            PlatformTransactionManager txManager) {
        return new StepBuilder("cbact02cStep", jobRepository)
                .tasklet((contribution, context) -> {
                    // TODO: delegate to Cbact02cService
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    @Bean
    public Job cbact02cJob(JobRepository jobRepository,
            @Qualifier("cbact02cStep") Step step) {
        return new JobBuilder("cbact02cJob", jobRepository)
                .start(step)
                .build();
    }

    // ----- CBACT03C (Account list) -----------------------------------------
    @Bean
    public Step cbact03cStep(JobRepository jobRepository,
            PlatformTransactionManager txManager) {
        return new StepBuilder("cbact03cStep", jobRepository)
                .tasklet((contribution, context) -> {
                    // TODO: delegate to Cbact03cService
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    @Bean
    public Job cbact03cJob(JobRepository jobRepository,
            @Qualifier("cbact03cStep") Step step) {
        return new JobBuilder("cbact03cJob", jobRepository)
                .start(step)
                .build();
    }

    // ----- CBACT04C (Interest calculation) --------------------------------
    // Source JCL: INTCALC.jcl – STEP15 EXEC PGM=CBACT04C,PARM='2022071800'
    // DD: TCATBALF, XREFFILE, XREFFIL1, ACCTFILE, DISCGRP → TRANSACT (output)
    @Bean
    public Step cbact04cStep(JobRepository jobRepository,
            PlatformTransactionManager txManager) {
        return new StepBuilder("cbact04cStep", jobRepository)
                .tasklet((contribution, context) -> {
                    // TODO: delegate to Cbact04cService (interest + fees)
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    @Bean
    public Job cbact04cJob(JobRepository jobRepository,
            @Qualifier("cbact04cStep") Step step) {
        return new JobBuilder("cbact04cJob", jobRepository)
                .start(step)
                .build();
    }

    // ----- CBCUS01C (Customer file processing) ----------------------------
    @Bean
    public Step cbcus01cStep(JobRepository jobRepository,
            PlatformTransactionManager txManager) {
        return new StepBuilder("cbcus01cStep", jobRepository)
                .tasklet((contribution, context) -> {
                    // TODO: delegate to Cbcus01cService
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    @Bean
    public Job cbcus01cJob(JobRepository jobRepository,
            @Qualifier("cbcus01cStep") Step step) {
        return new JobBuilder("cbcus01cJob", jobRepository)
                .start(step)
                .build();
    }

    // ----- CBTRN01C (Transaction processing – stub) -----------------------
    @Bean
    public Step cbtrn01cStep(JobRepository jobRepository,
            PlatformTransactionManager txManager) {
        return new StepBuilder("cbtrn01cStep", jobRepository)
                .tasklet((contribution, context) -> {
                    // TODO: delegate to Cbtrn01cService
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    @Bean
    public Job cbtrn01cJob(JobRepository jobRepository,
            @Qualifier("cbtrn01cStep") Step step) {
        return new JobBuilder("cbtrn01cJob", jobRepository)
                .start(step)
                .build();
    }

    // ----- CBTRN02C (Post transactions) ------------------------------------
    // Source JCL: POSTTRAN.jcl – STEP15 EXEC PGM=CBTRN02C
    // DD: TRANFILE, DALYTRAN, XREFFILE → DALYREJS (output), ACCTFILE, TCATBALF
    @Bean
    public Step cbtrn02cStep(JobRepository jobRepository,
            PlatformTransactionManager txManager) {
        return new StepBuilder("cbtrn02cStep", jobRepository)
                .tasklet((contribution, context) -> {
                    // TODO: delegate to Cbtrn02cService (daily transaction posting)
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    @Bean
    public Job cbtrn02cJob(JobRepository jobRepository,
            @Qualifier("cbtrn02cStep") Step step) {
        return new JobBuilder("cbtrn02cJob", jobRepository)
                .start(step)
                .build();
    }

    // ----- CBTRN03C (Transaction report) -----------------------------------
    // Source JCL: TRANREPT.jcl – STEP10R EXEC PGM=CBTRN03C
    // DD: TRANFILE, CARDXREF, TRANTYPE, TRANCATG, DATEPARM → TRANREPT (output)
    @Bean
    public Step cbtrn03cStep(JobRepository jobRepository,
            PlatformTransactionManager txManager) {
        return new StepBuilder("cbtrn03cStep", jobRepository)
                .tasklet((contribution, context) -> {
                    // TODO: delegate to Cbtrn03cService (transaction report)
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    @Bean
    public Job cbtrn03cJob(JobRepository jobRepository,
            @Qualifier("cbtrn03cStep") Step step) {
        return new JobBuilder("cbtrn03cJob", jobRepository)
                .start(step)
                .build();
    }

    // ----- CBSTM03A (Statement generation) --------------------------------
    // Source JCL: CREASTMT.JCL – STEP040 EXEC PGM=CBSTM03A
    // DD: TRNXFILE, XREFFILE, ACCTFILE, CUSTFILE → STMTFILE, HTMLFILE (outputs)
    @Bean
    public Step cbstm03aStep(JobRepository jobRepository,
            PlatformTransactionManager txManager) {
        return new StepBuilder("cbstm03aStep", jobRepository)
                .tasklet((contribution, context) -> {
                    // TODO: delegate to Cbstm03aService (statement generator)
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    @Bean
    public Job cbstm03aJob(JobRepository jobRepository,
            @Qualifier("cbstm03aStep") Step step) {
        return new JobBuilder("cbstm03aJob", jobRepository)
                .start(step)
                .build();
    }

    // ----- CBEXPORT (Export customer data) --------------------------------
    // Source JCL: CBEXPORT.jcl – STEP02 EXEC PGM=CBEXPORT
    // DD: CUSTFILE, ACCTFILE, XREFFILE, TRANSACT, CARDFILE → EXPFILE (output)
    @Bean
    public Step cbexportStep(JobRepository jobRepository,
            PlatformTransactionManager txManager) {
        return new StepBuilder("cbexportStep", jobRepository)
                .tasklet((contribution, context) -> {
                    // TODO: delegate to CbexportService
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    @Bean
    public Job cbexportJob(JobRepository jobRepository,
            @Qualifier("cbexportStep") Step step) {
        return new JobBuilder("cbexportJob", jobRepository)
                .start(step)
                .build();
    }

    // ----- CBIMPORT (Import customer data) --------------------------------
    // Source JCL: CBIMPORT.jcl – STEP01 EXEC PGM=CBIMPORT
    // DD: EXPFILE → CUSTOUT, ACCTOUT, XREFOUT, TRNXOUT, ERROUT (outputs)
    @Bean
    public Step cbimportStep(JobRepository jobRepository,
            PlatformTransactionManager txManager) {
        return new StepBuilder("cbimportStep", jobRepository)
                .tasklet((contribution, context) -> {
                    // TODO: delegate to CbimportService
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    @Bean
    public Job cbimportJob(JobRepository jobRepository,
            @Qualifier("cbimportStep") Step step) {
        return new JobBuilder("cbimportJob", jobRepository)
                .start(step)
                .build();
    }
}
