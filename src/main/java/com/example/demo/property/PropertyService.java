package com.example.demo.property;

import com.example.demo.user.User; // Import User
import org.springframework.security.access.prepost.PreAuthorize; // Import PreAuthorize
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.util.List;
import java.util.Optional;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
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
            throw new IllegalStateException("Authentication principal is not the expected User type.");
        }
    }

    // No specific auth needed beyond being logged in for tenant
    public List<Property> getAllProperties() {
        Long tenantId = getCurrentTenantId();
        return propertyRepository.findAllByTenantId(tenantId);
    }

    public Optional<Property> getPropertyById(Long id) {
        Long tenantId = getCurrentTenantId();
        return propertyRepository.findByIdAndTenantId(id, tenantId);
    }

    // No specific auth needed beyond being logged in for tenant
    @Transactional
    public Property createProperty(Property property) {
        Long tenantId = getCurrentTenantId();
        property.setTenantId(tenantId);
        return propertyRepository.save(property);
    }

    // No specific auth needed beyond being logged in for tenant
    @Transactional
    public Optional<Property> updateProperty(Long id, Property propertyDetails) {
        Long tenantId = getCurrentTenantId();
        return propertyRepository.findByIdAndTenantId(id, tenantId)
                .map(existingProperty -> {
                    existingProperty.setAddress(propertyDetails.getAddress());
                    existingProperty.setType(propertyDetails.getType());
                    existingProperty.setBedrooms(propertyDetails.getBedrooms());
                    existingProperty.setBathrooms(propertyDetails.getBathrooms());
                    return propertyRepository.save(existingProperty);
                });
    }

    // --- SECURE THE DELETE METHOD ---
    @Transactional
    // --- FIX: Add PreAuthorize annotation ---
    @PreAuthorize("hasAuthority('PERMISSION_DELETE_PROPERTY')") // Use hasAuthority
    public boolean deleteProperty(Long id) {
        // --- END FIX ---
        Long tenantId = getCurrentTenantId();
        if (propertyRepository.existsByIdAndTenantId(id, tenantId)) {
            propertyRepository.deleteById(id);
            return true;
        } else {
            return false;
        }
    }
}

