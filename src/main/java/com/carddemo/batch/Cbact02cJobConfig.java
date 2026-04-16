package com.carddemo.batch;

import com.carddemo.model.CardData;
import com.carddemo.model.CardUpdateRequest;
import com.carddemo.service.Cbact02cService;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.BindException;

/**
 * Spring Batch configuration for the CBACT02C card-file batch job.
 *
 * <h2>COBOL → Java Mapping</h2>
 * <pre>
 *  COBOL CBACT02C.CBL                      Java (this class)
 *  ─────────────────────────────────────── ──────────────────────────────────────
 *  PERFORM 0000-CARDFILE-OPEN              FlatFileItemReader open (framework)
 *  PERFORM UNTIL END-OF-FILE = 'Y'         chunk-oriented step loop (chunk=100)
 *    PERFORM 1000-CARDFILE-GET-NEXT        FlatFileItemReader.read()
 *    IF END-OF-FILE = 'N' DISPLAY ...      Cbact02cItemProcessor.process()
 *  END-PERFORM                             (loop managed by batch framework)
 *  PERFORM 9000-CARDFILE-CLOSE             FlatFileItemReader close (framework)
 *  9999-ABEND-PROGRAM                      fault-tolerant skip / exception throw
 * </pre>
 *
 * <h2>Input File Layout (CVACT02Y copybook – 91 bytes/record)</h2>
 * <pre>
 *  Cols  1-16 : CARD-NUM             PIC X(16)
 *  Cols 17-27 : CARD-ACCT-ID         PIC 9(11)
 *  Cols 28-30 : CARD-CVV-CD          PIC 9(03)
 *  Cols 31-80 : CARD-EMBOSSED-NAME   PIC X(50)
 *  Cols 81-90 : CARD-EXPIRAION-DATE  PIC X(10)
 *  Col  91    : CARD-ACTIVE-STATUS   PIC X(01)
 * </pre>
 *
 * <h2>COMP-3 / Arithmetic</h2>
 * COBOL COMP-3 (packed decimal) arithmetic is handled in
 * {@link Cbact02cService} using {@link java.math.BigDecimal} with
 * {@link java.math.RoundingMode#HALF_UP} rounding where applicable.
 */
@Slf4j
@Configuration
public class Cbact02cJobConfig {

    // -------------------------------------------------------------------------
    // FieldSetMapper – custom type-safe mapping for CVACT02Y record layout
    // -------------------------------------------------------------------------

    /**
     * Stateless, thread-safe {@link FieldSetMapper} that converts raw
     * fixed-width string tokens into a typed {@link CardUpdateRequest}.
     *
     * <p>This replaces the generic {@code BeanWrapperFieldSetMapper} to
     * avoid relying on Spring's property-editor type coercion for numeric
     * COBOL fields (CARD-ACCT-ID PIC 9(11), CARD-CVV-CD PIC 9(03)).
     */
    static class CardUpdateRequestFieldSetMapper
            implements FieldSetMapper<CardUpdateRequest> {

        @Override
        public CardUpdateRequest mapFieldSet(FieldSet fs) throws BindException {
            // Each readString() call preserves leading/trailing spaces for
            // PIC X fields, mirroring COBOL MOVE SPACES behaviour.
            String cardNum  = fs.readString(0).trim();
            String acctRaw  = fs.readString(1).trim();
            String cvvRaw   = fs.readString(2).trim();
            String embName  = fs.readString(3).trim();
            String expiry   = fs.readString(4).trim();
            String status   = fs.readString(5).trim();

            Long    acctId = acctRaw.isEmpty()  ? null : Long.parseLong(acctRaw);
            Integer cvv    = cvvRaw.isEmpty()   ? null : Integer.parseInt(cvvRaw);

            return CardUpdateRequest.builder()
                    .cardNum(cardNum)
                    .cardAcctId(acctId)
                    .cardCvvCd(cvv)
                    .cardEmbossedName(embName)
                    .cardExpirationDate(expiry)
                    .cardActiveStatus(status.isEmpty() ? null : status)
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // Reader – maps to COBOL PERFORM 1000-CARDFILE-GET-NEXT
    // -------------------------------------------------------------------------

    /**
     * Fixed-width flat-file reader for the CARDFILE input dataset.
     * The file path is injected from job parameter {@code inputFile}.
     *
     * <p>{@code strict(false)} allows lines longer than the declared 91-char
     * range (e.g. lines with trailing CR/LF or padding) without throwing an
     * {@code IncorrectLineLengthException}, mirroring COBOL's tolerance of
     * FILLER bytes beyond the last defined field.
     */
    @Bean
    @StepScope
    public FlatFileItemReader<CardUpdateRequest> cbact02cItemReader(
            @Value("#{jobParameters['inputFile']}") Resource inputFile) {

        return new FlatFileItemReaderBuilder<CardUpdateRequest>()
                .name("cbact02cItemReader")
                .resource(inputFile)
                .strict(false)         // tolerate lines with trailing padding
                .fixedLength()
                    .columns(
                        new Range(1,  16),  // CARD-NUM
                        new Range(17, 27),  // CARD-ACCT-ID
                        new Range(28, 30),  // CARD-CVV-CD
                        new Range(31, 80),  // CARD-EMBOSSED-NAME
                        new Range(81, 90),  // CARD-EXPIRAION-DATE
                        new Range(91, 91)   // CARD-ACTIVE-STATUS
                    )
                    .names("cardNum", "cardAcctId", "cardCvvCd",
                           "cardEmbossedName", "cardExpirationDate",
                           "cardActiveStatus")
                    .fieldSetMapper(new CardUpdateRequestFieldSetMapper())
                    .strict(false)     // FixedLengthTokenizer strict=false
                .build();
    }

    // -------------------------------------------------------------------------
    // Processor – mirrors COBOL IF/EVALUATE business logic
    // -------------------------------------------------------------------------

    /**
     * Item processor: delegates to {@link Cbact02cService#processCardUpdate}
     * which preserves all COBOL IF/EVALUATE validation logic.
     *
     * <p>Returns {@code null} (skip) for structurally blank lines.
     * Re-throws {@link Cbact02cService.CardFileProcessingException} so the
     * fault-tolerant step skip policy can decide the outcome.
     */
    @Bean
    public ItemProcessor<CardUpdateRequest, CardData> cbact02cItemProcessor(
            Cbact02cService cbact02cService) {

        return request -> {
            // Filter out completely blank records (COBOL SPACE-FILLED lines)
            if (request.getCardNum() == null || request.getCardNum().isBlank()) {
                log.debug("CBACT02C – skipping blank record");
                return null;
            }
            try {
                return cbact02cService.processCardUpdate(request);
            } catch (Cbact02cService.CardFileProcessingException ex) {
                log.error("CBACT02C – processing error [APPL-RESULT={}]: {}",
                          ex.getApplResult(), ex.getMessage());
                throw ex;   // honour fault-tolerant skip policy
            }
        };
    }

    // -------------------------------------------------------------------------
    // Writer – JpaItemWriter persists updated CardData entities
    // -------------------------------------------------------------------------

    /**
     * JPA item writer calling {@code EntityManager.merge()} per chunk.
     * This is equivalent to the COBOL {@code REWRITE} verb that overwrites
     * an existing VSAM record with updated working-storage content.
     */
    @Bean
    public JpaItemWriter<CardData> cbact02cItemWriter(
            EntityManagerFactory entityManagerFactory) {

        return new JpaItemWriterBuilder<CardData>()
                .entityManagerFactory(entityManagerFactory)
                .usePersist(false)   // merge = upsert, mirrors COBOL REWRITE
                .build();
    }

    // -------------------------------------------------------------------------
    // Step – chunk-oriented, transactional
    // -------------------------------------------------------------------------

    /**
     * Single step for the CBACT02C job.
     *
     * <p>Chunk size 100 is a reasonable default matching a VSAM block size.
     * Spring Batch wraps each chunk commit in a transaction provided by the
     * injected {@link PlatformTransactionManager}, fulfilling the
     * {@code @Transactional} requirement.
     *
     * <p>Skip policy: up to 10 non-fatal
     * {@link Cbact02cService.CardFileProcessingException} per run, mirroring
     * the COBOL practice of logging bad records (DISPLAY) and continuing.
     */
    @Bean
    public Step cbact02cStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<CardUpdateRequest> cbact02cItemReader,
            ItemProcessor<CardUpdateRequest, CardData> cbact02cItemProcessor,
            JpaItemWriter<CardData> cbact02cItemWriter) {

        return new StepBuilder("cbact02cStep", jobRepository)
                .<CardUpdateRequest, CardData>chunk(100, transactionManager)
                .reader(cbact02cItemReader)
                .processor(cbact02cItemProcessor)
                .writer(cbact02cItemWriter)
                .faultTolerant()
                    .skip(Cbact02cService.CardFileProcessingException.class)
                    .skipLimit(10)
                .listener(new Cbact02cStepListener())
                .build();
    }

    // -------------------------------------------------------------------------
    // Job – 'cbact02cJob'
    // -------------------------------------------------------------------------

    /**
     * Spring Batch Job definition for CBACT02C.
     *
     * <p>A {@link RunIdIncrementer} allows the job to be re-submitted for
     * each new run (mirrors the COBOL batch scheduler re-run capability).
     */
    @Bean
    public Job cbact02cJob(
            JobRepository jobRepository,
            Step cbact02cStep) {

        return new JobBuilder("cbact02cJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(cbact02cStep)
                .build();
    }

    // -------------------------------------------------------------------------
    // Step listener – logs COBOL DISPLAY equivalents
    // -------------------------------------------------------------------------

    /**
     * Step listener that prints start/end messages mirroring the COBOL
     * {@code DISPLAY 'START OF EXECUTION...'} and
     * {@code DISPLAY 'END OF EXECUTION...'} statements.
     */
    static class Cbact02cStepListener implements StepExecutionListener {

        @Override
        public void beforeStep(StepExecution stepExecution) {
            log.info("START OF EXECUTION OF PROGRAM CBACT02C");
        }

        @Override
        public org.springframework.batch.core.ExitStatus afterStep(
                StepExecution stepExecution) {
            log.info("END OF EXECUTION OF PROGRAM CBACT02C"
                     + " – readCount={} writeCount={} skipCount={}",
                     stepExecution.getReadCount(),
                     stepExecution.getWriteCount(),
                     stepExecution.getSkipCount());
            return stepExecution.getExitStatus();
        }
    }
}
