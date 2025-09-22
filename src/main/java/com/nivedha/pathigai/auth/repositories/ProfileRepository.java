package com.nivedha.pathigai.auth.repositories;

import com.nivedha.pathigai.auth.entities.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Integer> {
    Optional<Profile> findByName(String name);
    boolean existsByName(String name);

    @Query("SELECT p FROM Profile p ORDER BY p.hierarchyLevel ASC")
    List<Profile> findAllOrderedByHierarchy();

    @Query("SELECT p FROM Profile p WHERE p.hierarchyLevel >= :minLevel ORDER BY p.hierarchyLevel ASC")
    List<Profile> findByHierarchyLevelGreaterThanEqual(Integer minLevel);
}
