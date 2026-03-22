package com.chrono.orchestrator.domain.repository;

import com.chrono.orchestrator.domain.entity.Workflow;
import com.chrono.orchestrator.domain.entity.WorkflowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

    Optional<Workflow> findByWorkflowId(String workflowId);

    Page<Workflow> findByTenantIdAndDeletedAtIsNull(String tenantId, Pageable pageable);

    Page<Workflow> findByTenantIdAndStatusAndDeletedAtIsNull(
        String tenantId, WorkflowStatus status, Pageable pageable);
}
