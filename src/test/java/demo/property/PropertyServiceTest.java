package com.example.demo.property;

import com.example.demo.permission.Permission; // Import Permission
import com.example.demo.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException; // Import AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet; // Import HashSet
import java.util.List;
import java.util.Optional;
import java.util.Set; // Import Set

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.example.demo.permission.Permission; // Import Permission
import com.example.demo.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException; // Import AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet; // Import HashSet
import java.util.List;
import java.util.Optional;
import java.util.Set; // Import Set

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest // Loads the full application context
public class PropertyServiceTest {

    @MockBean // Use @MockBean for dependencies when using @SpringBootTest
    private PropertyRepository propertyRepository;

    @Autowired // Inject the actual service bean from the Spring context
    private PropertyService propertyService;

    // Define mock user and tenant ID
    private static final Long MOCK_TENANT_ID = 1L;

    // --- Define mock permissions ---
    private Permission mockUserPermission;
    private Permission mockAdminDeletePropertyPermission; // Specific admin permission
    // --- End mock permissions ---

    // Helper to set up security context
    private void setupMockSecurityContext(boolean isAdmin) {
        User mockUser = new User();
        mockUser.setId(isAdmin ? 2L : 1L);
        mockUser.setUsername(isAdmin ? "admin@example.com" : "user@example.com");
        mockUser.setTenantId(MOCK_TENANT_ID);

        // --- Set Permissions based on isAdmin flag ---
        Set<Permission> permissions = new HashSet<>();
        if (mockUserPermission != null) {
            permissions.add(mockUserPermission); // All users get the basic permission
        }
        // --- FIX: Add the specific admin permission if isAdmin ---
        if (isAdmin && mockAdminDeletePropertyPermission != null) {
            permissions.add(mockAdminDeletePropertyPermission); // Add delete permission for ADMIN
        }
        // --- END FIX ---
        mockUser.setPermissions(permissions); // Assign the Set<Permission>
        // --- End Set Permissions ---

        Authentication authentication = new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @BeforeEach
    void setUpPermissions() {
        mockUserPermission = new Permission("PERMISSION_READ_OWN_DATA");
        mockUserPermission.setId(100L);
        // Ensure this permission matches @PreAuthorize in PropertyService
        mockAdminDeletePropertyPermission = new Permission("PERMISSION_DELETE_PROPERTY");
        mockAdminDeletePropertyPermission.setId(103L);
    }


    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
    }


    @Test
    void whenGetAllProperties_asUser_thenReturnsTenantProperties() {
        setupMockSecurityContext(false);
        Property prop1 = new Property(); prop1.setTenantId(MOCK_TENANT_ID);
        Property prop2 = new Property(); prop2.setTenantId(MOCK_TENANT_ID);
        when(propertyRepository.findAllByTenantId(MOCK_TENANT_ID)).thenReturn(List.of(prop1, prop2));
        List<Property> properties = propertyService.getAllProperties();
        assertEquals(2, properties.size());
        verify(propertyRepository).findAllByTenantId(MOCK_TENANT_ID);
    }

    @Test
    void whenGetPropertyById_givenValidIdAndTenant_asUser_thenReturnsProperty() {
        setupMockSecurityContext(false);
        Long propertyId = 1L;
        Property property = new Property(); property.setId(propertyId); property.setTenantId(MOCK_TENANT_ID);
        when(propertyRepository.findByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(Optional.of(property));
        Optional<Property> foundProperty = propertyService.getPropertyById(propertyId);
        assertTrue(foundProperty.isPresent());
        verify(propertyRepository).findByIdAndTenantId(propertyId, MOCK_TENANT_ID);
    }

    @Test
    void whenGetPropertyById_givenInvalidTenant_asUser_thenReturnsEmpty() {
        setupMockSecurityContext(false);
        Long propertyId = 1L;
        when(propertyRepository.findByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(Optional.empty());
        Optional<Property> foundProperty = propertyService.getPropertyById(propertyId);
        assertFalse(foundProperty.isPresent());
        verify(propertyRepository).findByIdAndTenantId(propertyId, MOCK_TENANT_ID);
    }

    @Test
    void whenCreateProperty_asUser_thenSetsTenantIdAndSaves() {
        setupMockSecurityContext(false);
        Property propertyToSave = new Property(); propertyToSave.setAddress("New Address");
        Property savedPropertyResult = new Property();
        savedPropertyResult.setId(1L);
        savedPropertyResult.setAddress("New Address");
        savedPropertyResult.setTenantId(MOCK_TENANT_ID);

        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> {
            Property p = invocation.getArgument(0);
            assertEquals(MOCK_TENANT_ID, p.getTenantId());
            Property resultProp = new Property();
            resultProp.setId(1L);
            resultProp.setAddress(p.getAddress());
            resultProp.setType(p.getType());
            resultProp.setBedrooms(p.getBedrooms());
            resultProp.setBathrooms(p.getBathrooms());
            resultProp.setTenantId(MOCK_TENANT_ID);
            return resultProp;
        });

        Property result = propertyService.createProperty(propertyToSave);
        assertNotNull(result);
        assertEquals(MOCK_TENANT_ID, result.getTenantId());
        assertEquals(1L, result.getId());
        verify(propertyRepository).save(propertyToSave);
    }

    @Test
    void whenUpdateProperty_givenValidIdAndTenant_asUser_thenUpdatesAndSaves() {
        setupMockSecurityContext(false);
        Long propertyId = 1L;
        Property existingProperty = new Property(); existingProperty.setId(propertyId); existingProperty.setTenantId(MOCK_TENANT_ID); existingProperty.setAddress("Old Address");
        Property updatedDetails = new Property(); updatedDetails.setAddress("New Address");
        when(propertyRepository.findByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(Optional.of(existingProperty));
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Optional<Property> result = propertyService.updateProperty(propertyId, updatedDetails);
        assertTrue(result.isPresent());
        assertEquals("New Address", result.get().getAddress());
        verify(propertyRepository).findByIdAndTenantId(propertyId, MOCK_TENANT_ID);
        verify(propertyRepository).save(existingProperty);
    }

    @Test
    void whenUpdateProperty_givenInvalidIdOrTenant_asUser_thenReturnsEmpty() {
        setupMockSecurityContext(false);
        Long propertyId = 99L;
        Property updatedDetails = new Property(); updatedDetails.setAddress("New Address");
        when(propertyRepository.findByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(Optional.empty());
        Optional<Property> result = propertyService.updateProperty(propertyId, updatedDetails);
        assertFalse(result.isPresent());
        verify(propertyRepository).findByIdAndTenantId(propertyId, MOCK_TENANT_ID);
        verify(propertyRepository, never()).save(any(Property.class));
    }

    // --- Tests for Delete Authorization ---

    @Test
    void whenDeleteProperty_givenValidIdAndTenant_asAdmin_thenDeletesAndReturnsTrue() {
        setupMockSecurityContext(true); // Set context for ADMIN
        Long propertyId = 1L;
        when(propertyRepository.existsByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(true);

        assertDoesNotThrow(() -> {
            boolean result = propertyService.deleteProperty(propertyId);
            assertTrue(result);
        });
        verify(propertyRepository).existsByIdAndTenantId(propertyId, MOCK_TENANT_ID);
        verify(propertyRepository).deleteById(propertyId);
    }

    @Test
    void whenDeleteProperty_givenValidIdAndTenant_asUser_thenThrowsAccessDenied() {
        setupMockSecurityContext(false); // Set context for USER
        Long propertyId = 1L;
        when(propertyRepository.existsByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(true);

        assertThrows(AccessDeniedException.class, () -> {
            propertyService.deleteProperty(propertyId);
        }, "Should throw AccessDeniedException for USER role trying to delete");

        verify(propertyRepository, never()).deleteById(anyLong());
    }

    @Test
    void whenDeleteProperty_givenInvalidIdOrTenant_asAdmin_thenReturnsFalse() {
        setupMockSecurityContext(true); // Set context for ADMIN
        Long propertyId = 99L;
        when(propertyRepository.existsByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(false);

        assertDoesNotThrow(() -> {
            boolean result = propertyService.deleteProperty(propertyId);
            assertFalse(result);
        });
        verify(propertyRepository).existsByIdAndTenantId(propertyId, MOCK_TENANT_ID);
        verify(propertyRepository, never()).deleteById(anyLong());
    }

    @Test
    void whenDeleteProperty_givenInvalidIdOrTenant_asUser_thenThrowsAccessDenied() {
        setupMockSecurityContext(false); // Set context for USER
        Long propertyId = 99L;
        when(propertyRepository.existsByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> {
            propertyService.deleteProperty(propertyId);
        }, "Should throw AccessDeniedException for USER role even if property doesn't exist");

        verify(propertyRepository, never()).deleteById(anyLong());
        verify(propertyRepository, never()).existsByIdAndTenantId(anyLong(), anyLong());
    }
}