package com.example.demo.workorder;

import com.example.demo.property.Property; // Import Property
import com.example.demo.property.PropertyRepository; // Import PropertyRepository
import com.example.demo.user.User; // Import User
import com.example.demo.vendor.Vendor; // Import Vendor
import com.example.demo.vendor.VendorRepository; // Import VendorRepository
import org.springframework.security.access.AccessDeniedException; // Import AccessDeniedException
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime; // Import LocalDateTime
import java.util.List;
import java.util.Optional;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final PropertyRepository propertyRepository; // Inject PropertyRepository
    private final VendorRepository vendorRepository;     // Inject VendorRepository

    // Updated constructor
    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            PropertyRepository propertyRepository,
                            VendorRepository vendorRepository) {
        this.workOrderRepository = workOrderRepository;
        this.propertyRepository = propertyRepository;
        this.vendorRepository = vendorRepository;
    }

    // --- Helper method to get current user's tenant ID ---
    private Long getCurrentTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new IllegalStateException("User must be authenticated to perform this action.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getTenantId();
        } else {
            // Handle cases where principal might be UserDetails but not your User entity
            // This might involve loading the User entity based on username
            throw new IllegalStateException("Authentication principal is not the expected User type.");
        }
    }

    // --- Tenant-aware methods ---

    public List<WorkOrder> getAllWorkOrders() {
        Long tenantId = getCurrentTenantId();
        return workOrderRepository.findAllByTenantId(tenantId);
    }

    public Optional<WorkOrder> getWorkOrderById(Long id) {
        Long tenantId = getCurrentTenantId();
        return workOrderRepository.findByIdAndTenantId(id, tenantId);
    }

    @Transactional
    public WorkOrder createWorkOrder(WorkOrder workOrder, Long propertyId, Long optionalVendorId) {
        Long tenantId = getCurrentTenantId();

        // 1. Verify Property belongs to the tenant
        Property property = propertyRepository.findByIdAndTenantId(propertyId, tenantId)
                .orElseThrow(() -> new AccessDeniedException("Property not found or access denied."));

        // 2. Verify Vendor (if provided) belongs to the tenant
        Vendor vendor = null;
        if (optionalVendorId != null) {
            vendor = vendorRepository.findByIdAndTenantId(optionalVendorId, tenantId)
                    .orElseThrow(() -> new AccessDeniedException("Vendor not found or access denied."));
        }

        // 3. Set associations and tenant ID on the new WorkOrder
        workOrder.setProperty(property);
        workOrder.setVendor(vendor); // Can be null if optionalVendorId was null
        workOrder.setTenantId(tenantId);
        workOrder.setStatus("PENDING"); // Ensure default status
        workOrder.setCreatedAt(LocalDateTime.now()); // Set timestamps
        workOrder.setUpdatedAt(LocalDateTime.now());

        return workOrderRepository.save(workOrder);
    }

    @Transactional
    public Optional<WorkOrder> updateWorkOrder(Long id, WorkOrder workOrderDetails, Long newPropertyId, Long newOptionalVendorId) {
        Long tenantId = getCurrentTenantId();

        // Find existing WorkOrder belonging to the tenant
        return workOrderRepository.findByIdAndTenantId(id, tenantId)
                .map(existingWorkOrder -> {
                    // Verify new Property belongs to the tenant
                    Property newProperty = propertyRepository.findByIdAndTenantId(newPropertyId, tenantId)
                            .orElseThrow(() -> new AccessDeniedException("New Property not found or access denied."));

                    // Verify new Vendor (if provided) belongs to the tenant
                    Vendor newVendor = null;
                    if (newOptionalVendorId != null) {
                        newVendor = vendorRepository.findByIdAndTenantId(newOptionalVendorId, tenantId)
                                .orElseThrow(() -> new AccessDeniedException("New Vendor not found or access denied."));
                    }

                    // Update fields
                    existingWorkOrder.setDescription(workOrderDetails.getDescription());
                    existingWorkOrder.setStatus(workOrderDetails.getStatus()); // Allow status update
                    existingWorkOrder.setProperty(newProperty);
                    existingWorkOrder.setVendor(newVendor);
                    // tenantId does not change
                    // createdAt does not change
                    // updatedAt will be handled by @PreUpdate

                    return workOrderRepository.save(existingWorkOrder);
                });
    }

    // Method specifically for updating status (might be simpler for frontend)
    @Transactional
    public Optional<WorkOrder> updateWorkOrderStatus(Long id, String newStatus) {
        Long tenantId = getCurrentTenantId();
        return workOrderRepository.findByIdAndTenantId(id, tenantId)
                .map(existingWorkOrder -> {
                    // Add validation for allowed status transitions if needed
                    existingWorkOrder.setStatus(newStatus);
                    return workOrderRepository.save(existingWorkOrder);
                });
    }


    @Transactional
    public boolean deleteWorkOrder(Long id) {
        Long tenantId = getCurrentTenantId();
        // Check if the work order exists *and* belongs to the current tenant
        if (workOrderRepository.existsByIdAndTenantId(id, tenantId)) {
            workOrderRepository.deleteById(id);
            return true;
        } else {
            return false;
        }
    }
}

