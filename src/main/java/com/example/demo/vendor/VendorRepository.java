package com.example.demo.vendor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    // --- NEW: Tenant-aware find methods ---

    /**
     * Finds all vendors belonging to a specific tenant.
     * Replaces the generic findAll().
     */
    List<Vendor> findAllByTenantId(Long tenantId);

    /**
     * Finds a specific vendor by its ID, but only if it belongs to the specified tenant.
     * Replaces the generic findById().
     */
    Optional<Vendor> findByIdAndTenantId(Long id, Long tenantId);

    /**
     * Checks if a vendor exists by its ID and belongs to the specified tenant.
     * Useful for delete/update operations.
     */
    boolean existsByIdAndTenantId(Long id, Long tenantId);

    // --- Generic save() and delete() methods from JpaRepository are retained ---
}

