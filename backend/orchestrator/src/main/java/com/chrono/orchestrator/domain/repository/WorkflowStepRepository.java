package com.chrono.orchestrator.domain.repository;

import com.chrono.orchestrator.domain.entity.Workflow;
import com.chrono.orchestrator.domain.entity.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {

    List<WorkflowStep> findByWorkflowOrderByStepOrderAsc(Workflow workflow);
}
