package com.nivedha.pathigai.auth.repositories;

import com.nivedha.pathigai.auth.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u WHERE u.userId = :userId AND u.enabled = false")
    Optional<User> findPendingUserById(@Param("userId") Integer userId);

    // Fetch user with role and profile relationships
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.primaryRole LEFT JOIN FETCH u.primaryProfile WHERE u.userId = :userId")
    Optional<User> findByIdWithRoleAndProfile(@Param("userId") Integer userId);

    // Find users by company with search functionality
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.primaryRole LEFT JOIN FETCH u.primaryProfile " +
           "WHERE u.company.companyId = :companyId AND u.userStatus != :status " +
           "AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> findByCompanyIdAndSearchQuery(@Param("companyId") Integer companyId,
                                            @Param("query") String query,
                                            @Param("status") User.UserStatus status,
                                            Pageable pageable);

    // Find users by company excluding deleted
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.primaryRole LEFT JOIN FETCH u.primaryProfile " +
           "WHERE u.company.companyId = :companyId AND u.userStatus != :status")
    Page<User> findByCompanyIdAndUserStatusNot(@Param("companyId") Integer companyId,
                                              @Param("status") User.UserStatus status,
                                              Pageable pageable);

    // Deactivate all sessions for a user (will be used by session management)
    @Modifying
    @Query("UPDATE Session s SET s.isActive = false WHERE s.user.userId = :userId")
    void deactivateAllUserSessions(@Param("userId") Integer userId);
}