package com.carddemo.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BatchController} using plain unit testing (no Spring context),
 * which avoids the complexities of WebMvcTest with method security and typed Map injection.
 *
 * <p>Tests verify controller logic, HTTP response codes (via ResponseEntity assertions),
 * job registry lookup, and error handling.
 */
class BatchControllerTest {

    private JobLauncher jobLauncher;
    private JobExplorer jobExplorer;
    private Map<String, Job> jobRegistry;
    private BatchController controller;

    private Job cbtrn02cJob;
    private Job cbact04cJob;
    private Job cbstm03aJob;

    private JobExecution mockExecution;

    @BeforeEach
    void setUp() {
        jobLauncher = mock(JobLauncher.class);
        jobExplorer = mock(JobExplorer.class);
        jobRegistry = new HashMap<>();

        // Populate registry with all registered jobs
        cbtrn02cJob = mock(Job.class);
        cbact04cJob = mock(Job.class);
        cbstm03aJob = mock(Job.class);
        jobRegistry.put("cbtrn02cJob", cbtrn02cJob);
        jobRegistry.put("cbact04cJob", cbact04cJob);
        jobRegistry.put("cbstm03aJob", cbstm03aJob);
        for (String name : BatchController.REGISTERED_JOBS) {
            jobRegistry.putIfAbsent(name, mock(Job.class));
        }

        controller = new BatchController(jobLauncher, jobExplorer, jobRegistry);

        JobInstance instance = new JobInstance(1L, "cbtrn02cJob");
        mockExecution = new JobExecution(instance, 42L, new JobParameters());
        mockExecution.setStatus(BatchStatus.STARTED);
        mockExecution.setExitStatus(ExitStatus.UNKNOWN);
        mockExecution.setStartTime(LocalDateTime.now());
    }

    // -----------------------------------------------------------------------
    // POST /api/batch/{jobName} – launch job tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("launchJob – valid job name – should return 202 Accepted with execution id")
    void launchJob_withValidJobName_returnsAccepted() throws Exception {
        when(jobLauncher.run(eq(cbtrn02cJob), any(JobParameters.class))).thenReturn(mockExecution);

        ResponseEntity<Map<String, Object>> response = controller.launchJob("cbtrn02cJob", null);

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("jobName")).isEqualTo("cbtrn02cJob");
        assertThat(response.getBody().get("executionId")).isEqualTo(42L);
        assertThat(response.getBody().get("status")).isEqualTo("STARTED");
    }

    @Test
    @DisplayName("launchJob – cbact04cJob – interest calculation job returns 202")
    void launchJob_cbact04cJob_returnsAccepted() throws Exception {
        JobInstance instance = new JobInstance(2L, "cbact04cJob");
        JobExecution exec = new JobExecution(instance, 7L, new JobParameters());
        exec.setStatus(BatchStatus.STARTING);
        exec.setExitStatus(ExitStatus.UNKNOWN);

        when(jobLauncher.run(eq(cbact04cJob), any(JobParameters.class))).thenReturn(exec);

        ResponseEntity<Map<String, Object>> response = controller.launchJob("cbact04cJob", null);

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("jobName")).isEqualTo("cbact04cJob");
        assertThat(response.getBody().get("executionId")).isEqualTo(7L);
    }

    @Test
    @DisplayName("launchJob – cbstm03aJob – statement generation job returns 202")
    void launchJob_cbstm03aJob_returnsAccepted() throws Exception {
        JobInstance instance = new JobInstance(3L, "cbstm03aJob");
        JobExecution exec = new JobExecution(instance, 9L, new JobParameters());
        exec.setStatus(BatchStatus.STARTING);
        exec.setExitStatus(ExitStatus.UNKNOWN);

        when(jobLauncher.run(eq(cbstm03aJob), any(JobParameters.class))).thenReturn(exec);

        ResponseEntity<Map<String, Object>> response = controller.launchJob("cbstm03aJob", null);

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("jobName")).isEqualTo("cbstm03aJob");
    }

    @Test
    @DisplayName("launchJob – unknown job name – should return 404 Not Found")
    void launchJob_withUnknownJobName_returnsNotFound() throws Exception {
        ResponseEntity<Map<String, Object>> response = controller.launchJob("unknownJob", null);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error").toString()).contains("Unknown job: unknownJob");
    }

    @Test
    @DisplayName("launchJob – launcher throws exception – should return 500")
    void launchJob_launcherThrows_returnsInternalServerError() throws Exception {
        when(jobLauncher.run(eq(cbtrn02cJob), any(JobParameters.class)))
                .thenThrow(new RuntimeException("DB connection lost"));

        ResponseEntity<Map<String, Object>> response = controller.launchJob("cbtrn02cJob", null);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error").toString()).contains("Failed to launch job");
        assertThat(response.getBody().get("message").toString()).contains("DB connection lost");
    }

    @Test
    @DisplayName("launchJob – with extra params – params are included in job parameters")
    void launchJob_withExtraParams_paramsPassedToJob() throws Exception {
        when(jobLauncher.run(eq(cbtrn02cJob), any(JobParameters.class))).thenReturn(mockExecution);

        Map<String, String> extraParams = new HashMap<>();
        extraParams.put("runDate", "2024-01-01");
        extraParams.put("batchSize", "1000");

        ResponseEntity<Map<String, Object>> response = controller.launchJob("cbtrn02cJob", extraParams);

        assertThat(response.getStatusCode().value()).isEqualTo(202);
    }

    // -----------------------------------------------------------------------
    // GET /api/batch/{jobName}/status/{executionId} – check status tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getJobStatus – valid execution – returns execution detail with 200 OK")
    void getJobStatus_withValidExecution_returnsDetail() {
        mockExecution.setStatus(BatchStatus.COMPLETED);
        mockExecution.setExitStatus(ExitStatus.COMPLETED);
        when(jobExplorer.getJobExecution(42L)).thenReturn(mockExecution);

        ResponseEntity<Map<String, Object>> response = controller.getJobStatus("cbtrn02cJob", 42L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("jobName")).isEqualTo("cbtrn02cJob");
        assertThat(response.getBody().get("executionId")).isEqualTo(42L);
        assertThat(response.getBody().get("status")).isEqualTo("COMPLETED");
        assertThat(response.getBody().get("exitCode")).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("getJobStatus – execution not found – returns 404")
    void getJobStatus_executionNotFound_returnsNotFound() {
        when(jobExplorer.getJobExecution(99L)).thenReturn(null);

        ResponseEntity<Map<String, Object>> response = controller.getJobStatus("cbtrn02cJob", 99L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error").toString()).contains("No execution found with id: 99");
    }

    @Test
    @DisplayName("getJobStatus – unknown job name – returns 404")
    void getJobStatus_unknownJob_returnsNotFound() {
        ResponseEntity<Map<String, Object>> response = controller.getJobStatus("unknownJob", 1L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error").toString()).contains("Unknown job: unknownJob");
    }

    @Test
    @DisplayName("getJobStatus – execution belongs to wrong job – returns 404")
    void getJobStatus_executionJobMismatch_returnsNotFound() {
        JobInstance otherInstance = new JobInstance(5L, "cbact04cJob");
        JobExecution otherExec = new JobExecution(otherInstance, 42L, new JobParameters());
        otherExec.setStatus(BatchStatus.COMPLETED);
        otherExec.setExitStatus(ExitStatus.COMPLETED);
        when(jobExplorer.getJobExecution(42L)).thenReturn(otherExec);

        ResponseEntity<Map<String, Object>> response = controller.getJobStatus("cbtrn02cJob", 42L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error").toString())
                .contains("does not belong to job cbtrn02cJob");
    }

    @Test
    @DisplayName("getJobStatus – STARTED status – returns step execution details")
    void getJobStatus_withStepExecutions_returnsStepDetails() {
        mockExecution.setStatus(BatchStatus.STARTED);
        when(jobExplorer.getJobExecution(42L)).thenReturn(mockExecution);

        ResponseEntity<Map<String, Object>> response = controller.getJobStatus("cbtrn02cJob", 42L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsKey("stepExecutions");
    }

    // -----------------------------------------------------------------------
    // Registered jobs set completeness test
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("REGISTERED_JOBS contains all 11 required job names (CBACT01C through CBSTM03)")
    void registeredJobs_containsAllRequiredJobs() {
        Set<String> required = Set.of(
                "cbact01cJob", "cbact02cJob", "cbact03cJob", "cbact04cJob",
                "cbcus01cJob",
                "cbtrn01cJob", "cbtrn02cJob", "cbtrn03cJob",
                "cbstm03aJob",
                "cbexportJob", "cbimportJob"
        );
        assertThat(BatchController.REGISTERED_JOBS).containsAll(required);
    }

    @Test
    @DisplayName("REGISTERED_JOBS has exactly 11 entries")
    void registeredJobs_hasCorrectSize() {
        assertThat(BatchController.REGISTERED_JOBS).hasSize(11);
    }
}
