package com.nivedha.pathigai.auth.repositories;

import com.nivedha.pathigai.auth.entities.Verification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationRepository extends JpaRepository<Verification, Integer> {

    @Query("SELECT v FROM Verification v WHERE v.user.userId = :userId " +
            "AND v.verificationType = :verificationType " +
            "AND v.context = :context " +
            "AND v.verified = false " +
            "AND v.expiresAt > :currentTime " +
            "ORDER BY v.createdAt DESC " +
            "LIMIT 1")
    Optional<Verification> findActiveVerification(
            @Param("userId") Integer userId,
            @Param("verificationType") Verification.VerificationType verificationType,
            @Param("context") Verification.VerificationContext context,
            @Param("currentTime") LocalDateTime currentTime
    );

    @Query("SELECT v FROM Verification v WHERE v.user.userId = :userId " +
            "AND v.context = :context " +
            "AND v.verified = false " +
            "AND v.expiresAt > :currentTime " +
            "ORDER BY v.createdAt DESC")
    List<Verification> findAllActiveVerifications(
            @Param("userId") Integer userId,
            @Param("context") Verification.VerificationContext context,
            @Param("currentTime") LocalDateTime currentTime
    );

    @Query("SELECT v FROM Verification v WHERE v.user.userId = :userId " +
            "AND v.verificationType = :verificationType " +
            "AND v.context = :context " +
            "AND v.verified = true " +
            "ORDER BY v.createdAt DESC " +
            "LIMIT 1")
    Optional<Verification> findCompletedVerification(
            @Param("userId") Integer userId,
            @Param("verificationType") Verification.VerificationType verificationType,
            @Param("context") Verification.VerificationContext context
    );

    // Add method to properly invalidate existing verifications
    @Query("SELECT v FROM Verification v WHERE v.user.userId = :userId " +
            "AND v.verificationType = :verificationType " +
            "AND v.context = :context " +
            "AND v.verified = false " +
            "AND v.expiresAt > :currentTime")
    List<Verification> findExistingActiveVerifications(
            @Param("userId") Integer userId,
            @Param("verificationType") Verification.VerificationType verificationType,
            @Param("context") Verification.VerificationContext context,
            @Param("currentTime") LocalDateTime currentTime
    );
}