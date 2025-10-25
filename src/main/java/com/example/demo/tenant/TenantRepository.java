package com.example.demo.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    // We might need custom queries later, like findByName
    Optional<Tenant> findByName(String name);

    // Method to check if any tenants exist
    boolean existsByNameIsNotNull(); // Or simply count() > 0
}
