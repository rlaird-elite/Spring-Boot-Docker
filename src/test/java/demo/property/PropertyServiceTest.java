package com.example.demo.property;

import com.example.demo.user.User; // Import User
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Security imports
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class PropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @InjectMocks
    private PropertyService propertyService;

    // Define a mock user and tenant ID for tests
    private static final Long MOCK_TENANT_ID = 1L;
    private User mockUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // --- Mock Security Context ---
        mockUser = new User();
        mockUser.setId(1L); // Assign an ID
        mockUser.setUsername("test@example.com");
        mockUser.setTenantId(MOCK_TENANT_ID); // Set the tenant ID for the mock user
        mockUser.setRole(User.Role.USER); // Set a role

        // Create mock Authentication and SecurityContext
        Authentication authentication = new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        // --- End Mock Security Context ---
    }

    // --- Update existing tests to use tenant-aware mocks ---

    @Test
    void whenGetAllProperties_thenReturnsTenantProperties() {
        // Arrange
        Property prop1 = new Property();
        prop1.setTenantId(MOCK_TENANT_ID);
        Property prop2 = new Property();
        prop2.setTenantId(MOCK_TENANT_ID);
        when(propertyRepository.findAllByTenantId(MOCK_TENANT_ID)).thenReturn(List.of(prop1, prop2));

        // Act
        List<Property> properties = propertyService.getAllProperties();

        // Assert
        assertEquals(2, properties.size());
        verify(propertyRepository).findAllByTenantId(MOCK_TENANT_ID); // Verify correct method called
    }

    @Test
    void whenGetPropertyById_givenValidIdAndTenant_thenReturnsProperty() {
        // Arrange
        Long propertyId = 1L;
        Property property = new Property();
        property.setId(propertyId);
        property.setTenantId(MOCK_TENANT_ID);
        when(propertyRepository.findByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(Optional.of(property));

        // Act
        Optional<Property> foundProperty = propertyService.getPropertyById(propertyId);

        // Assert
        assertTrue(foundProperty.isPresent());
        assertEquals(propertyId, foundProperty.get().getId());
        verify(propertyRepository).findByIdAndTenantId(propertyId, MOCK_TENANT_ID);
    }

    @Test
    void whenGetPropertyById_givenInvalidTenant_thenReturnsEmpty() {
        // Arrange
        Long propertyId = 1L;
        // Assume the repo returns empty if tenant ID doesn't match
        when(propertyRepository.findByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(Optional.empty());

        // Act
        Optional<Property> foundProperty = propertyService.getPropertyById(propertyId);

        // Assert
        assertFalse(foundProperty.isPresent());
        verify(propertyRepository).findByIdAndTenantId(propertyId, MOCK_TENANT_ID);
    }

    @Test
    void whenCreateProperty_thenSetsTenantIdAndSaves() {
        // Arrange
        Property propertyToSave = new Property(); // Input property doesn't have tenantId yet
        Property savedProperty = new Property(); // Mock returned property
        savedProperty.setTenantId(MOCK_TENANT_ID); // Ensure mock return has tenantId

        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> {
            Property p = invocation.getArgument(0);
            // Verify tenantId was set *before* save is called
            assertEquals(MOCK_TENANT_ID, p.getTenantId());
            savedProperty.setId(1L); // Simulate ID generation
            savedProperty.setAddress(p.getAddress()); // Copy other fields
            // ... copy other fields ...
            return savedProperty;
        });


        // Act
        Property result = propertyService.createProperty(propertyToSave);

        // Assert
        assertNotNull(result);
        assertEquals(MOCK_TENANT_ID, result.getTenantId());
        verify(propertyRepository).save(propertyToSave); // Verify save was called
    }

    @Test
    void whenUpdateProperty_givenValidIdAndTenant_thenUpdatesAndSaves() {
        // Arrange
        Long propertyId = 1L;
        Property existingProperty = new Property();
        existingProperty.setId(propertyId);
        existingProperty.setTenantId(MOCK_TENANT_ID);
        existingProperty.setAddress("Old Address");

        Property updatedDetails = new Property();
        updatedDetails.setAddress("New Address");

        when(propertyRepository.findByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(Optional.of(existingProperty));
        // Mock the save operation for the update
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // Act
        Optional<Property> result = propertyService.updateProperty(propertyId, updatedDetails);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("New Address", result.get().getAddress());
        assertEquals(MOCK_TENANT_ID, result.get().getTenantId()); // Tenant ID should remain
        verify(propertyRepository).findByIdAndTenantId(propertyId, MOCK_TENANT_ID);
        verify(propertyRepository).save(existingProperty); // Verify save was called on the updated object
    }

    @Test
    void whenUpdateProperty_givenInvalidIdOrTenant_thenReturnsEmpty() {
        // Arrange
        Long propertyId = 99L;
        Property updatedDetails = new Property();
        updatedDetails.setAddress("New Address");
        when(propertyRepository.findByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(Optional.empty());

        // Act
        Optional<Property> result = propertyService.updateProperty(propertyId, updatedDetails);

        // Assert
        assertFalse(result.isPresent());
        verify(propertyRepository).findByIdAndTenantId(propertyId, MOCK_TENANT_ID);
        verify(propertyRepository, never()).save(any(Property.class)); // Ensure save wasn't called
    }

    @Test
    void whenDeleteProperty_givenValidIdAndTenant_thenDeletesAndReturnsTrue() {
        // Arrange
        Long propertyId = 1L;
        when(propertyRepository.existsByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(true);
        // No need to mock deleteById, just verify it's called

        // Act
        boolean result = propertyService.deleteProperty(propertyId);

        // Assert
        assertTrue(result);
        verify(propertyRepository).existsByIdAndTenantId(propertyId, MOCK_TENANT_ID);
        verify(propertyRepository).deleteById(propertyId); // Verify deleteById was called
    }

    @Test
    void whenDeleteProperty_givenInvalidIdOrTenant_thenReturnsFalse() {
        // Arrange
        Long propertyId = 99L;
        when(propertyRepository.existsByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(false);

        // Act
        boolean result = propertyService.deleteProperty(propertyId);

        // Assert
        assertFalse(result);
        verify(propertyRepository).existsByIdAndTenantId(propertyId, MOCK_TENANT_ID);
        verify(propertyRepository, never()).deleteById(anyLong()); // Ensure deleteById wasn't called
    }
}

