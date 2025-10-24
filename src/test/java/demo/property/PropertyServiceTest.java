package com.example.demo.property;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for PropertyService.
 * This test mocks the PropertyRepository to ensure the service logic is correct.
 */
public class PropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository; // Mock the repository

    @InjectMocks
    private PropertyService propertyService; // Test the service

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllProperties() {
        // Arrange
        Property prop1 = new Property();
        prop1.setAddress("111 First St");
        when(propertyRepository.findAll()).thenReturn(List.of(prop1));

        // Act
        List<Property> properties = propertyService.getAllProperties();

        // Assert
        assertNotNull(properties);
        assertEquals(1, properties.size());
        assertEquals("111 First St", properties.get(0).getAddress());
        verify(propertyRepository).findAll(); // Verify the repo was called
    }

    @Test
    void testGetPropertyById_whenExists() {
        // Arrange
        Property property = new Property();
        property.setId(1L);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        // Act
        Optional<Property> found = propertyService.getPropertyById(1L);

        // Assert
        assertTrue(found.isPresent());
        assertEquals(1L, found.get().getId());
        verify(propertyRepository).findById(1L);
    }

    @Test
    void testGetPropertyById_whenNotExists() {
        // Arrange
        when(propertyRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        Optional<Property> found = propertyService.getPropertyById(99L);

        // Assert
        assertFalse(found.isPresent());
        verify(propertyRepository).findById(99L);
    }

    @Test
    void testCreateProperty() {
        // Arrange
        Property propertyToSave = new Property();
        propertyToSave.setAddress("123 Main St");

        when(propertyRepository.save(any(Property.class))).thenReturn(propertyToSave);

        // Act
        Property savedProperty = propertyService.createProperty(propertyToSave);

        // Assert
        assertNotNull(savedProperty);
        assertEquals("123 Main St", savedProperty.getAddress());
        verify(propertyRepository).save(propertyToSave);
    }

    @Test
    void testUpdateProperty_whenExists() {
        // Arrange
        Property existingProperty = new Property();
        existingProperty.setId(1L);
        existingProperty.setAddress("Old Address");

        Property propertyDetails = new Property();
        propertyDetails.setAddress("New Address");

        when(propertyRepository.findById(1L)).thenReturn(Optional.of(existingProperty));
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Return the saved entity

        // Act
        Optional<Property> updated = propertyService.updateProperty(1L, propertyDetails);

        // Assert
        assertTrue(updated.isPresent());
        assertEquals("New Address", updated.get().getAddress()); // Check that the address was updated
        verify(propertyRepository).findById(1L);
        verify(propertyRepository).save(existingProperty); // Verify save was called on the *existing* entity
    }

    @Test
    void testUpdateProperty_whenNotExists() {
        // Arrange
        Property propertyDetails = new Property();
        when(propertyRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        Optional<Property> updated = propertyService.updateProperty(99L, propertyDetails);

        // Assert
        assertFalse(updated.isPresent());
        verify(propertyRepository).findById(99L);
    }

    @Test
    void testDeleteProperty_whenExists() {
        // Arrange
        Property existingProperty = new Property();
        existingProperty.setId(1L);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(existingProperty));

        // Act
        boolean wasDeleted = propertyService.deleteProperty(1L);

        // Assert
        assertTrue(wasDeleted);
        verify(propertyRepository).findById(1L);
        verify(propertyRepository).delete(existingProperty); // Verify the delete call
    }

    @Test
    void testDeleteProperty_whenNotExists() {
        // Arrange
        when(propertyRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        boolean wasDeleted = propertyService.deleteProperty(99L);

        // Assert
        assertFalse(wasDeleted);
        verify(propertyRepository).findById(99L);
    }
}
