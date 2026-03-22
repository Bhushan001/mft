package com.chrono.audit.domain.repository;

import com.chrono.audit.domain.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query("SELECT e FROM AuditEvent e " +
           "WHERE (:tenantId IS NULL OR e.tenantId = :tenantId) " +
           "AND (:action IS NULL OR UPPER(e.action) = UPPER(:action)) " +
           "AND (:resourceType IS NULL OR UPPER(e.resourceType) = UPPER(:resourceType)) " +
           "AND (:performedBy IS NULL OR e.performedBy = :performedBy) " +
           "ORDER BY e.createdAt DESC")
    Page<AuditEvent> search(
            @Param("tenantId")     String tenantId,
            @Param("action")       String action,
            @Param("resourceType") String resourceType,
            @Param("performedBy")  String performedBy,
            Pageable pageable);
}
