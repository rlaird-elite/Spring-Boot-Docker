package com.example.demo.workorder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    // --- NEW: Tenant-aware find methods ---

    /**
     * Finds all work orders belonging to a specific tenant.
     * Replaces the generic findAll().
     */
    List<WorkOrder> findAllByTenantId(Long tenantId);

    /**
     * Finds a specific work order by its ID, but only if it belongs to the specified tenant.
     * Replaces the generic findById().
     */
    Optional<WorkOrder> findByIdAndTenantId(Long id, Long tenantId);

    /**
     * Checks if a work order exists by its ID and belongs to the specified tenant.
     * Useful for delete/update operations.
     */
    boolean existsByIdAndTenantId(Long id, Long tenantId);

    // --- Generic save() and delete() methods from JpaRepository are retained ---
    // Service layer will handle setting/checking tenantId before calling these.
}

