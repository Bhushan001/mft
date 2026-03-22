package com.chrono.mapper.domain.repository;

import com.chrono.mapper.domain.entity.MappingRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MappingRuleRepository extends JpaRepository<MappingRule, Long> {

    Optional<MappingRule> findByTenantIdAndActiveTrue(String tenantId);

    Optional<MappingRule> findByRuleId(String ruleId);

    boolean existsByRuleId(String ruleId);

    @Query("SELECT m FROM MappingRule m WHERE m.tenantId = :tenantId AND m.deletedAt IS NULL " +
           "AND (:name IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<MappingRule> searchByTenant(
        @Param("tenantId") String tenantId,
        @Param("name") String name,
        Pageable pageable
    );
}
