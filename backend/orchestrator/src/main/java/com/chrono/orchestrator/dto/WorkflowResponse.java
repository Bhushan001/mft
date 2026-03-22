package com.chrono.orchestrator.dto;

import com.chrono.orchestrator.domain.entity.Workflow;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class WorkflowResponse {

    private String workflowId;
    private String tenantId;
    private String workflowType;
    private String status;
    private String correlationId;
    private String outputPayload;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<WorkflowStepResponse> steps;

    public static WorkflowResponse from(Workflow workflow) {
        WorkflowResponse response = new WorkflowResponse();
        response.setWorkflowId(workflow.getWorkflowId());
        response.setTenantId(workflow.getTenantId());
        response.setWorkflowType(workflow.getWorkflowType());
        response.setStatus(workflow.getStatus() != null ? workflow.getStatus().name() : null);
        response.setCorrelationId(workflow.getCorrelationId());
        response.setOutputPayload(workflow.getOutputPayload());
        response.setErrorMessage(workflow.getErrorMessage());
        response.setStartedAt(workflow.getStartedAt());
        response.setCompletedAt(workflow.getCompletedAt());
        if (workflow.getSteps() != null) {
            response.setSteps(workflow.getSteps().stream()
                .map(WorkflowStepResponse::from)
                .collect(Collectors.toList()));
        }
        return response;
    }
}
