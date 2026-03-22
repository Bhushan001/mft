package com.chrono.etl.domain.repository;

import com.chrono.etl.domain.entity.EtlJob;
import com.chrono.etl.domain.entity.EtlJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EtlJobRepository extends JpaRepository<EtlJob, Long> {

    Optional<EtlJob> findByJobId(String jobId);

    Optional<EtlJob> findByTenantIdAndSourceRefAndBatchDate(
        String tenantId, String sourceRef, String batchDate);

    Page<EtlJob> findByTenantIdAndDeletedAtIsNull(String tenantId, Pageable pageable);

    Page<EtlJob> findByTenantIdAndStatusAndDeletedAtIsNull(
        String tenantId, EtlJobStatus status, Pageable pageable);
}
