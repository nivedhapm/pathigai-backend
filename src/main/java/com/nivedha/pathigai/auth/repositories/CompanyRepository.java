package com.nivedha.pathigai.auth.repositories;

import com.nivedha.pathigai.auth.entities.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {

    Optional<Company> findByCompanyNameIgnoreCase(String companyName);

    boolean existsByCompanyNameIgnoreCase(String companyName);
}