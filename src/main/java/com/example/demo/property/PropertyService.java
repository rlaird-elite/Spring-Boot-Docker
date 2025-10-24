package com.example.demo.property;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for managing Property business logic.
 * This class abstracts the repository from the controller.
 */
@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;

    // Inject the repository
    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    /**
     * Retrieves all properties.
     * @return A list of all properties.
     */
    public List<Property> getAllProperties() {
        return propertyRepository.findAll();
    }

    /**
     * Retrieves a single property by its ID.
     * @param id The ID of the property.
     * @return An Optional containing the property if found, or empty if not.
     */
    public Optional<Property> getPropertyById(Long id) {
        return propertyRepository.findById(id);
    }

    /**
     * Creates and saves a new property.
     * @param property The property data to save.
     * @return The saved property.
     */
    public Property createProperty(Property property) {
        return propertyRepository.save(property);
    }

    /**
     * Updates an existing property.
     * @param id The ID of the property to update.
     * @param propertyDetails The new details for the property.
     * @return An Optional containing the updated property if it was found, or empty if not.
     */
    public Optional<Property> updateProperty(Long id, Property propertyDetails) {
        Optional<Property> optionalProperty = propertyRepository.findById(id);
        if (optionalProperty.isPresent()) {
            Property existingProperty = optionalProperty.get();
            existingProperty.setAddress(propertyDetails.getAddress());
            existingProperty.setType(propertyDetails.getType());
            existingProperty.setBedrooms(propertyDetails.getBedrooms());
            existingProperty.setBathrooms(propertyDetails.getBathrooms());

            Property updatedProperty = propertyRepository.save(existingProperty);
            return Optional.of(updatedProperty);
        } else {
            return Optional.empty(); // Not found, so can't update
        }
    }

    /**
     * Deletes a property by its ID.
     * @param id The ID of the property to delete.
     * @return true if the property was found and deleted, false otherwise.
     */
    public boolean deleteProperty(Long id) {
        Optional<Property> propertyOptional = propertyRepository.findById(id);
        if (propertyOptional.isPresent()) {
            propertyRepository.delete(propertyOptional.get());
            return true; // Found and deleted
        } else {
            return false; // Not found
        }
    }
}
