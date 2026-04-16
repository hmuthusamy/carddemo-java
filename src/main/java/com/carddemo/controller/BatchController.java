package com.carddemo.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for triggering and monitoring Spring Batch jobs that were formerly
 * submitted as JCL job streams on the mainframe.
 *
 * <p><b>Supported job names</b> (maps directly to the Spring Batch {@link Job}
 * beans in {@link com.carddemo.batch.BatchJobLauncherConfig}):
 * <ul>
 *   <li>{@code cbact01cJob} – Account file processing</li>
 *   <li>{@code cbact02cJob} – Account details</li>
 *   <li>{@code cbact03cJob} – Account list</li>
 *   <li>{@code cbact04cJob} – Interest calculation  (INTCALC.jcl)</li>
 *   <li>{@code cbcus01cJob} – Customer file processing</li>
 *   <li>{@code cbtrn01cJob} – Transaction processing</li>
 *   <li>{@code cbtrn02cJob} – Post daily transactions  (POSTTRAN.jcl)</li>
 *   <li>{@code cbtrn03cJob} – Transaction report  (TRANREPT.jcl)</li>
 *   <li>{@code cbstm03aJob} – Statement generation  (CREASTMT.JCL)</li>
 *   <li>{@code cbexportJob} – Export customer data  (CBEXPORT.jcl)</li>
 *   <li>{@code cbimportJob} – Import customer data  (CBIMPORT.jcl)</li>
 * </ul>
 *
 * <p>All endpoints require {@code ROLE_ADMIN}.
 */
@RestController
@RequestMapping("/api/batch")
@Secured("ROLE_ADMIN")
public class BatchController {

    /** Names of all registered batch jobs, mirroring the JCL job stream inventory. */
    static final Set<String> REGISTERED_JOBS = Set.of(
            "cbact01cJob", "cbact02cJob", "cbact03cJob", "cbact04cJob",
            "cbcus01cJob",
            "cbtrn01cJob", "cbtrn02cJob", "cbtrn03cJob",
            "cbstm03aJob",
            "cbexportJob", "cbimportJob"
    );

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final Map<String, Job> jobRegistry;

    /**
     * Constructor injection.  Spring auto-populates {@code jobRegistry} with
     * all {@link Job} beans from the application context keyed by their bean name.
     *
     * <p>Spring will inject a {@code Map<String, Job>} automatically containing
     * all beans of type {@link Job} in the context.
     *
     * @param jobLauncher  async job launcher bean
     * @param jobExplorer  Spring Batch explorer for querying execution history
     * @param jobRegistry  map of all {@link Job} beans (bean-name → Job)
     */
    public BatchController(JobLauncher jobLauncher,
                           JobExplorer jobExplorer,
                           @org.springframework.beans.factory.annotation.Autowired
                           Map<String, Job> jobRegistry) {
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
        this.jobRegistry = jobRegistry;
    }

    // -----------------------------------------------------------------------
    // POST /api/batch/{jobName}
    // Replaces: submitting a JCL job to the internal reader (INTRDRJ1.JCL)
    // -----------------------------------------------------------------------

    /**
     * Trigger a named batch job asynchronously.
     *
     * <p>Equivalent to submitting the corresponding JCL job to the mainframe
     * internal reader.  A unique timestamp parameter is appended to allow the
     * same job to be relaunched multiple times (mirrors JCL CLASS= re-submit).
     *
     * @param jobName  Spring Batch job bean name (e.g. {@code cbtrn02cJob})
     * @param params   optional additional job parameters (key=value pairs)
     * @return 202 Accepted with the new execution ID, or 404 / 500 on error
     */
    @PostMapping("/{jobName}")
    public ResponseEntity<Map<String, Object>> launchJob(
            @PathVariable String jobName,
            @RequestParam(required = false) Map<String, String> params) {

        if (!REGISTERED_JOBS.contains(jobName)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Unknown job: " + jobName,
                                 "registeredJobs", REGISTERED_JOBS));
        }

        Job job = jobRegistry.get(jobName);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Job bean not found: " + jobName));
        }

        try {
            JobParametersBuilder builder = new JobParametersBuilder()
                    .addLong("launchTimestamp", System.currentTimeMillis());

            if (params != null) {
                params.forEach((k, v) -> {
                    if (!"launchTimestamp".equals(k)) {
                        builder.addString(k, v);
                    }
                });
            }

            JobParameters jobParameters = builder.toJobParameters();
            JobExecution execution = jobLauncher.run(job, jobParameters);

            Map<String, Object> body = new HashMap<>();
            body.put("jobName", jobName);
            body.put("executionId", execution.getId());
            body.put("status", execution.getStatus().name());
            body.put("startTime", execution.getStartTime() != null
                    ? execution.getStartTime().toString() : null);

            return ResponseEntity.accepted().body(body);

        } catch (Exception ex) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to launch job: " + jobName);
            error.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/batch/{jobName}/status/{executionId}
    // Replaces: querying job status via SDSF or JESMSGLG on the mainframe
    // -----------------------------------------------------------------------

    /**
     * Check the status of a previously launched batch job execution.
     *
     * <p>Equivalent to checking a job's status in SDSF H (held output) or
     * JESLOG on the mainframe.  Returns the {@link BatchStatus} along with
     * step-level summary information.
     *
     * @param jobName     Spring Batch job bean name
     * @param executionId the execution ID returned by {@link #launchJob}
     * @return 200 OK with status detail, or 404 if execution not found
     */
    @GetMapping("/{jobName}/status/{executionId}")
    public ResponseEntity<Map<String, Object>> getJobStatus(
            @PathVariable String jobName,
            @PathVariable Long executionId) {

        if (!REGISTERED_JOBS.contains(jobName)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Unknown job: " + jobName));
        }

        JobExecution execution = jobExplorer.getJobExecution(executionId);
        if (execution == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No execution found with id: " + executionId));
        }

        // Verify the execution belongs to the requested job
        if (!jobName.equals(execution.getJobInstance().getJobName())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error",
                            "Execution " + executionId + " does not belong to job " + jobName));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("jobName", execution.getJobInstance().getJobName());
        body.put("executionId", execution.getId());
        body.put("status", execution.getStatus().name());
        body.put("exitCode", execution.getExitStatus().getExitCode());
        body.put("exitDescription", execution.getExitStatus().getExitDescription());
        body.put("startTime", execution.getStartTime() != null
                ? execution.getStartTime().toString() : null);
        body.put("endTime", execution.getEndTime() != null
                ? execution.getEndTime().toString() : null);
        body.put("stepExecutions", execution.getStepExecutions().stream()
                .map(se -> {
                    Map<String, Object> step = new HashMap<>();
                    step.put("stepName", se.getStepName());
                    step.put("status", se.getStatus().name());
                    step.put("readCount", se.getReadCount());
                    step.put("writeCount", se.getWriteCount());
                    step.put("commitCount", se.getCommitCount());
                    step.put("skipCount", se.getSkipCount());
                    return step;
                })
                .toList());

        return ResponseEntity.ok(body);
    }
}
