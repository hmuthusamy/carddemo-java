package com.carddemo.batch;

import com.carddemo.model.AccountData;
import com.carddemo.model.AccountUpdateRequest;
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
 * Spring Batch configuration for the CBACT02C account-file batch job.
 *
 * <h2>COBOL → Java Mapping</h2>
 * <pre>
 *  COBOL CBACT02C.CBL                        Java (this class)
 *  ─────────────────────────────────────── ──────────────────────────────────────
 *  PERFORM 0000-CARDFILE-OPEN               FlatFileItemReader open (framework)
 *  PERFORM UNTIL END-OF-FILE = 'Y'          chunk-oriented step loop (chunk=100)
 *    PERFORM 1000-CARDFILE-GET-NEXT         FlatFileItemReader.read()
 *    IF END-OF-FILE = 'N' DISPLAY …         Cbact02cItemProcessor.process()
 *  END-PERFORM                              (loop managed by batch framework)
 *  PERFORM 9000-CARDFILE-CLOSE              FlatFileItemReader close (framework)
 *  9999-ABEND-PROGRAM                       fault-tolerant skip / exception throw
 * </pre>
 *
 * <h2>Input File Layout (CVACT01Y copybook — 96 bytes/record)</h2>
 * <pre>
 *  Cols  1-11 : ACCT-ID               PIC 9(11)
 *  Col   12   : ACCT-ACTIVE-STATUS    PIC X(01)
 *  Cols 13-25 : ACCT-CURR-BAL         PIC S9(10)V99  (13 chars: sign+10+dot+2)
 *  Cols 26-38 : ACCT-CREDIT-LIMIT     PIC S9(10)V99
 *  Cols 39-51 : ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99
 *  Cols 52-61 : ACCT-OPEN-DATE        PIC X(10)
 *  Cols 62-71 : ACCT-EXPIRAION-DATE   PIC X(10)
 *  Cols 72-81 : ACCT-REISSUE-DATE     PIC X(10)
 *  Cols 82-91 : ACCT-ADDR-ZIP         PIC X(10)
 *  Cols 92-101: ACCT-GROUP-ID         PIC X(10)
 * </pre>
 *
 * <h2>COMP-3 / Arithmetic</h2>
 * COBOL COMP-3 (packed decimal) arithmetic is handled in
 * {@link Cbact02cService} using {@link java.math.BigDecimal} with
 * {@link java.math.RoundingMode#HALF_UP} rounding, faithfully reproducing
 * COBOL's default rounding behaviour.
 */
@Slf4j
@Configuration
public class Cbact02cJobConfig {

    // -------------------------------------------------------------------------
    // FieldSetMapper – type-safe mapping for CVACT01Y account record layout
    // -------------------------------------------------------------------------

    /**
     * Stateless, thread-safe {@link FieldSetMapper} that converts raw
     * fixed-width string tokens into a typed {@link AccountUpdateRequest}.
     *
     * <p>String fields are trimmed (mirrors COBOL MOVE SPACES behaviour).
     * Numeric string fields (balance, limits) are kept as trimmed strings
     * so that {@link Cbact02cService} can parse them into
     * {@link java.math.BigDecimal} with correct HALF_UP rounding.
     */
    static class AccountUpdateRequestFieldSetMapper
            implements FieldSetMapper<AccountUpdateRequest> {

        @Override
        public AccountUpdateRequest mapFieldSet(FieldSet fs) throws BindException {
            String acctIdRaw    = fs.readString(0).trim();
            String status       = fs.readString(1).trim();
            String currBal      = fs.readString(2).trim();
            String creditLimit  = fs.readString(3).trim();
            String cashLimit    = fs.readString(4).trim();
            String openDate     = fs.readString(5).trim();
            String expiryDate   = fs.readString(6).trim();
            String reissueDate  = fs.readString(7).trim();
            String zip          = fs.readString(8).trim();
            String groupId      = fs.readString(9).trim();

            Long acctId = acctIdRaw.isEmpty() ? null : Long.parseLong(acctIdRaw);

            return AccountUpdateRequest.builder()
                    .acctId(acctId)
                    .acctActiveStatus(status.isEmpty()    ? null : status)
                    .acctCurrBal(currBal.isEmpty()        ? null : currBal)
                    .acctCreditLimit(creditLimit.isEmpty()? null : creditLimit)
                    .acctCashCreditLimit(cashLimit.isEmpty()? null : cashLimit)
                    .acctOpenDate(openDate.isEmpty()      ? null : openDate)
                    .acctExpirationDate(expiryDate.isEmpty()? null : expiryDate)
                    .acctReissueDate(reissueDate.isEmpty()? null : reissueDate)
                    .acctAddrZip(zip.isEmpty()            ? null : zip)
                    .acctGroupId(groupId.isEmpty()        ? null : groupId)
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // Reader – maps to COBOL PERFORM 1000-CARDFILE-GET-NEXT
    // -------------------------------------------------------------------------

    /**
     * Fixed-width flat-file reader for the account VSAM input dataset.
     * The file path is injected from job parameter {@code inputFile}.
     *
     * <p>{@code strict(false)} allows records with trailing padding beyond the
     * declared 101-character range (mirroring COBOL FILLER tolerance) without
     * throwing {@code IncorrectLineLengthException}.
     */
    @Bean
    @StepScope
    public FlatFileItemReader<AccountUpdateRequest> cbact02cItemReader(
            @Value("#{jobParameters['inputFile']}") Resource inputFile) {

        return new FlatFileItemReaderBuilder<AccountUpdateRequest>()
                .name("cbact02cItemReader")
                .resource(inputFile)
                .strict(false)
                .fixedLength()
                    .columns(
                        new Range(1,  11),  // ACCT-ID
                        new Range(12, 12),  // ACCT-ACTIVE-STATUS
                        new Range(13, 25),  // ACCT-CURR-BAL
                        new Range(26, 38),  // ACCT-CREDIT-LIMIT
                        new Range(39, 51),  // ACCT-CASH-CREDIT-LIMIT
                        new Range(52, 61),  // ACCT-OPEN-DATE
                        new Range(62, 71),  // ACCT-EXPIRAION-DATE
                        new Range(72, 81),  // ACCT-REISSUE-DATE
                        new Range(82, 91),  // ACCT-ADDR-ZIP
                        new Range(92, 101)  // ACCT-GROUP-ID
                    )
                    .names("acctId", "acctActiveStatus", "acctCurrBal",
                           "acctCreditLimit", "acctCashCreditLimit",
                           "acctOpenDate", "acctExpirationDate",
                           "acctReissueDate", "acctAddrZip", "acctGroupId")
                    .fieldSetMapper(new AccountUpdateRequestFieldSetMapper())
                    .strict(false)
                .build();
    }

    // -------------------------------------------------------------------------
    // Processor – mirrors COBOL IF/EVALUATE business logic
    // -------------------------------------------------------------------------

    /**
     * Item processor: delegates to {@link Cbact02cService#processAccountUpdate}
     * which preserves all COBOL IF/EVALUATE validation and arithmetic logic.
     *
     * <p>Returns {@code null} (skip) for structurally blank lines.
     * Re-throws {@link Cbact02cService.AccountFileProcessingException} so the
     * fault-tolerant step skip policy can decide the outcome, mirroring the
     * COBOL {@code 9999-ABEND-PROGRAM} path.
     */
    @Bean
    public ItemProcessor<AccountUpdateRequest, AccountData> cbact02cItemProcessor(
            Cbact02cService cbact02cService) {

        return request -> {
            // Filter out completely blank records (COBOL SPACE-FILLED lines)
            if (request.getAcctId() == null) {
                log.debug("CBACT02C – skipping blank account record");
                return null;
            }
            try {
                return cbact02cService.processAccountUpdate(request);
            } catch (Cbact02cService.AccountFileProcessingException ex) {
                log.error("CBACT02C – processing error [APPL-RESULT={}]: {}",
                          ex.getApplResult(), ex.getMessage());
                throw ex;   // honour fault-tolerant skip policy
            }
        };
    }

    // -------------------------------------------------------------------------
    // Writer – JpaItemWriter persists updated AccountData entities
    // -------------------------------------------------------------------------

    /**
     * JPA item writer calling {@code EntityManager.merge()} per chunk.
     *
     * <p>This is equivalent to the COBOL {@code REWRITE} verb that overwrites
     * an existing VSAM record with updated working-storage content.
     * {@code usePersist(false)} ensures merge (upsert) semantics so that
     * both new and existing account records are handled correctly.
     */
    @Bean
    public JpaItemWriter<AccountData> cbact02cItemWriter(
            EntityManagerFactory entityManagerFactory) {

        return new JpaItemWriterBuilder<AccountData>()
                .entityManagerFactory(entityManagerFactory)
                .usePersist(false)   // merge = upsert, mirrors COBOL REWRITE
                .build();
    }

    // -------------------------------------------------------------------------
    // Step – chunk-oriented, @Transactional per chunk
    // -------------------------------------------------------------------------

    /**
     * Single transactional step for the CBACT02C job.
     *
     * <p>Chunk size 100 is a reasonable default matching a typical VSAM block
     * size. Spring Batch wraps each chunk commit in a transaction provided by
     * the injected {@link PlatformTransactionManager}, satisfying the
     * {@code @Transactional} step requirement.
     *
     * <p>Skip policy: up to 10 non-fatal
     * {@link Cbact02cService.AccountFileProcessingException} per run, mirroring
     * the COBOL practice of logging bad records (DISPLAY) and continuing.
     */
    @Bean
    public Step cbact02cStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<AccountUpdateRequest> cbact02cItemReader,
            ItemProcessor<AccountUpdateRequest, AccountData> cbact02cItemProcessor,
            JpaItemWriter<AccountData> cbact02cItemWriter) {

        return new StepBuilder("cbact02cStep", jobRepository)
                .<AccountUpdateRequest, AccountData>chunk(100, transactionManager)
                .reader(cbact02cItemReader)
                .processor(cbact02cItemProcessor)
                .writer(cbact02cItemWriter)
                .faultTolerant()
                    .skip(Cbact02cService.AccountFileProcessingException.class)
                    .skipLimit(10)
                .listener(new Cbact02cStepListener())
                .build();
    }

    // -------------------------------------------------------------------------
    // Job – 'cbact02cJob'
    // -------------------------------------------------------------------------

    /**
     * Spring Batch Job definition for CBACT02C account data file update.
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
     * {@code DISPLAY 'START OF EXECUTION OF PROGRAM CBACT02C'} and
     * {@code DISPLAY 'END OF EXECUTION OF PROGRAM CBACT02C'} statements.
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
