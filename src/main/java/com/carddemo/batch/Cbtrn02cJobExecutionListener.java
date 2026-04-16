package com.carddemo.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Job-level listener that prints the final summary counters.
 *
 * <p>Mirrors the COBOL end-of-job DISPLAY statements:</p>
 * <pre>
 *   DISPLAY 'TRANSACTIONS PROCESSED :' WS-TRANSACTION-COUNT
 *   DISPLAY 'TRANSACTIONS REJECTED  :' WS-REJECT-COUNT
 *   IF WS-REJECT-COUNT > 0
 *      MOVE 4 TO RETURN-CODE
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
public class Cbtrn02cJobExecutionListener implements JobExecutionListener {

    private final AtomicLong processedCount;
    private final AtomicLong skippedCount;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("START OF EXECUTION OF JOB cbtrn02cJob (runId={})",
                jobExecution.getJobParameters().getLong("run.id"));
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("TRANSACTIONS PROCESSED : {}", processedCount.get());
        log.info("TRANSACTIONS REJECTED  : {}", skippedCount.get());
        if (skippedCount.get() > 0) {
            log.warn("Job completed with {} rejected transaction(s). Exit code = COMPLETED WITH SKIPS",
                    skippedCount.get());
        }
        log.info("END OF EXECUTION OF JOB cbtrn02cJob – status={}",
                jobExecution.getStatus());
    }
}
