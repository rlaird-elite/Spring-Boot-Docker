package com.example.demo.vendor;

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
 * Test class for VendorService.
 * This test mocks the VendorRepository to ensure the service logic is correct.
 */
public class VendorServiceTest {

    @Mock
    private VendorRepository vendorRepository; // Mock the repository

    @InjectMocks
    private VendorService vendorService; // Test the service

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllProperties() {
        // Arrange
        Vendor prop1 = new Vendor();
        prop1.setName("Vendor Name");
        when(vendorRepository.findAll()).thenReturn(List.of(prop1));

        // Act
        List<Vendor> properties = vendorService.getAllVendors();

        // Assert
        assertNotNull(properties);
        assertEquals(1, properties.size());
        assertEquals("Vendor Name", properties.get(0).getName());
        verify(vendorRepository).findAll(); // Verify the repo was called
    }

    @Test
    void testGetVendorById_whenExists() {
        // Arrange
        Vendor vendor = new Vendor();
        vendor.setId(1L);
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));

        // Act
        Optional<Vendor> found = vendorService.getVendorById(1L);

        // Assert
        assertTrue(found.isPresent());
        assertEquals(1L, found.get().getId());
        verify(vendorRepository).findById(1L);
    }

    @Test
    void testGetVendorById_whenNotExists() {
        // Arrange
        when(vendorRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        Optional<Vendor> found = vendorService.getVendorById(99L);

        // Assert
        assertFalse(found.isPresent());
        verify(vendorRepository).findById(99L);
    }

    @Test
    void testCreateVendor() {
        // Arrange
        Vendor vendorToSave = new Vendor();
        vendorToSave.setName("Vendor Name");

        when(vendorRepository.save(any(Vendor.class))).thenReturn(vendorToSave);

        // Act
        Vendor savedVendor = vendorService.createVendor(vendorToSave);

        // Assert
        assertNotNull(savedVendor);
        assertEquals("Vendor Name", savedVendor.getName());
        verify(vendorRepository).save(vendorToSave);
    }

    @Test
    void testUpdateVendor_whenExists() {
        // Arrange
        Vendor existingVendor = new Vendor();
        existingVendor.setId(1L);
        existingVendor.setName("Old Vendor");

        Vendor vendorDetails = new Vendor();
        vendorDetails.setName("New Vendor");

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(existingVendor));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Return the saved entity

        // Act
        Optional<Vendor> updated = vendorService.updateVendor(1L, vendorDetails);

        // Assert
        assertTrue(updated.isPresent());
        assertEquals("New Vendor", updated.get().getName()); // Check that the address was updated
        verify(vendorRepository).findById(1L);
        verify(vendorRepository).save(existingVendor); // Verify save was called on the *existing* entity
    }

    @Test
    void testUpdateVendor_whenNotExists() {
        // Arrange
        Vendor vendorDetails = new Vendor();
        when(vendorRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        Optional<Vendor> updated = vendorService.updateVendor(99L, vendorDetails);

        // Assert
        assertFalse(updated.isPresent());
        verify(vendorRepository).findById(99L);
    }

    @Test
    void testDeleteVendor_whenExists() {
        // Arrange
        Vendor existingVendor = new Vendor();
        existingVendor.setId(1L);
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(existingVendor));

        // Act
        boolean wasDeleted = vendorService.deleteVendor(1L);

        // Assert
        assertTrue(wasDeleted);
        verify(vendorRepository).findById(1L);
        verify(vendorRepository).delete(existingVendor); // Verify the delete call
    }

    @Test
    void testDeleteVendor_whenNotExists() {
        // Arrange
        when(vendorRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        boolean wasDeleted = vendorService.deleteVendor(99L);

        // Assert
        assertFalse(wasDeleted);
        verify(vendorRepository).findById(99L);
    }
}
