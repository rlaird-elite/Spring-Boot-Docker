package com.example.demo.property;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
// JpaRepository<EntityType, PrimaryKeyType>
public interface PropertyRepository extends JpaRepository<Property, Long> {

    // --- NEW: Tenant-aware find methods ---

    /**
     * Finds all properties belonging to a specific tenant.
     * Replaces the generic findAll().
     * Spring Data JPA automatically creates the query based on the method name.
     */
    List<Property> findAllByTenantId(Long tenantId);

    /**
     * Finds a specific property by its ID, but only if it belongs to the specified tenant.
     * Replaces the generic findById().
     */
    Optional<Property> findByIdAndTenantId(Long id, Long tenantId);

    /**
     * Checks if a property exists by its ID and belongs to the specified tenant.
     * Useful for delete/update operations to ensure the user owns the record.
     */
    boolean existsByIdAndTenantId(Long id, Long tenantId);

    // --- The generic save() and delete() methods from JpaRepository are usually okay,
    // --- as the service layer will ensure the entity has the correct tenantId before saving
    // --- or verify ownership before deleting.
}

