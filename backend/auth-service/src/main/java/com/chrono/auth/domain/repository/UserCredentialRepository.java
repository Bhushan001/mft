package com.chrono.auth.domain.repository;

import com.chrono.auth.domain.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {

    Optional<UserCredential> findByEmail(String email);

    Optional<UserCredential> findByUserId(String userId);

    boolean existsByEmail(String email);

    Optional<UserCredential> findByPasswordResetToken(String token);

    @Modifying
    @Query("UPDATE UserCredential uc SET uc.failedLoginAttempts = 0, uc.lockedUntil = null WHERE uc.userId = :userId")
    void resetFailedAttempts(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE UserCredential uc SET uc.lastLoginAt = :loginAt WHERE uc.userId = :userId")
    void updateLastLogin(@Param("userId") String userId, @Param("loginAt") LocalDateTime loginAt);
}
