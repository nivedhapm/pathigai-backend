package com.nivedha.pathigai.auth.repositories;

import com.nivedha.pathigai.auth.entities.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Integer> {

    Optional<Session> findByJwtTokenAndIsActiveTrue(String jwtToken);

    @Query("SELECT s FROM Session s WHERE s.user.userId = :userId AND s.isActive = true AND s.expiresAt > :currentTime")
    List<Session> findActiveSessionsByUserId(@Param("userId") Integer userId, @Param("currentTime") LocalDateTime currentTime);

    @Query("UPDATE Session s SET s.isActive = false WHERE s.user.userId = :userId")
    void deactivateAllUserSessions(@Param("userId") Integer userId);

    @Query("UPDATE Session s SET s.isActive = false WHERE s.jwtToken = :jwtToken")
    void deactivateSessionByToken(@Param("jwtToken") String jwtToken);

    @Query("DELETE FROM Session s WHERE s.expiresAt < :currentTime OR s.isActive = false")
    void cleanupExpiredSessions(@Param("currentTime") LocalDateTime currentTime);
}