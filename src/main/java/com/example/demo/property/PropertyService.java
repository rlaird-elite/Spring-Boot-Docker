package com.example.demo.property;

import com.example.demo.user.User; // Import User
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
    // No longer needs UserRepository directly, tenantId comes from security context

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    // --- Helper method to get current user's tenant ID ---
    private Long getCurrentTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            // Handle cases where there is no authenticated user
            // Depending on your security setup, this might throw an exception earlier
            // Or you might want to throw a specific exception here
            throw new IllegalStateException("User must be authenticated to perform this action.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User) { // Assuming your UserDetails implementation is your User entity
            return ((User) principal).getTenantId();
        } else if (principal instanceof UserDetails) {
            // If the principal is a standard UserDetails, you might need another way
            // to fetch the tenantId, perhaps by loading the full User entity
            // based on the username ((UserDetails) principal).getUsername()
            // For now, let's assume it's our User entity.
            throw new IllegalStateException("Unexpected principal type found in security context.");
        } else {
            throw new IllegalStateException("Authentication principal is not an instance of UserDetails.");
        }
    }


    public List<Property> getAllProperties() {
        Long tenantId = getCurrentTenantId();
        return propertyRepository.findAllByTenantId(tenantId);
    }

    public Optional<Property> getPropertyById(Long id) {
        Long tenantId = getCurrentTenantId();
        return propertyRepository.findByIdAndTenantId(id, tenantId);
    }

    @Transactional // Good practice for create/update/delete
    public Property createProperty(Property property) {
        Long tenantId = getCurrentTenantId();
        property.setTenantId(tenantId); // Ensure the property belongs to the current tenant
        return propertyRepository.save(property);
    }

    @Transactional
    public Optional<Property> updateProperty(Long id, Property propertyDetails) {
        Long tenantId = getCurrentTenantId();
        // Find the existing property *belonging to the current tenant*
        return propertyRepository.findByIdAndTenantId(id, tenantId)
                .map(existingProperty -> {
                    existingProperty.setAddress(propertyDetails.getAddress());
                    existingProperty.setType(propertyDetails.getType());
                    existingProperty.setBedrooms(propertyDetails.getBedrooms());
                    existingProperty.setBathrooms(propertyDetails.getBathrooms());
                    // tenantId does not change during update
                    return propertyRepository.save(existingProperty);
                });
    }

    @Transactional
    public boolean deleteProperty(Long id) {
        Long tenantId = getCurrentTenantId();
        // Check if the property exists *and* belongs to the current tenant before deleting
        if (propertyRepository.existsByIdAndTenantId(id, tenantId)) {
            propertyRepository.deleteById(id); // Standard deleteById is fine after existence check
            return true;
        } else {
            return false;
        }
    }
}

