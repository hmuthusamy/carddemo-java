package com.carddemo.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * Cbact01cJobListener – mirrors the COBOL DISPLAY statements at program boundaries.
 *
 * <pre>
 *   DISPLAY 'START OF EXECUTION OF PROGRAM CBACT01C'
 *   ...
 *   DISPLAY 'END OF EXECUTION OF PROGRAM CBACT01C'
 * </pre>
 *
 * Also logs any abnormal termination equivalent to the COBOL 9999-ABEND-PROGRAM
 * paragraph (DISPLAY 'ABENDING PROGRAM' / CALL 'CEE3ABD').
 */
@Slf4j
public class Cbact01cJobListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("START OF EXECUTION OF PROGRAM CBACT01C");
        log.info("Job '{}' starting. JobInstanceId={}",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobInstance().getInstanceId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        switch (jobExecution.getStatus()) {
            case COMPLETED ->
                log.info("END OF EXECUTION OF PROGRAM CBACT01C. "
                        + "Read={}, Written={}, Skipped={}",
                        jobExecution.getStepExecutions().stream()
                                .mapToLong(s -> s.getReadCount()).sum(),
                        jobExecution.getStepExecutions().stream()
                                .mapToLong(s -> s.getWriteCount()).sum(),
                        jobExecution.getStepExecutions().stream()
                                .mapToLong(s -> s.getSkipCount()).sum());
            case FAILED -> {
                // Equivalent to 9999-ABEND-PROGRAM:
                //   DISPLAY 'ABENDING PROGRAM'
                //   MOVE 999 TO ABCODE
                //   CALL 'CEE3ABD' USING ABCODE, TIMING
                log.error("ABENDING PROGRAM – CBACT01C job FAILED. ExitStatus={}",
                        jobExecution.getExitStatus().getExitDescription());
                jobExecution.getAllFailureExceptions()
                        .forEach(ex -> log.error("Failure cause: ", ex));
            }
            default ->
                log.warn("CBACT01C job ended with status: {}", jobExecution.getStatus());
        }
    }
}
