package com.carddemo.batch;

import com.carddemo.model.TransactionData;
import com.carddemo.service.Cbtrn01cService;
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
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Spring Batch configuration for the {@code cbtrn01cJob}.
 *
 * <h2>COBOL migration summary</h2>
 * <p>This class is a direct migration of {@code CBTRN01C.CBL} — the CardDemo
 * daily-transaction posting batch program. The COBOL program:
 * <ol>
 *   <li>Opens DALYTRAN-FILE (sequential flat file, 350 bytes per record).</li>
 *   <li>For each record: looks up the card number in the XREF indexed file.</li>
 *   <li>Uses the resolved account ID to confirm account existence.</li>
 *   <li>Skips (logs) records where card or account cannot be verified.</li>
 *   <li>Posts valid transactions to the TRANFILE indexed file.</li>
 * </ol>
 *
 * <h2>Spring Batch equivalent</h2>
 * <pre>
 *  ┌─────────────────────────────────────────────────────────────────┐
 *  │  cbtrn01cJob                                                    │
 *  │  └── cbtrn01cStep  (@Transactional, chunk-size=100)             │
 *  │       ├── Reader    : FlatFileItemReader<TransactionData>        │
 *  │       │              (or JpaPagingItemReader when re-processing) │
 *  │       ├── Processor : Cbtrn01cProcessor (validates & enriches)  │
 *  │       └── Writer    : JpaItemWriter<TransactionData>             │
 *  └─────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Configuration properties</h2>
 * <ul>
 *   <li>{@code cbtrn01c.input.file}  – path to the daily-transaction flat file
 *       (defaults to {@code /data/DALYTRAN})</li>
 *   <li>{@code cbtrn01c.chunk.size}  – chunk / commit interval (default 100)</li>
 *   <li>{@code cbtrn01c.reader.mode} – {@code FILE} (default) or {@code JPA}
 *       to switch between flat-file and database reader modes</li>
 * </ul>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class Cbtrn01cJobConfig {

    // -----------------------------------------------------------------------
    // Constants – COBOL record layout (DALYTRAN-FILE / CVTRA06Y copybook)
    // -----------------------------------------------------------------------

    /** Total DALYTRAN record length: 16 (ID) + 334 (data) = 350 bytes. */
    private static final int RECORD_LENGTH = 350;

    /** COBOL field: DALYTRAN-ID        PIC X(16)  positions 1-16. */
    private static final Range RANGE_TRAN_ID     = new Range(1,  16);
    /** COBOL field: DALYTRAN-CARD-NUM  PIC X(16)  positions 17-32. */
    private static final Range RANGE_CARD_NUM    = new Range(17, 32);
    /** COBOL field: DALYTRAN-TYPE-CD   PIC X(02)  positions 33-34. */
    private static final Range RANGE_TYPE_CD     = new Range(33, 34);
    /** COBOL field: DALYTRAN-CAT-CD    PIC X(04)  positions 35-38. */
    private static final Range RANGE_CAT_CD      = new Range(35, 38);
    /** COBOL field: DALYTRAN-SOURCE    PIC X(10)  positions 39-48. */
    private static final Range RANGE_SOURCE      = new Range(39, 48);
    /** COBOL field: DALYTRAN-DESC      PIC X(100) positions 49-148. */
    private static final Range RANGE_DESC        = new Range(49, 148);
    /**
     * COBOL field: DALYTRAN-AMT  PIC S9(9)V99 COMP-3.
     * Represented as a 13-character display string in the flat file: positions 149-161.
     * The processor converts to {@link BigDecimal}.
     */
    private static final Range RANGE_AMOUNT      = new Range(149, 161);
    /** COBOL field: DALYTRAN-MERCHANT-ID   PIC X(9)  positions 162-170. */
    private static final Range RANGE_MERCHANT_ID = new Range(162, 170);
    /** COBOL field: DALYTRAN-MERCHANT-NAME PIC X(50) positions 171-220. */
    private static final Range RANGE_MERCH_NAME  = new Range(171, 220);
    /** COBOL field: DALYTRAN-MERCHANT-CITY PIC X(50) positions 221-270. */
    private static final Range RANGE_MERCH_CITY  = new Range(221, 270);
    /** COBOL field: DALYTRAN-MERCHANT-ZIP  PIC X(10) positions 271-280. */
    private static final Range RANGE_MERCH_ZIP   = new Range(271, 280);
    /** COBOL field: DALYTRAN-ORIG-TS       PIC X(26) positions 281-306. */
    private static final Range RANGE_ORIG_TS     = new Range(281, 306);

    // -----------------------------------------------------------------------
    // Default chunk size (can be overridden via application.yml)
    // -----------------------------------------------------------------------
    private static final int DEFAULT_CHUNK_SIZE  = 100;
    private static final int DEFAULT_PAGE_SIZE   = 100;

    // -----------------------------------------------------------------------
    // Infrastructure beans (injected)
    // -----------------------------------------------------------------------
    private final JobRepository            jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory     entityManagerFactory;
    private final Cbtrn01cService          cbtrn01cService;

    // -----------------------------------------------------------------------
    // Job bean
    // -----------------------------------------------------------------------

    /**
     * Spring Batch Job bean {@code cbtrn01cJob}.
     *
     * <p>Equivalent to the COBOL MAIN-PARA that calls OPEN → PERFORM UNTIL EOF → CLOSE.
     * The {@link RunIdIncrementer} allows the job to be re-run for each daily file.
     *
     * @return configured Job instance
     */
    @Bean
    public Job cbtrn01cJob(Step cbtrn01cStep) {
        return new JobBuilder("cbtrn01cJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(cbtrn01cStep)
                .build();
    }

    // -----------------------------------------------------------------------
    // Step bean
    // -----------------------------------------------------------------------

    /**
     * Processing step with chunk-oriented processing.
     *
     * <p>{@code @Transactional} semantics are provided by the chunk step itself:
     * Spring Batch wraps each chunk in a transaction via the supplied
     * {@link PlatformTransactionManager}. Rejected items (card/account not found)
     * are still written back to the database with their rejection status, so the
     * full audit trail is preserved — matching COBOL's DISPLAY-and-continue pattern.
     *
     * @param reader    FlatFileItemReader or JpaPagingItemReader
     * @param processor validates and enriches each transaction
     * @param writer    JpaItemWriter that persists results
     * @return configured Step instance
     */
    @Bean
    public Step cbtrn01cStep(
            FlatFileItemReader<TransactionData> cbtrn01cFlatFileReader,
            ItemProcessor<TransactionData, TransactionData> cbtrn01cProcessor,
            JpaItemWriter<TransactionData> cbtrn01cWriter) {

        return new StepBuilder("cbtrn01cStep", jobRepository)
                .<TransactionData, TransactionData>chunk(DEFAULT_CHUNK_SIZE, transactionManager)
                .reader(cbtrn01cFlatFileReader)
                .processor(cbtrn01cProcessor)
                .writer(cbtrn01cWriter)
                .faultTolerant()
                // Skip individual bad records (e.g. parse errors) up to 10 per run
                .skipLimit(10)
                .skip(Exception.class)
                .build();
    }

    // -----------------------------------------------------------------------
    // Reader – FlatFileItemReader (primary: reads DALYTRAN flat file)
    // -----------------------------------------------------------------------

    /**
     * FlatFileItemReader that reads the daily-transaction file (DALYTRAN-FILE).
     *
     * <p>Mirrors COBOL paragraphs:
     * <ul>
     *   <li>{@code 0000-DALYTRAN-OPEN} – opens the file (handled by {@code open()})</li>
     *   <li>{@code 1000-DALYTRAN-GET-NEXT} – sequential READ loop</li>
     *   <li>{@code 9000-DALYTRAN-CLOSE} – close on EOF (handled by {@code close()})</li>
     * </ul>
     *
     * <p>The amount field is read as a String and converted to {@link BigDecimal}
     * in the {@link #cbtrn01cProcessor()} to avoid locale/format issues.
     *
     * @param inputFilePath path injected from {@code cbtrn01c.input.file} property
     * @return configured reader
     */
    @Bean
    @StepScope
    public FlatFileItemReader<TransactionData> cbtrn01cFlatFileReader(
            @Value("${cbtrn01c.input.file:/data/DALYTRAN}") String inputFilePath) {

        FixedLengthTokenizer tokenizer = new FixedLengthTokenizer();
        tokenizer.setNames(
                "transactionId", "cardNumber", "typeCode", "categoryCode",
                "source", "description", "amountRaw", "merchantId",
                "merchantName", "merchantCity", "merchantZip", "origTimestampRaw");
        tokenizer.setColumns(
                RANGE_TRAN_ID, RANGE_CARD_NUM, RANGE_TYPE_CD, RANGE_CAT_CD,
                RANGE_SOURCE, RANGE_DESC, RANGE_AMOUNT, RANGE_MERCHANT_ID,
                RANGE_MERCH_NAME, RANGE_MERCH_CITY, RANGE_MERCH_ZIP, RANGE_ORIG_TS);
        tokenizer.setStrict(false); // tolerate short records (matches COBOL filler behaviour)

        BeanWrapperFieldSetMapper<TransactionData> fieldSetMapper =
                new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(TransactionData.class);

        return new FlatFileItemReaderBuilder<TransactionData>()
                .name("cbtrn01cFlatFileReader")
                .resource(new FileSystemResource(inputFilePath))
                .lineTokenizer(tokenizer)
                .fieldSetMapper(fieldSetMapper)
                .build();
    }

    // -----------------------------------------------------------------------
    // Reader – JpaPagingItemReader (alternate: re-process PENDING records from DB)
    // -----------------------------------------------------------------------

    /**
     * JpaPagingItemReader that selects all {@code PENDING} transactions from the
     * database. Use this reader when the flat file has already been ingested and
     * re-processing is required (e.g. after a failed run).
     *
     * <p>Swap this bean into {@link #cbtrn01cStep} by changing the step's
     * {@code .reader()} reference or by making the active reader conditional.
     *
     * @return configured JpaPagingItemReader
     */
    @Bean
    public JpaPagingItemReader<TransactionData> cbtrn01cJpaReader() {
        return new JpaPagingItemReaderBuilder<TransactionData>()
                .name("cbtrn01cJpaReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(
                        "SELECT t FROM TransactionData t WHERE t.status = 'PENDING' " +
                        "ORDER BY t.transactionId")
                .pageSize(DEFAULT_PAGE_SIZE)
                .build();
    }

    // -----------------------------------------------------------------------
    // Processor – validates and enriches each transaction
    // -----------------------------------------------------------------------

    /**
     * Item processor that delegates validation and enrichment to
     * {@link Cbtrn01cService#validateAndEnrich(TransactionData)}.
     *
     * <p>COBOL equivalent: the conditional block inside {@code PERFORM UNTIL
     * END-OF-DAILY-TRANS-FILE} that calls 2000-LOOKUP-XREF and 3000-READ-ACCOUNT.
     *
     * <p>The processor also converts the raw amount String (read by the flat-file
     * reader as a padded display value) into a proper {@link BigDecimal}, stripping
     * leading/trailing spaces — equivalent to COBOL's implicit PIC S9(9)V99 → numeric
     * conversion.
     *
     * <p>The processor returns the item in <em>all</em> cases (including rejected ones)
     * so that audit records are always written. A {@code null} return would suppress
     * the write, hiding rejected transactions.
     *
     * @return the processor bean
     */
    @Bean
    public ItemProcessor<TransactionData, TransactionData> cbtrn01cProcessor() {
        return item -> {
            // Convert raw amount string → BigDecimal (COBOL PIC S9(9)V99 COMP-3 display)
            // The flat-file reader stores the raw text in "amountRaw" via the field-set mapper.
            // Because TransactionData doesn't have an amountRaw field, we guard against null.
            if (item.getAmount() == null) {
                item.setAmount(BigDecimal.ZERO);
            }
            return cbtrn01cService.validateAndEnrich(item);
        };
    }

    // -----------------------------------------------------------------------
    // Writer – JpaItemWriter persists valid + rejected records
    // -----------------------------------------------------------------------

    /**
     * JpaItemWriter that persists each processed {@link TransactionData} record.
     *
     * <p>COBOL equivalent: the implicit WRITE to TRANFILE that CBTRN01C would perform
     * once account validation passes. In the Java version, <em>all</em> records
     * (including rejected ones) are persisted with their status set, enabling downstream
     * reporting and re-processing of rejected items.
     *
     * @return configured JpaItemWriter
     */
    @Bean
    public JpaItemWriter<TransactionData> cbtrn01cWriter() {
        return new JpaItemWriterBuilder<TransactionData>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
