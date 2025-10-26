package com.example.demo.vendor;

import com.example.demo.user.User;
import org.springframework.security.access.prepost.PreAuthorize; // Import PreAuthorize
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
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

    // No specific role needed for reading data (within the tenant)
    public List<Vendor> getAllVendors() {
        Long tenantId = getCurrentTenantId();
        return vendorRepository.findAllByTenantId(tenantId);
    }

    public Optional<Vendor> getVendorById(Long id) {
        Long tenantId = getCurrentTenantId();
        return vendorRepository.findByIdAndTenantId(id, tenantId);
    }

    // No specific role needed for creating (within the tenant)
    @Transactional
    public Vendor createVendor(Vendor vendor) {
        Long tenantId = getCurrentTenantId();
        vendor.setTenantId(tenantId);
        return vendorRepository.save(vendor);
    }

    // No specific role needed for updating (within the tenant)
    @Transactional
    public Optional<Vendor> updateVendor(Long id, Vendor vendorDetails) {
        Long tenantId = getCurrentTenantId();
        return vendorRepository.findByIdAndTenantId(id, tenantId)
                .map(existingVendor -> {
                    existingVendor.setName(vendorDetails.getName());
                    // --- FIX: Corrected typo from getSpecialialty() to getSpecialty() ---
                    existingVendor.setSpecialty(vendorDetails.getSpecialty());
                    // --- END FIX ---
                    existingVendor.setPhone(vendorDetails.getPhone());
                    return vendorRepository.save(existingVendor);
                });
    }

    // --- SECURE THE DELETE METHOD ---
    @Transactional
    // --- FIX: Change hasRole('ADMIN') to hasAuthority('PERMISSION_DELETE_VENDOR') ---
    @PreAuthorize("hasAuthority('PERMISSION_DELETE_VENDOR')")
    public boolean deleteVendor(Long id) {
        // --- END FIX ---
        Long tenantId = getCurrentTenantId();
        if (vendorRepository.existsByIdAndTenantId(id, tenantId)) {
            vendorRepository.deleteById(id);
            return true;
        } else {
            return false;
        }
    }
}

