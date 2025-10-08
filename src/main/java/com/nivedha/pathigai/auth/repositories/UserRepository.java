package com.nivedha.pathigai.auth.repositories;

import com.nivedha.pathigai.auth.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndEnabledTrue(String email);

    // Add missing methods for compilation
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByEmailAndUserStatus(String email, User.UserStatus userStatus);

    List<User> findByCompanyCompanyId(Integer companyId);

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.primaryRole.name = :role) AND " +
           "(:profile IS NULL OR u.primaryProfile.name = :profile) AND " +
           "(:companyId IS NULL OR u.company.companyId = :companyId) AND " +
           "u.userStatus != 'DELETED'")
    List<User> findUsersWithFilters(@Param("role") String role,
                                   @Param("profile") String profile,
                                   @Param("companyId") Integer companyId);

    List<User> findByPrimaryRoleNameAndCompanyCompanyIdAndEnabledTrue(String roleName, Integer companyId);

    List<User> findByPrimaryProfileNameAndCompanyCompanyIdAndEnabledTrue(String profileName, Integer companyId);

    @Query("SELECT u FROM User u WHERE u.primaryProfile.hierarchyLevel <= :hierarchyLevel AND u.company.companyId = :companyId AND u.enabled = true")
    List<User> findByProfileHierarchyLevelLessThanEqual(@Param("hierarchyLevel") Integer hierarchyLevel,
                                                       @Param("companyId") Integer companyId);

    Optional<User> findByPhone(String phone);

    // Get users by company and status
    List<User> findByCompanyCompanyIdAndUserStatus(Integer companyId, User.UserStatus userStatus);

    // Get user with profile and role for authentication
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.primaryProfile LEFT JOIN FETCH u.primaryRole WHERE u.email = :email AND u.userStatus = :status")
    Optional<User> findByEmailAndUserStatusWithProfileAndRole(@Param("email") String email, @Param("status") User.UserStatus userStatus);

    List<User> findByFullNameIn(List<String> fullNames);
}
