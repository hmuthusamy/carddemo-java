package com.carddemo.batch;

import com.carddemo.model.AccountData;
import com.carddemo.service.Cbact01cService;
import com.carddemo.service.Cbact01cService.AccountArrayRecord;
import com.carddemo.service.Cbact01cService.AccountOutputRecord;
import com.carddemo.service.Cbact01cService.VbrcRecord;
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
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

/**
 * Cbact01cJobConfig – Spring Batch job that migrates CBACT01C.CBL.
 *
 * <h2>COBOL Program Summary</h2>
 * <p>CBACT01C opens an indexed (VSAM KSDS) account file and reads every
 * record sequentially. For each record it:
 * <ol>
 *   <li>Displays all account fields (SYSOUT).</li>
 *   <li>Builds an output flat record (OUT-FILE) applying:
 *       <ul>
 *         <li>Date reformatting via assembler COBDATFT (ACCT-REISSUE-DATE).</li>
 *         <li>Default value 2525.00 for ACCT-CURR-CYC-DEBIT when zero.</li>
 *       </ul>
 *   </li>
 *   <li>Builds an array record (ARRY-FILE) with 5 slots,
 *       hard-coding slots 1–3 with fixed debit/balance values.</li>
 *   <li>Writes two variable-length VBRC records (12 and 39 bytes).</li>
 * </ol>
 *
 * <h2>Spring Batch Mapping</h2>
 * <pre>
 *   VSAM KSDS sequential read  →  JpaPagingItemReader&lt;AccountData&gt;
 *   1100 + 1300 + 1400 + 1500  →  Cbact01cItemProcessor   (@Transactional)
 *   1350 + 1450 + 1550 + 1575  →  Cbact01cItemWriter      (log + report file)
 * </pre>
 *
 * <h2>Datasource</h2>
 * <p>Production: PostgreSQL (configured via application.properties).
 * <p>Tests: H2 in-memory (configured in test application.properties).
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class Cbact01cJobConfig {

    // ---------------------------------------------------------------
    // Batch chunk size – matches typical mainframe block factor
    // ---------------------------------------------------------------
    private static final int CHUNK_SIZE = 100;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final Cbact01cService cbact01cService;

    // ---------------------------------------------------------------
    // Job Definition
    // ---------------------------------------------------------------

    /**
     * cbact01cJob – the top-level Spring Batch job.
     * Equivalent to the PROCEDURE DIVISION of CBACT01C:
     *   PERFORM 0000-ACCTFILE-OPEN
     *   PERFORM UNTIL END-OF-FILE = 'Y' ... (read/process loop)
     *   PERFORM 9000-ACCTFILE-CLOSE
     */
    @Bean
    public Job cbact01cJob(Step cbact01cStep) {
        log.info("START OF EXECUTION OF PROGRAM CBACT01C");
        return new JobBuilder("cbact01cJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(cbact01cStep)
                .listener(new Cbact01cJobListener())
                .build();
    }

    // ---------------------------------------------------------------
    // Step Definition
    // ---------------------------------------------------------------

    @Bean
    public Step cbact01cStep(
            JpaPagingItemReader<AccountData> cbact01cItemReader,
            Cbact01cItemProcessor cbact01cItemProcessor,
            Cbact01cItemWriter cbact01cItemWriter) {

        return new StepBuilder("cbact01cStep", jobRepository)
                .<AccountData, ProcessedAccountResult>chunk(CHUNK_SIZE, transactionManager)
                .reader(cbact01cItemReader)
                .processor(cbact01cItemProcessor)
                .writer(cbact01cItemWriter)
                .faultTolerant()
                .build();
    }

    // ---------------------------------------------------------------
    // Reader: JpaPagingItemReader (replaces VSAM KSDS sequential READ)
    // ---------------------------------------------------------------

    /**
     * Reads all AccountData rows from PostgreSQL in page-based batches,
     * ordered by acctId to preserve VSAM KSDS key-sequential semantics.
     *
     * COBOL equivalent:
     *   READ ACCTFILE-FILE INTO ACCOUNT-RECORD
     *   (ORGANIZATION IS INDEXED, ACCESS MODE IS SEQUENTIAL)
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<AccountData> cbact01cItemReader() {
        return new JpaPagingItemReaderBuilder<AccountData>()
                .name("cbact01cItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT a FROM AccountData a ORDER BY a.acctId ASC")
                .pageSize(CHUNK_SIZE)
                .build();
    }

    // ---------------------------------------------------------------
    // Processor: applies all COBOL business rules from paragraphs
    //            1100, 1300, 1400, 1500
    // ---------------------------------------------------------------

    /**
     * Cbact01cItemProcessor applies:
     * <ul>
     *   <li>1100-DISPLAY-ACCT-RECORD   – log account fields</li>
     *   <li>1300-POPUL-ACCT-RECORD     – build output record + date reformat + CYC-DEBIT default</li>
     *   <li>1400-POPUL-ARRAY-RECORD    – build 5-slot array record</li>
     *   <li>1500-POPUL-VBRC-RECORD     – build VBR-REC1 + VBR-REC2</li>
     * </ul>
     *
     * <p>Annotated {@link Transactional} to ensure lazy-loaded associations
     * (if any) are accessible within the same transaction.
     */
    @Bean
    public Cbact01cItemProcessor cbact01cItemProcessor() {
        return new Cbact01cItemProcessor(cbact01cService);
    }

    // ---------------------------------------------------------------
    // Writer: logs + writes to report file (replaces WRITE statements)
    // ---------------------------------------------------------------

    @Bean
    public Cbact01cItemWriter cbact01cItemWriter(
            @Value("${cbact01c.output.file:target/CBACT01C_OUTPUT.txt}") String outputFile) {
        return new Cbact01cItemWriter(outputFile);
    }

    // ===================================================================
    // Inner classes
    // ===================================================================

    /**
     * Result DTO carrying all three derived records for one account.
     * Passed from processor to writer within the chunk.
     */
    public record ProcessedAccountResult(
            AccountData source,
            AccountOutputRecord outputRecord,
            AccountArrayRecord arrayRecord,
            VbrcRecord vbrcRecord) {
    }

    // -------------------------------------------------------------------
    // ItemProcessor
    // -------------------------------------------------------------------

    @Slf4j
    public static class Cbact01cItemProcessor
            implements ItemProcessor<AccountData, ProcessedAccountResult> {

        private final Cbact01cService service;

        public Cbact01cItemProcessor(Cbact01cService service) {
            this.service = service;
        }

        /**
         * Processes one account record through all COBOL paragraphs.
         *
         * <p>Implements the conditional logic from 1000-ACCTFILE-GET-NEXT:
         * <pre>
         *   IF  ACCTFILE-STATUS = '00'
         *       PERFORM 1100-DISPLAY-ACCT-RECORD
         *       PERFORM 1300-POPUL-ACCT-RECORD
         *       PERFORM 1400-POPUL-ARRAY-RECORD
         *       PERFORM 1500-POPUL-VBRC-RECORD
         *   ...
         * </pre>
         *
         * @param account account entity read from PostgreSQL
         * @return {@link ProcessedAccountResult} or {@code null} to skip (filter)
         */
        @Override
        @Transactional
        public ProcessedAccountResult process(AccountData account) {
            if (account == null) {
                return null; // skip null records (APPL-EOF equivalent)
            }

            // ---- 1100-DISPLAY-ACCT-RECORD ----
            service.displayAccountRecord(account);

            // ---- 1300-POPUL-ACCT-RECORD ----
            // Includes COBDATFT date reformat + zero-CYC-DEBIT default
            AccountOutputRecord outputRecord = service.populateOutputRecord(account);

            // ---- 1400-POPUL-ARRAY-RECORD ----
            AccountArrayRecord arrayRecord = service.populateArrayRecord(account);

            // ---- 1500-POPUL-VBRC-RECORD ----
            // Passes the reformatted reissue date (already in outputRecord)
            VbrcRecord vbrcRecord = service.populateVbrcRecord(account,
                    outputRecord.getReissueDate());

            return new ProcessedAccountResult(account, outputRecord, arrayRecord, vbrcRecord);
        }
    }

    // -------------------------------------------------------------------
    // ItemWriter
    // -------------------------------------------------------------------

    /**
     * Cbact01cItemWriter writes each processed result to:
     * <ol>
     *   <li>SLF4J log (mirrors COBOL DISPLAY / batch SYSOUT).</li>
     *   <li>A flat text report file (mirrors OUT-FILE, ARRY-FILE, VBRC-FILE).</li>
     * </ol>
     *
     * Paragraphs replicated: 1350-WRITE-ACCT-RECORD, 1450-WRITE-ARRY-RECORD,
     * 1550-WRITE-VB1-RECORD, 1575-WRITE-VB2-RECORD.
     */
    @Slf4j
    public static class Cbact01cItemWriter
            implements ItemWriter<ProcessedAccountResult> {

        private final String outputFilePath;

        public Cbact01cItemWriter(String outputFilePath) {
            this.outputFilePath = outputFilePath;
        }

        /**
         * Writes all three record types for each account in the chunk.
         * File is opened in append mode so multiple chunks accumulate.
         */
        @Override
        public void write(Chunk<? extends ProcessedAccountResult> items) throws IOException {
            try (PrintWriter writer = new PrintWriter(
                    new BufferedWriter(new FileWriter(outputFilePath, true)))) {

                for (ProcessedAccountResult result : items) {
                    writeOutputRecord(writer, result.outputRecord());
                    writeArrayRecord(writer, result.arrayRecord());
                    writeVbrcRecords(writer, result.vbrcRecord());
                }
            }
        }

        // ---- 1350-WRITE-ACCT-RECORD (OUT-FILE) ----
        private void writeOutputRecord(PrintWriter pw, AccountOutputRecord rec) {
            // Format mirrors the FD OUT-ACCT-REC layout
            String line = String.format(
                    "OUTACCT|%011d|%1s|%s|%s|%s|%10s|%10s|%10s|%s|%s|%10s",
                    rec.getAcctId(),
                    nvl(rec.getActiveStatus()),
                    fmtBd(rec.getCurrBal()),
                    fmtBd(rec.getCreditLimit()),
                    fmtBd(rec.getCashCreditLimit()),
                    nvl(rec.getOpenDate()),
                    nvl(rec.getExpirationDate()),
                    nvl(rec.getReissueDate()),
                    fmtBd(rec.getCurrCycCredit()),
                    fmtBd(rec.getCurrCycDebit()),  // COMP-3
                    nvl(rec.getGroupId())
            );
            pw.println(line);
            log.debug("1350-WRITE-ACCT-RECORD: {}", line);
        }

        // ---- 1450-WRITE-ARRY-RECORD (ARRY-FILE) ----
        private void writeArrayRecord(PrintWriter pw, AccountArrayRecord arr) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("ARRYACCT|%011d", arr.getAcctId()));
            for (int i = 0; i < 5; i++) {
                // ARR-ACCT-CURR-BAL(i) and ARR-ACCT-CURR-CYC-DEBIT(i)
                sb.append(String.format("|%s|%s",
                        fmtBd(arr.getBals()[i]),
                        fmtBd(arr.getDebits()[i])));
            }
            pw.println(sb);
            log.debug("1450-WRITE-ARRY-RECORD: {}", sb);
        }

        // ---- 1550-WRITE-VB1-RECORD + 1575-WRITE-VB2-RECORD (VBRC-FILE) ----
        private void writeVbrcRecords(PrintWriter pw, VbrcRecord vbrc) {
            // VBR-REC1: WS-RECD-LEN = 12
            // 11-digit acct-id + 1-char active-status
            String vb1 = String.format("VB1|%011d%1s",
                    vbrc.getVb1AcctId(),
                    nvl(vbrc.getVb1ActiveStatus()));
            pw.println(vb1);
            log.debug("1550-WRITE-VB1-RECORD: {}", vb1);

            // VBR-REC2: WS-RECD-LEN = 39
            // 11-digit acct-id + 12-char curr-bal + 12-char credit-limit + 4-char yyyy
            String vb2 = String.format("VB2|%011d%s%s%4s",
                    vbrc.getVb2AcctId(),
                    fmtBd(vbrc.getVb2CurrBal()),
                    fmtBd(vbrc.getVb2CreditLimit()),
                    nvl(vbrc.getVb2ReissueYyyy()));
            pw.println(vb2);
            log.debug("1575-WRITE-VB2-RECORD: {}", vb2);
        }

        private String fmtBd(BigDecimal val) {
            if (val == null) return "+0000000000.00";
            String sign = val.signum() < 0 ? "-" : "+";
            BigDecimal abs = val.abs().setScale(2, java.math.RoundingMode.HALF_UP);
            String[] parts = abs.toPlainString().split("\\.");
            return sign + String.format("%010d", Long.parseLong(parts[0])) + "." + parts[1];
        }

        private String nvl(String s) {
            return s == null ? "" : s;
        }
    }
}
