package com.nivedha.pathigai.auth.repositories;

import com.nivedha.pathigai.auth.entities.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Integer> {
    Optional<Profile> findByName(String name);

    // Add methods needed by ProfileService
    List<Profile> findAllByOrderByHierarchyLevelAsc();

    List<Profile> findByHierarchyLevelLessThanEqual(Integer hierarchyLevel);

    List<Profile> findByHierarchyLevelGreaterThanEqualOrderByHierarchyLevel(Integer hierarchyLevel);
}