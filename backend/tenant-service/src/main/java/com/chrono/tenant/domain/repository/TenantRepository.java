package com.chrono.tenant.domain.repository;

import com.chrono.tenant.domain.entity.Tenant;
import com.chrono.tenant.domain.entity.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByTenantId(String tenantId);

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByContactEmail(String contactEmail);

    @Query("SELECT t FROM Tenant t " +
           "WHERE t.deletedAt IS NULL " +
           "AND (:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(t.slug) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR t.status = :status)")
    Page<Tenant> searchTenants(
            @Param("search") String search,
            @Param("status") TenantStatus status,
            Pageable pageable);
}
