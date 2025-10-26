package com.example.demo.workorder;

import com.example.demo.property.Property;
import com.example.demo.property.PropertyRepository;
import com.example.demo.user.User;
import com.example.demo.vendor.Vendor;
import com.example.demo.vendor.VendorRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize; // Import PreAuthorize
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final PropertyRepository propertyRepository;
    private final VendorRepository vendorRepository;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            PropertyRepository propertyRepository,
                            VendorRepository vendorRepository) {
        this.workOrderRepository = workOrderRepository;
        this.propertyRepository = propertyRepository;
        this.vendorRepository = vendorRepository;
    }

    // Helper method to get current user's tenant ID
    private Long getCurrentTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new IllegalStateException("User must be authenticated to perform this action.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getTenantId();
        } else {
            throw new IllegalStateException("Authentication principal is not the expected User type.");
        }
    }

    // No specific role needed for reading
    public List<WorkOrder> getAllWorkOrders() {
        Long tenantId = getCurrentTenantId();
        return workOrderRepository.findAllByTenantId(tenantId);
    }

    public Optional<WorkOrder> getWorkOrderById(Long id) {
        Long tenantId = getCurrentTenantId();
        return workOrderRepository.findByIdAndTenantId(id, tenantId);
    }

    // No specific role needed for creating
    @Transactional
    public WorkOrder createWorkOrder(WorkOrder workOrder, Long propertyId, Long optionalVendorId) {
        Long tenantId = getCurrentTenantId();

        Property property = propertyRepository.findByIdAndTenantId(propertyId, tenantId)
                .orElseThrow(() -> new AccessDeniedException("Property not found or access denied."));

        Vendor vendor = null;
        if (optionalVendorId != null) {
            vendor = vendorRepository.findByIdAndTenantId(optionalVendorId, tenantId)
                    .orElseThrow(() -> new AccessDeniedException("Vendor not found or access denied."));
        }

        workOrder.setProperty(property);
        workOrder.setVendor(vendor);
        workOrder.setTenantId(tenantId);
        workOrder.setStatus("PENDING");
        workOrder.setCreatedAt(LocalDateTime.now());
        workOrder.setUpdatedAt(LocalDateTime.now());

        return workOrderRepository.save(workOrder);
    }

    // No specific role needed for updating generally
    @Transactional
    public Optional<WorkOrder> updateWorkOrder(Long id, WorkOrder workOrderDetails, Long newPropertyId, Long newOptionalVendorId) {
        Long tenantId = getCurrentTenantId();

        return workOrderRepository.findByIdAndTenantId(id, tenantId)
                .map(existingWorkOrder -> {
                    Property newProperty = propertyRepository.findByIdAndTenantId(newPropertyId, tenantId)
                            .orElseThrow(() -> new AccessDeniedException("New Property not found or access denied."));

                    Vendor newVendor = null;
                    if (newOptionalVendorId != null) {
                        newVendor = vendorRepository.findByIdAndTenantId(newOptionalVendorId, tenantId)
                                .orElseThrow(() -> new AccessDeniedException("New Vendor not found or access denied."));
                    }

                    existingWorkOrder.setDescription(workOrderDetails.getDescription());
                    existingWorkOrder.setStatus(workOrderDetails.getStatus());
                    existingWorkOrder.setProperty(newProperty);
                    existingWorkOrder.setVendor(newVendor);

                    return workOrderRepository.save(existingWorkOrder);
                });
    }

    // No specific role needed for updating status (could be refined later)
    @Transactional
    public Optional<WorkOrder> updateWorkOrderStatus(Long id, String newStatus) {
        Long tenantId = getCurrentTenantId();
        return workOrderRepository.findByIdAndTenantId(id, tenantId)
                .map(existingWorkOrder -> {
                    existingWorkOrder.setStatus(newStatus);
                    return workOrderRepository.save(existingWorkOrder);
                });
    }

    // --- SECURE THE DELETE METHOD ---
    @Transactional
    // --- FIX: Change hasRole('ADMIN') to hasAuthority(...) ---
    @PreAuthorize("hasAuthority('PERMISSION_DELETE_WORK_ORDER')") // Check for the specific permission
    public boolean deleteWorkOrder(Long id) {
        // --- END FIX ---
        Long tenantId = getCurrentTenantId();
        if (workOrderRepository.existsByIdAndTenantId(id, tenantId)) {
            workOrderRepository.deleteById(id);
            return true;
        } else {
            return false;
        }
    }
}

