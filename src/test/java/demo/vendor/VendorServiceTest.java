package com.example.demo.vendor;

import com.example.demo.permission.Permission; // Import Permission
import com.example.demo.user.User;
import org.junit.jupiter.api.AfterEach; // Import AfterEach
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired; // Autowire the service
import org.springframework.boot.test.context.SpringBootTest; // Use SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean; // Use MockBean
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
public class VendorServiceTest {

    @MockBean // Use @MockBean for dependencies when using @SpringBootTest
    private VendorRepository vendorRepository;

    @Autowired // Inject the actual service bean from the Spring context
    private VendorService vendorService;

    private static final Long MOCK_TENANT_ID = 1L;

    // --- Define mock permissions ---
    private Permission mockUserPermission;
    private Permission mockAdminDeleteVendorPermission; // Specific admin permission
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
            permissions.add(mockUserPermission);
        }
        // --- FIX: Add the specific admin permission if isAdmin ---
        if (isAdmin && mockAdminDeleteVendorPermission != null) {
            permissions.add(mockAdminDeleteVendorPermission); // Add delete permission for ADMIN
        }
        // --- END FIX ---
        mockUser.setPermissions(permissions);
        // --- End Set Permissions ---

        Authentication authentication = new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @BeforeEach
    void setUpPermissions() {
        // Initialize mock permission objects
        mockUserPermission = new Permission("PERMISSION_READ_OWN_DATA");
        mockUserPermission.setId(100L);
        // Ensure this permission matches @PreAuthorize in VendorService
        mockAdminDeleteVendorPermission = new Permission("PERMISSION_DELETE_VENDOR");
        mockAdminDeleteVendorPermission.setId(101L);
    }

    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
    }


    @Test
    void whenGetAllVendors_asUser_thenReturnsTenantVendors() {
        setupMockSecurityContext(false);
        Vendor vendor1 = new Vendor(); vendor1.setTenantId(MOCK_TENANT_ID);
        Vendor vendor2 = new Vendor(); vendor2.setTenantId(MOCK_TENANT_ID);
        when(vendorRepository.findAllByTenantId(MOCK_TENANT_ID)).thenReturn(List.of(vendor1, vendor2));

        assertDoesNotThrow(() -> {
            List<Vendor> vendors = vendorService.getAllVendors();
            assertEquals(2, vendors.size());
            verify(vendorRepository).findAllByTenantId(MOCK_TENANT_ID);
        });
    }

    @Test
    void whenGetVendorById_givenValidIdAndTenant_asUser_thenReturnsVendor() {
        setupMockSecurityContext(false);
        Long vendorId = 1L;
        Vendor vendor = new Vendor(); vendor.setId(vendorId); vendor.setTenantId(MOCK_TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(vendorId, MOCK_TENANT_ID)).thenReturn(Optional.of(vendor));
        Optional<Vendor> foundVendor = vendorService.getVendorById(vendorId);
        assertTrue(foundVendor.isPresent());
        verify(vendorRepository).findByIdAndTenantId(vendorId, MOCK_TENANT_ID);
    }

    @Test
    void whenCreateVendor_asUser_thenSetsTenantIdAndSaves() {
        setupMockSecurityContext(false);
        Vendor vendorToSave = new Vendor(); vendorToSave.setName("New Vendor");
        Vendor savedVendorResult = new Vendor();
        savedVendorResult.setId(1L);
        savedVendorResult.setName("New Vendor");
        savedVendorResult.setTenantId(MOCK_TENANT_ID);

        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> {
            Vendor v = invocation.getArgument(0);
            assertEquals(MOCK_TENANT_ID, v.getTenantId());
            Vendor resultVendor = new Vendor();
            resultVendor.setId(1L);
            resultVendor.setName(v.getName());
            resultVendor.setSpecialty(v.getSpecialty());
            resultVendor.getPhone();
            resultVendor.setTenantId(MOCK_TENANT_ID);
            return resultVendor;
        });

        Vendor result = vendorService.createVendor(vendorToSave);
        assertNotNull(result);
        assertEquals(MOCK_TENANT_ID, result.getTenantId());
        assertEquals(1L, result.getId());
        verify(vendorRepository).save(vendorToSave);
    }

    @Test
    void whenUpdateVendor_givenValidIdAndTenant_asUser_thenUpdatesAndSaves() {
        setupMockSecurityContext(false);
        Long vendorId = 1L;
        Vendor existingVendor = new Vendor(); existingVendor.setId(vendorId); existingVendor.setTenantId(MOCK_TENANT_ID); existingVendor.setName("Old Name");
        Vendor updatedDetails = new Vendor(); updatedDetails.setName("New Name");
        when(vendorRepository.findByIdAndTenantId(vendorId, MOCK_TENANT_ID)).thenReturn(Optional.of(existingVendor));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Optional<Vendor> result = vendorService.updateVendor(vendorId, updatedDetails);
        assertTrue(result.isPresent());
        assertEquals("New Name", result.get().getName());
        verify(vendorRepository).findByIdAndTenantId(vendorId, MOCK_TENANT_ID);
        verify(vendorRepository).save(existingVendor);
    }


    // --- Tests for Delete Authorization ---

    @Test
    void whenDeleteVendor_givenValidIdAndTenant_asAdmin_thenDeletesAndReturnsTrue() {
        setupMockSecurityContext(true); // Set context for ADMIN
        Long vendorId = 1L;
        when(vendorRepository.existsByIdAndTenantId(vendorId, MOCK_TENANT_ID)).thenReturn(true);

        assertDoesNotThrow(() -> {
            boolean result = vendorService.deleteVendor(vendorId);
            assertTrue(result);
        });
        verify(vendorRepository).existsByIdAndTenantId(vendorId, MOCK_TENANT_ID);
        verify(vendorRepository).deleteById(vendorId);
    }

    @Test
    void whenDeleteVendor_givenValidIdAndTenant_asUser_thenThrowsAccessDenied() {
        setupMockSecurityContext(false); // Set context for USER
        Long vendorId = 1L;
        when(vendorRepository.existsByIdAndTenantId(vendorId, MOCK_TENANT_ID)).thenReturn(true);

        assertThrows(AccessDeniedException.class, () -> {
            vendorService.deleteVendor(vendorId);
        }, "Should throw AccessDeniedException for USER role trying to delete");

        verify(vendorRepository, never()).deleteById(anyLong());
    }

    @Test
    void whenDeleteVendor_givenInvalidIdOrTenant_asAdmin_thenReturnsFalse() {
        setupMockSecurityContext(true); // Set context for ADMIN
        Long vendorId = 99L;
        when(vendorRepository.existsByIdAndTenantId(vendorId, MOCK_TENANT_ID)).thenReturn(false);

        assertDoesNotThrow(() -> {
            boolean result = vendorService.deleteVendor(vendorId);
            assertFalse(result);
        });
        verify(vendorRepository).existsByIdAndTenantId(vendorId, MOCK_TENANT_ID);
        verify(vendorRepository, never()).deleteById(anyLong());
    }

    @Test
    void whenDeleteVendor_givenInvalidIdOrTenant_asUser_thenThrowsAccessDenied() {
        setupMockSecurityContext(false); // Set context for USER
        Long vendorId = 99L;
        when(vendorRepository.existsByIdAndTenantId(vendorId, MOCK_TENANT_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> {
            vendorService.deleteVendor(vendorId);
        }, "Should throw AccessDeniedException for USER role even if vendor doesn't exist");

        verify(vendorRepository, never()).deleteById(anyLong());
        verify(vendorRepository, never()).existsByIdAndTenantId(anyLong(), anyLong());
    }

}

