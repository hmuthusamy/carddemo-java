package com.carddemo.batch;

import com.carddemo.model.AccountData;
import com.carddemo.model.CardXref;
import com.carddemo.model.CustomerData;
import com.carddemo.model.StatementRecord;
import com.carddemo.model.TransactionData;
import com.carddemo.service.StatementService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * StatementJobConfig – Spring Batch configuration for statement generation.
 *
 * Migrated from CBSTM03A.CBL + CBSTM03B.CBL.
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  Job: statementJob                                                  │
 * │  ┌─────────────────────────────────────────────────────────────┐   │
 * │  │  Step 1: generateStatementHeadersStep                       │   │
 * │  │  Source: CBSTM03A paragraphs 1000-MAINLINE,                 │   │
 * │  │          1000-XREFFILE-GET-NEXT (XREFFILE sequential read), │   │
 * │  │          2000-CUSTFILE-GET (CUSTFILE keyed read),           │   │
 * │  │          3000-ACCTFILE-GET (ACCTFILE keyed read),           │   │
 * │  │          5000-CREATE-STATEMENT                              │   │
 * │  │  Reads:  CardXref records (XREF-FILE sequential)            │   │
 * │  │  Processes: looks up CustomerData + AccountData by key      │   │
 * │  │  Writes:  StatementRecord list (in-memory for step 2)       │   │
 * │  └─────────────────────────────────────────────────────────────┘   │
 * │  ┌─────────────────────────────────────────────────────────────┐   │
 * │  │  Step 2: generateLineItemsStep                              │   │
 * │  │  Source: CBSTM03A paragraphs 4000-TRNXFILE-GET,            │   │
 * │  │          6000-WRITE-TRANS, ST-LINE14A total line            │   │
 * │  │          CBSTM03B paragraphs 1000-TRNXFILE-PROC             │   │
 * │  │  Reads:  StatementRecord list produced by step 1            │   │
 * │  │  Processes: matches transactions → enrichWithTransactions() │   │
 * │  │  Writes:  FlatFileItemWriter → formatted statement text     │   │
 * │  └─────────────────────────────────────────────────────────────┘   │
 * └─────────────────────────────────────────────────────────────────────┘
 *
 * Data providers (CardXrefDataProvider, CustomerDataProvider,
 * AccountDataProvider, TransactionDataProvider) are inner interfaces
 * whose implementations are supplied by the application context
 * (e.g., backed by Spring Data repositories or file readers) so the
 * batch configuration remains testable without a real database.
 */
@Configuration
public class StatementJobConfig {

    /* ------------------------------------------------------------------ */
    /*  Provider interfaces – decouple batch config from storage layer     */
    /* ------------------------------------------------------------------ */

    /** Provides ordered list of card cross-reference entries (XREF-FILE). */
    public interface CardXrefDataProvider {
        List<CardXref> findAll();
    }

    /** Provides customer data by customer ID (CUST-FILE keyed read). */
    public interface CustomerDataProvider {
        CustomerData findByCustomerId(String customerId);
    }

    /** Provides account data by account ID (ACCT-FILE keyed read). */
    public interface AccountDataProvider {
        AccountData findByAccountId(String accountId);
    }

    /**
     * Provides transaction data filtered by card number (TRNX-FILE).
     * Mirrors CBSTM03A WS-TRNX-TABLE / 8500-READTRNX-READ grouping.
     */
    public interface TransactionDataProvider {
        List<TransactionData> findByCardNumber(String cardNumber);
    }

    /* ------------------------------------------------------------------ */
    /*  Step-level execution context key for passing data between steps    */
    /* ------------------------------------------------------------------ */
    static final String CTX_STATEMENT_LIST = "statementHeaderList";

    /* ------------------------------------------------------------------ */
    /*  Injected dependencies                                              */
    /* ------------------------------------------------------------------ */

    private final StatementService statementService;
    private final CardXrefDataProvider cardXrefProvider;
    private final CustomerDataProvider customerProvider;
    private final AccountDataProvider accountProvider;
    private final TransactionDataProvider transactionProvider;

    @Autowired
    public StatementJobConfig(StatementService statementService,
                              CardXrefDataProvider cardXrefProvider,
                              CustomerDataProvider customerProvider,
                              AccountDataProvider accountProvider,
                              TransactionDataProvider transactionProvider) {
        this.statementService    = statementService;
        this.cardXrefProvider    = cardXrefProvider;
        this.customerProvider    = customerProvider;
        this.accountProvider     = accountProvider;
        this.transactionProvider = transactionProvider;
    }

    /* ------------------------------------------------------------------ */
    /*  Job definition                                                     */
    /* ------------------------------------------------------------------ */

    /**
     * Defines the {@code statementJob} with two sequential steps.
     *
     * <p>Mirrors the top-level CBSTM03A PROCEDURE DIVISION flow:
     * open files → 1000-MAINLINE (xref loop) → close files.</p>
     */
    @Bean
    public Job statementJob(JobRepository jobRepository,
                            Step generateStatementHeadersStep,
                            Step generateLineItemsStep) {
        return new JobBuilder("statementJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(generateStatementHeadersStep)
            .next(generateLineItemsStep)
            .build();
    }

    /* ------------------------------------------------------------------ */
    /*  Step 1 – generate statement headers (CBSTM03A paragraphs           */
    /*           1000-MAINLINE, 2000, 3000, 5000)                          */
    /* ------------------------------------------------------------------ */

    /**
     * Step 1: reads every {@link CardXref} record sequentially (mirrors
     * CBSTM03A 1000-XREFFILE-GET-NEXT / CBSTM03B 2000-XREFFILE-PROC),
     * performs keyed lookups for customer + account (mirrors
     * CBSTM03A 2000-CUSTFILE-GET, 3000-ACCTFILE-GET), and builds
     * {@link StatementRecord} headers (mirrors 5000-CREATE-STATEMENT).
     *
     * The resulting list is stored in the step execution context so
     * step 2 can consume it.
     */
    @Bean
    public Step generateStatementHeadersStep(JobRepository jobRepository,
                                             PlatformTransactionManager txManager) {
        return new StepBuilder("generateStatementHeadersStep", jobRepository)
            .<CardXref, StatementRecord>chunk(10, txManager)
            .reader(cardXrefReader())
            .processor(statementHeaderProcessor())
            .writer(statementHeaderContextWriter())
            .build();
    }

    /**
     * Reads CardXref entries sequentially.
     * Mirrors CBSTM03A 1000-XREFFILE-GET-NEXT calling CBSTM03B with XREFFILE/R.
     */
    @Bean
    @StepScope
    public ItemReader<CardXref> cardXrefReader() {
        return new ListItemReader<>(cardXrefProvider.findAll());
    }

    /**
     * For each CardXref: looks up customer + account, builds header.
     * Mirrors CBSTM03A:
     *   2000-CUSTFILE-GET  (CALL CBSTM03B USING WS-M03B-AREA with DD=CUSTFILE, OPER=K)
     *   3000-ACCTFILE-GET  (CALL CBSTM03B USING WS-M03B-AREA with DD=ACCTFILE, OPER=K)
     *   5000-CREATE-STATEMENT
     */
    @Bean
    public ItemProcessor<CardXref, StatementRecord> statementHeaderProcessor() {
        return xref -> {
            CustomerData customer = customerProvider.findByCustomerId(xref.getCustomerId());
            AccountData  account  = accountProvider.findByAccountId(xref.getAccountId());

            if (customer == null || account == null) {
                // Mirrors CBSTM03A 9999-ABEND-PROGRAM for missing records
                throw new IllegalStateException(
                    "Missing data for card=" + xref.getCardNumber()
                    + " custId=" + xref.getCustomerId()
                    + " acctId=" + xref.getAccountId());
            }
            return statementService.buildStatementHeader(
                xref.getCardNumber(), account, customer);
        };
    }

    /**
     * Accumulates headers into a job-scoped list stored in ExecutionContext.
     * This bridges step 1 output → step 2 input (replaces COBOL in-memory
     * WS-TRNX-TABLE that was populated in 8500-READTRNX-READ).
     */
    @Bean
    public ItemWriter<StatementRecord> statementHeaderContextWriter() {
        return chunk -> {
            // Store in a thread-local list so step 2 can read them
            StatementHeaderHolder.getHeaders().addAll(chunk.getItems());
        };
    }

    /* ------------------------------------------------------------------ */
    /*  Step 2 – generate line items (CBSTM03A paragraphs 4000, 6000;     */
    /*           CBSTM03B paragraph 1000-TRNXFILE-PROC)                   */
    /* ------------------------------------------------------------------ */

    /**
     * Step 2: for each statement header from step 1, fetches the matching
     * transactions and writes a fully-formatted statement to a flat file.
     *
     * Mirrors CBSTM03A:
     *   4000-TRNXFILE-GET  – match card in WS-CARD-TBL, loop over WS-TRAN-TBL
     *   ADD TRNX-AMT TO WS-TOTAL-AMT  (running subtotal → scale 2)
     *   6000-WRITE-TRANS   – write ST-LINE14 per transaction
     *   WRITE ST-LINE14A   – write total line
     *   WRITE ST-LINE15    – write end-of-statement marker
     */
    @Bean
    public Step generateLineItemsStep(JobRepository jobRepository,
                                      PlatformTransactionManager txManager,
                                      @Value("${statement.output.file:target/statements.txt}")
                                      String outputFilePath) {
        return new StepBuilder("generateLineItemsStep", jobRepository)
            .<StatementRecord, List<String>>chunk(1, txManager)
            .reader(statementHeaderReader())
            .processor(statementLineItemProcessor())
            .writer(statementFlatFileWriter(outputFilePath))
            .build();
    }

    /**
     * Reads headers accumulated by step 1 from the in-memory holder.
     */
    @Bean
    @StepScope
    public ItemReader<StatementRecord> statementHeaderReader() {
        return new ListItemReader<>(StatementHeaderHolder.getHeaders());
    }

    /**
     * Enriches each header with its transactions and produces formatted lines.
     *
     * Mirrors CBSTM03A 4000-TRNXFILE-GET:
     *   Loops WS-CARD-TBL looking for XREF-CARD-NUM match,
     *   then inner loop over WS-TRAN-TBL calling 6000-WRITE-TRANS,
     *   then accumulates WS-TOTAL-AMT (scale 2 BigDecimal).
     *
     * Mirrors CBSTM03B 1000-TRNXFILE-PROC:
     *   Sequential read of TRNX-FILE (replaced here by provider lookup).
     */
    @Bean
    public ItemProcessor<StatementRecord, List<String>> statementLineItemProcessor() {
        return header -> {
            // Fetch transactions for this card (CBSTM03B TRNXFILE sequential/keyed)
            List<TransactionData> txns = transactionProvider.findByCardNumber(
                header.getCardNumber());

            // Enrich header: attach transactions and compute running total
            // Mirrors: ADD TRNX-AMT TO WS-TOTAL-AMT (COMP-3 → BigDecimal scale 2)
            statementService.enrichWithTransactions(header, txns);

            // Build plain-text statement lines
            return statementService.buildPlainTextStatement(header);
        };
    }

    /**
     * Writes formatted statement lines to a flat file.
     *
     * Mirrors CBSTM03A: WRITE FD-STMTFILE-REC FROM ST-LINE* statements.
     * Each {@code List<String>} item is a full statement for one account;
     * lines are written one per file row (80 chars, mirrors FD-STMTFILE-REC PIC X(80)).
     */
    @Bean
    public ItemWriter<List<String>> statementFlatFileWriter(String outputFilePath) {
        // Flatten List<List<String>> → individual lines via a delegating writer
        FlatFileItemWriter<String> lineWriter = new FlatFileItemWriterBuilder<String>()
            .name("statementLineWriter")
            .resource(new FileSystemResource(outputFilePath))
            .lineAggregator(new PassThroughLineAggregator<>())
            .build();

        return chunk -> {
            List<String> allLines = new ArrayList<>();
            for (List<String> statementLines : chunk.getItems()) {
                allLines.addAll(statementLines);
            }
            // Delegate to FlatFileItemWriter
            org.springframework.batch.item.Chunk<String> lineChunk =
                new org.springframework.batch.item.Chunk<>(allLines);
            lineWriter.write(lineChunk);
        };
    }

    /* ------------------------------------------------------------------ */
    /*  StatementHeaderHolder – bridges step 1 → step 2                   */
    /* ------------------------------------------------------------------ */

    /**
     * Thread-local store used to pass statement headers from step 1 to step 2
     * within the same job execution, replacing the COBOL WS-TRNX-TABLE.
     *
     * In production this can be replaced by a job-scoped Spring bean or
     * by persisting intermediate results to a staging table.
     */
    public static final class StatementHeaderHolder {
        private static final ThreadLocal<List<StatementRecord>> HEADERS =
            ThreadLocal.withInitial(ArrayList::new);

        private StatementHeaderHolder() {}

        public static List<StatementRecord> getHeaders() {
            return HEADERS.get();
        }

        /** Must be called at the start of each job execution to avoid state leakage. */
        public static void clear() {
            HEADERS.get().clear();
        }
    }
}
