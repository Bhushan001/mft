package com.chrono.etl.service;

import com.chrono.commons.dto.PageRequest;
import com.chrono.commons.dto.PageResponse;
import com.chrono.commons.exception.ResourceNotFoundException;
import com.chrono.commons.service.BaseService;
import com.chrono.etl.batch.LoanProcessingJobConfig;
import com.chrono.etl.domain.entity.EtlJob;
import com.chrono.etl.domain.entity.EtlJobStatus;
import com.chrono.etl.domain.repository.EtlJobRepository;
import com.chrono.etl.dto.EtlJobResponse;
import com.chrono.etl.dto.SubmitEtlJobRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlJobService extends BaseService {

    private final EtlJobRepository etlJobRepository;
    private final JobLauncher jobLauncher;
    private final Job loanProcessingJob;

    @Transactional
    public EtlJobResponse submitJob(SubmitEtlJobRequest request, String tenantId, String callerRole) {
        // Idempotency check — (tenantId, sourceRef, batchDate) must be unique
        Optional<EtlJob> existing = etlJobRepository.findByTenantIdAndSourceRefAndBatchDate(
            tenantId, request.getSourceRef(), request.getBatchDate());

        if (existing.isPresent()) {
            EtlJob job = existing.get();
            if (EtlJobStatus.COMPLETED.equals(job.getStatus())) {
                log.info("Idempotent skip: tenantId={}, sourceRef={}, batchDate={}",
                    tenantId, request.getSourceRef(), request.getBatchDate());
                job.setStatus(EtlJobStatus.SKIPPED);
                return EtlJobResponse.from(job);
            }
            // Failed/running jobs are re-submitted via Spring Batch restart
            log.info("Re-submitting failed job: jobId={}", job.getJobId());
            launchBatchJob(job, request.getInputPayload(), callerRole);
            return EtlJobResponse.from(job);
        }

        // Create new job record
        EtlJob etlJob = new EtlJob();
        etlJob.setJobId(UUID.randomUUID().toString());
        etlJob.setTenantId(tenantId);
        etlJob.setSourceRef(request.getSourceRef());
        etlJob.setBatchDate(request.getBatchDate());
        etlJob.setInputPayload(request.getInputPayload());
        etlJob.setStatus(EtlJobStatus.SUBMITTED);
        etlJob.setSubmittedAt(LocalDateTime.now());
        etlJobRepository.save(etlJob);

        log.info("ETL job submitted: jobId={}, tenantId={}, sourceRef={}",
            etlJob.getJobId(), tenantId, request.getSourceRef());

        launchBatchJob(etlJob, request.getInputPayload(), callerRole);
        return EtlJobResponse.from(etlJob);
    }

    @Async("etlExecutor")
    public void launchBatchJob(EtlJob etlJob, String inputPayload, String callerRole) {
        etlJob.setStatus(EtlJobStatus.RUNNING);
        etlJobRepository.save(etlJob);

        try {
            JobParameters params = new JobParametersBuilder()
                .addString("tenantId", etlJob.getTenantId())
                .addString("jobId", etlJob.getJobId())
                .addString("sourceRef", etlJob.getSourceRef())
                .addString("batchDate", etlJob.getBatchDate())
                .addString("inputPayload", inputPayload != null ? inputPayload : "{}")
                .addLong("runAt", System.currentTimeMillis())
                .toJobParameters();

            org.springframework.batch.core.JobExecution execution =
                jobLauncher.run(loanProcessingJob, params);

            etlJob.setBatchJobInstanceId(execution.getJobInstance().getInstanceId());
            etlJob.setBatchJobExecutionId(execution.getId());

            if (execution.getStatus().isUnsuccessful()) {
                etlJob.setStatus(EtlJobStatus.FAILED);
                etlJob.setErrorMessage("Batch execution failed: " + execution.getStatus().name());
                log.error("ETL batch failed: jobId={}, batchStatus={}", etlJob.getJobId(), execution.getStatus());
            } else {
                etlJob.setStatus(EtlJobStatus.COMPLETED);
                etlJob.setCompletedAt(LocalDateTime.now());
                log.info("ETL batch completed: jobId={}", etlJob.getJobId());
            }
        } catch (Exception e) {
            etlJob.setStatus(EtlJobStatus.FAILED);
            etlJob.setErrorMessage(e.getMessage());
            log.error("ETL job launch failed: jobId={}, error={}", etlJob.getJobId(), e.getMessage(), e);
        }

        etlJobRepository.save(etlJob);
    }

    @Transactional(readOnly = true)
    public EtlJobResponse getJob(String jobId, String callerTenantId, String callerRole) {
        EtlJob job = etlJobRepository.findByJobId(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("EtlJob", jobId));
        assertTenantAccess(job.getTenantId(), callerTenantId, callerRole);
        return EtlJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public PageResponse<EtlJobResponse> listJobs(
            String tenantId, EtlJobStatus status, PageRequest pageRequest,
            String callerTenantId, String callerRole) {
        assertTenantAccess(tenantId, callerTenantId, callerRole);
        org.springframework.data.domain.Page<EtlJob> page = status != null
            ? etlJobRepository.findByTenantIdAndStatusAndDeletedAtIsNull(
                tenantId, status, pageRequest.toSpringPageRequest())
            : etlJobRepository.findByTenantIdAndDeletedAtIsNull(
                tenantId, pageRequest.toSpringPageRequest());
        return PageResponse.from(page.map(EtlJobResponse::from));
    }
}
