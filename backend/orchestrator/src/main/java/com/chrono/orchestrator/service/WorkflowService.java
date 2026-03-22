package com.chrono.orchestrator.service;

import com.chrono.commons.dto.ApiResponse;
import com.chrono.commons.dto.PageRequest;
import com.chrono.commons.dto.PageResponse;
import com.chrono.commons.exception.BusinessException;
import com.chrono.commons.exception.ResourceNotFoundException;
import com.chrono.commons.service.BaseService;
import com.chrono.orchestrator.client.EngineServiceClient;
import com.chrono.orchestrator.client.EtlServiceClient;
import com.chrono.orchestrator.client.dto.EtlJobDto;
import com.chrono.orchestrator.client.dto.ProcessRequestDto;
import com.chrono.orchestrator.client.dto.ProcessResponseDto;
import com.chrono.orchestrator.client.dto.SubmitEtlJobDto;
import com.chrono.orchestrator.domain.entity.*;
import com.chrono.orchestrator.domain.repository.WorkflowRepository;
import com.chrono.orchestrator.domain.repository.WorkflowStepRepository;
import com.chrono.orchestrator.dto.StartWorkflowRequest;
import com.chrono.orchestrator.dto.WorkflowResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService extends BaseService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository stepRepository;
    private final EngineServiceClient engineServiceClient;
    private final EtlServiceClient etlServiceClient;

    @Transactional
    public WorkflowResponse startWorkflow(StartWorkflowRequest request, String tenantId, String callerRole) {
        Workflow workflow = new Workflow();
        workflow.setWorkflowId(UUID.randomUUID().toString());
        workflow.setTenantId(tenantId);
        workflow.setWorkflowType(request.getWorkflowType());
        workflow.setStatus(WorkflowStatus.PENDING);
        workflow.setCorrelationId(request.getCorrelationId());
        workflow.setInputPayload(request.getInputPayload());
        workflow.setStartedAt(LocalDateTime.now());
        workflowRepository.save(workflow);

        log.info("Workflow started: workflowId={}, type={}, tenantId={}",
            workflow.getWorkflowId(), workflow.getWorkflowType(), tenantId);

        executeWorkflowAsync(workflow.getWorkflowId(), tenantId, callerRole);
        return WorkflowResponse.from(workflow);
    }

    @Async("orchestratorExecutor")
    public void executeWorkflowAsync(String workflowId, String tenantId, String callerRole) {
        Workflow workflow = workflowRepository.findByWorkflowId(workflowId)
            .orElseThrow(() -> new ResourceNotFoundException("Workflow", workflowId));
        workflow.setStatus(WorkflowStatus.RUNNING);
        workflowRepository.save(workflow);

        try {
            if ("LOAN_PROCESSING".equals(workflow.getWorkflowType())) {
                executeLoanProcessingWorkflow(workflow, tenantId, callerRole);
            } else if ("ETL_PIPELINE".equals(workflow.getWorkflowType())) {
                executeEtlPipelineWorkflow(workflow, tenantId, callerRole);
            } else {
                throw new BusinessException("Unknown workflow type: " + workflow.getWorkflowType());
            }
            workflow.setStatus(WorkflowStatus.COMPLETED);
            workflow.setCompletedAt(LocalDateTime.now());
            log.info("Workflow completed: workflowId={}", workflowId);
        } catch (Exception e) {
            workflow.setStatus(WorkflowStatus.FAILED);
            workflow.setErrorMessage(e.getMessage());
            workflow.setCompletedAt(LocalDateTime.now());
            log.error("Workflow failed: workflowId={}, error={}", workflowId, e.getMessage());
            // TODO: trigger saga compensation steps
        }
        workflowRepository.save(workflow);
    }

    private void executeLoanProcessingWorkflow(Workflow workflow, String tenantId, String callerRole) {
        WorkflowStep step = createStep(workflow, 1, "ENGINE_PROCESS");
        try {
            ProcessRequestDto engineReq = new ProcessRequestDto();
            engineReq.setIdempotencyKey(workflow.getWorkflowId() + "-engine");
            engineReq.setInputPayload(workflow.getInputPayload());

            ApiResponse<ProcessResponseDto> response =
                engineServiceClient.process(tenantId, callerRole, engineReq);

            if (!response.isSuccess() || response.getData() == null) {
                throw new BusinessException("Engine processing failed: " +
                    (response.getMessage() != null ? response.getMessage() : "unknown error"));
            }

            step.setStatus(StepStatus.COMPLETED);
            step.setOutputPayload(response.getData().getOutputPayload());
            workflow.setOutputPayload(response.getData().getOutputPayload());
        } catch (Exception e) {
            step.setStatus(StepStatus.FAILED);
            step.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            step.setExecutedAt(LocalDateTime.now());
            stepRepository.save(step);
        }
    }

    private void executeEtlPipelineWorkflow(Workflow workflow, String tenantId, String callerRole) {
        WorkflowStep step = createStep(workflow, 1, "ETL_SUBMIT");
        try {
            SubmitEtlJobDto etlReq = new SubmitEtlJobDto();
            etlReq.setSourceRef(workflow.getCorrelationId());
            etlReq.setBatchDate(LocalDate.now().toString());
            etlReq.setInputPayload(workflow.getInputPayload());

            ApiResponse<EtlJobDto> response =
                etlServiceClient.submitJob(tenantId, callerRole, etlReq);

            if (!response.isSuccess() || response.getData() == null) {
                throw new BusinessException("ETL job submission failed: " +
                    (response.getMessage() != null ? response.getMessage() : "unknown error"));
            }

            step.setStatus(StepStatus.COMPLETED);
            step.setOutputPayload("{\"jobId\":\"" + response.getData().getJobId() + "\"}");
            workflow.setOutputPayload(step.getOutputPayload());
        } catch (Exception e) {
            step.setStatus(StepStatus.FAILED);
            step.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            step.setExecutedAt(LocalDateTime.now());
            stepRepository.save(step);
        }
    }

    private WorkflowStep createStep(Workflow workflow, int order, String name) {
        WorkflowStep step = new WorkflowStep();
        step.setWorkflow(workflow);
        step.setStepOrder(order);
        step.setStepName(name);
        step.setStatus(StepStatus.RUNNING);
        step.setInputPayload(workflow.getInputPayload());
        return stepRepository.save(step);
    }

    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflow(String workflowId, String callerTenantId, String callerRole) {
        Workflow workflow = workflowRepository.findByWorkflowId(workflowId)
            .orElseThrow(() -> new ResourceNotFoundException("Workflow", workflowId));
        assertTenantAccess(workflow.getTenantId(), callerTenantId, callerRole);
        return WorkflowResponse.from(workflow);
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkflowResponse> listWorkflows(
            String tenantId, WorkflowStatus status, PageRequest pageRequest,
            String callerTenantId, String callerRole) {
        assertTenantAccess(tenantId, callerTenantId, callerRole);
        Page<Workflow> page = status != null
            ? workflowRepository.findByTenantIdAndStatusAndDeletedAtIsNull(
                tenantId, status, pageRequest.toSpringPageRequest())
            : workflowRepository.findByTenantIdAndDeletedAtIsNull(
                tenantId, pageRequest.toSpringPageRequest());
        return PageResponse.from(page.map(WorkflowResponse::from));
    }
}
