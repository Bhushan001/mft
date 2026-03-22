package com.chrono.orchestrator.dto;

import com.chrono.orchestrator.domain.entity.WorkflowStep;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WorkflowStepResponse {

    private int stepOrder;
    private String stepName;
    private String status;
    private String outputPayload;
    private String errorMessage;
    private LocalDateTime executedAt;

    public static WorkflowStepResponse from(WorkflowStep step) {
        WorkflowStepResponse response = new WorkflowStepResponse();
        response.setStepOrder(step.getStepOrder());
        response.setStepName(step.getStepName());
        response.setStatus(step.getStatus() != null ? step.getStatus().name() : null);
        response.setOutputPayload(step.getOutputPayload());
        response.setErrorMessage(step.getErrorMessage());
        response.setExecutedAt(step.getExecutedAt());
        return response;
    }
}
