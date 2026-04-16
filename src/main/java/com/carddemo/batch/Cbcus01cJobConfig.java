package com.carddemo.batch;

import com.carddemo.model.CustomerData;
import com.carddemo.service.Cbcus01cService;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Cbcus01cJobConfig – Spring Batch configuration migrated from COBOL program CBCUS01C.
 *
 * <h2>COBOL → Spring Batch mapping</h2>
 * <pre>
 * COBOL construct                    Spring Batch equivalent
 * ---------------------------------  -------------------------------------------
 * OPEN INPUT CUSTFILE-FILE           JpaPagingItemReader initialises JPA cursor
 * READ CUSTFILE-FILE (sequential)    JpaPagingItemReader.read() – page by page
 * DISPLAY CUSTOMER-RECORD            Cbcus01cService.process() logs each record
 * Business validation / enrichment   Cbcus01cItemProcessor (delegates to service)
 * CLOSE CUSTFILE-FILE                Reader/writer lifecycle managed by Spring
 * ABEND on I/O error                 Step skip / retry / fault-tolerant policies
 * </pre>
 *
 * <h2>Job flow</h2>
 * <pre>
 *  cbcus01cJob
 *      └─ cbcus01cStep
 *              ├─ Reader    : JpaPagingItemReader&lt;CustomerData&gt;
 *              ├─ Processor : Cbcus01cItemProcessor (→ Cbcus01cService)
 *              └─ Writer    : JpaItemWriter&lt;CustomerData&gt;
 * </pre>
 */
@Configuration
public class Cbcus01cJobConfig {

    private static final Logger log = LoggerFactory.getLogger(Cbcus01cJobConfig.class);

    /** Page size for JPA paged reader – tunable via application property. */
    @Value("${cbcus01c.chunk-size:100}")
    private int chunkSize;

    /** Maximum number of records to skip before failing the step. */
    @Value("${cbcus01c.skip-limit:10}")
    private int skipLimit;

    private final EntityManagerFactory entityManagerFactory;
    private final Cbcus01cService cbcus01cService;

    @Autowired
    public Cbcus01cJobConfig(EntityManagerFactory entityManagerFactory,
                             Cbcus01cService cbcus01cService) {
        this.entityManagerFactory = entityManagerFactory;
        this.cbcus01cService = cbcus01cService;
    }

    // -------------------------------------------------------------------------
    // Job
    // -------------------------------------------------------------------------

    /**
     * Spring Batch {@link Job} bean for the CBCUS01C migration.
     *
     * <p>Uses a {@link RunIdIncrementer} so that the job can be re-executed with a new
     * {@code run.id} parameter – equivalent to the batch scheduler re-submitting the JCL.
     */
    @Bean
    public Job cbcus01cJob(JobRepository jobRepository, Step cbcus01cStep) {
        log.info("START OF EXECUTION OF PROGRAM CBCUS01C");
        return new JobBuilder("cbcus01cJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(cbcus01cStep)
                .build();
    }

    // -------------------------------------------------------------------------
    // Step
    // -------------------------------------------------------------------------

    /**
     * Single-step chunk-oriented step that reads, processes, and writes
     * {@link CustomerData} records.
     */
    @Bean
    public Step cbcus01cStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager) {
        return new StepBuilder("cbcus01cStep", jobRepository)
                .<CustomerData, CustomerData>chunk(chunkSize, transactionManager)
                .reader(cbcus01cItemReader())
                .processor(cbcus01cItemProcessor())
                .writer(cbcus01cItemWriter())
                /*
                 * Fault-tolerance mirrors the COBOL error-handling in Z-DISPLAY-IO-STATUS:
                 * log and skip up to `skipLimit` bad records rather than ABENDing immediately.
                 */
                .faultTolerant()
                .skipLimit(skipLimit)
                .skip(Exception.class)
                .build();
    }

    // -------------------------------------------------------------------------
    // Reader  –  replaces: OPEN INPUT + READ CUSTFILE-FILE (sequential KSDS)
    // -------------------------------------------------------------------------

    /**
     * JPA paged reader that sequentially reads all {@link CustomerData} rows,
     * ordered by {@code custId} – preserving the KSDS sequential-by-key ordering.
     *
     * <p>COBOL equivalent:
     * <pre>
     *   SELECT CUSTFILE-FILE ASSIGN TO CUSTFILE
     *          ORGANIZATION IS INDEXED
     *          ACCESS MODE  IS SEQUENTIAL
     *          RECORD KEY   IS FD-CUST-ID
     * </pre>
     */
    @Bean
    public JpaPagingItemReader<CustomerData> cbcus01cItemReader() {
        return new JpaPagingItemReaderBuilder<CustomerData>()
                .name("cbcus01cItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT c FROM CustomerData c ORDER BY c.custId ASC")
                .pageSize(chunkSize)
                .build();
    }

    // -------------------------------------------------------------------------
    // Processor  –  replaces: DISPLAY + business rules in PROCEDURE DIVISION
    // -------------------------------------------------------------------------

    /**
     * Item processor that delegates to {@link Cbcus01cService#process(CustomerData)}.
     *
     * <p>COBOL equivalent:
     * <pre>
     *   IF CUSTFILE-STATUS = '00'
     *       DISPLAY CUSTOMER-RECORD
     *   ...
     * </pre>
     * Returning {@code null} from the processor causes Spring Batch to filter the item
     * (i.e. it will not be passed to the writer) – analogous to the COBOL skip logic.
     */
    @Bean
    public ItemProcessor<CustomerData, CustomerData> cbcus01cItemProcessor() {
        return cbcus01cService::process;
    }

    // -------------------------------------------------------------------------
    // Writer  –  persists enriched records back to the database
    // -------------------------------------------------------------------------

    /**
     * JPA writer that merges the processed (enriched) {@link CustomerData} entities.
     *
     * <p>The original COBOL program only printed records to SYSOUT; in the modernised
     * version we also persist the enrichment flag and any corrected field values so
     * that downstream processes (e.g. card-account batch jobs) can rely on clean data.
     */
    @Bean
    public ItemWriter<CustomerData> cbcus01cItemWriter() {
        JpaItemWriter<CustomerData> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }
}
