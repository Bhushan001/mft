package com.chrono.engine.domain.repository;

import com.chrono.engine.domain.entity.ProcessingRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessingRequestRepository extends JpaRepository<ProcessingRequest, Long> {

    Optional<ProcessingRequest> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);

    Optional<ProcessingRequest> findByRequestId(String requestId);

    Page<ProcessingRequest> findByTenantIdAndDeletedAtIsNull(String tenantId, Pageable pageable);
}
