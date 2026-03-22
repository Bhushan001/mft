package com.chrono.user.domain.repository;

import com.chrono.user.domain.entity.UserProfile;
import com.chrono.user.domain.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(String userId);

    Optional<UserProfile> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUserId(String userId);

    Page<UserProfile> findByTenantIdAndDeletedAtIsNull(String tenantId, Pageable pageable);

    @Query("SELECT u FROM UserProfile u " +
           "WHERE u.tenantId = :tenantId " +
           "AND u.deletedAt IS NULL " +
           "AND (:search IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR u.status = :status)")
    Page<UserProfile> searchByTenant(
            @Param("tenantId") String tenantId,
            @Param("search") String search,
            @Param("status") UserStatus status,
            Pageable pageable);
}
