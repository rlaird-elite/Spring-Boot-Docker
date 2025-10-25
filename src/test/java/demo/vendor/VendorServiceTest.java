package com.example.demo.vendor;

import com.example.demo.user.User; // Import User
import org.junit.jupiter.api.AfterEach; // Import AfterEach
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
// Remove Mockito imports for InjectMocks/Mock/MockitoAnnotations
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired; // Autowire the service
import org.springframework.boot.test.context.SpringBootTest; // Use SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean; // Use MockBean
import org.springframework.context.annotation.Import; // Import necessary configs if needed
import org.springframework.security.access.AccessDeniedException; // Import AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
// Import Spring Security Test annotations if you prefer that style later
// import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

// --- Use @SpringBootTest to load context and enable AOP/Security ---
@SpringBootTest // Loads the full application context
// If needed, specify classes to load for a more focused context:
// @SpringBootTest(classes = {VendorService.class, SecurityConfig.class /*, other needed configs */})
public class VendorServiceTest {

    @MockBean // Use @MockBean for dependencies when using @SpringBootTest
    private VendorRepository vendorRepository;

    @Autowired // Inject the actual service bean from the Spring context
    private VendorService vendorService;

    private static final Long MOCK_TENANT_ID = 1L;
    // AutoCloseable is no longer needed

    // Helper to set up security context (remains the same)
    private void setupMockSecurityContext(User.Role role) {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername(role == User.Role.ADMIN ? "admin@example.com" : "user@example.com");
        mockUser.setTenantId(MOCK_TENANT_ID);
        mockUser.setRole(role); // Set the specified role

        Authentication authentication = new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // @BeforeEach no longer needed for MockitoAnnotations.openMocks
    // @BeforeEach
    // void setUp() { ... }

    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext(); // Clean up security context after each test
        // No need to close mocks manually with @MockBean
    }


    @Test
    void whenGetAllVendors_asUser_thenReturnsTenantVendors() {
        setupMockSecurityContext(User.Role.USER); // Set context for USER
        // Arrange
        Vendor vendor1 = new Vendor(); vendor1.setTenantId(MOCK_TENANT_ID);
        Vendor vendor2 = new Vendor(); vendor2.setTenantId(MOCK_TENANT_ID);
        when(vendorRepository.findAllByTenantId(MOCK_TENANT_ID)).thenReturn(List.of(vendor1, vendor2));

        // Act & Assert
        assertDoesNotThrow(() -> {
            List<Vendor> vendors = vendorService.getAllVendors();
            assertEquals(2, vendors.size());
            verify(vendorRepository).findAllByTenantId(MOCK_TENANT_ID);
        });
    }

    @Test
    void whenGetVendorById_givenValidIdAndTenant_asUser_thenReturnsVendor() {
        setupMockSecurityContext(User.Role.USER);
        Long vendorId = 1L;
        Vendor vendor = new Vendor(); vendor.setId(vendorId); vendor.setTenantId(MOCK_TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(vendorId, MOCK_TENANT_ID)).thenReturn(Optional.of(vendor));
        Optional<Vendor> foundVendor = vendorService.getVendorById(vendorId);
        assertTrue(foundVendor.isPresent());
        verify(vendorRepository).findByIdAndTenantId(vendorId, MOCK_TENANT_ID);
    }

    @Test
    void whenCreateVendor_asUser_thenSetsTenantIdAndSaves() {
        setupMockSecurityContext(User.Role.USER);
        Vendor vendorToSave = new Vendor(); vendorToSave.setName("New Vendor");
        // Mock return object structure
        Vendor savedVendorResult = new Vendor();
        savedVendorResult.setId(1L);
        savedVendorResult.setName("New Vendor");
        savedVendorResult.setTenantId(MOCK_TENANT_ID);


        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> {
            Vendor v = invocation.getArgument(0);
            assertEquals(MOCK_TENANT_ID, v.getTenantId());
            // Return a distinct object representing the saved state
            Vendor resultVendor = new Vendor();
            resultVendor.setId(1L); // Simulate ID generation
            resultVendor.setName(v.getName());
            resultVendor.setSpecialty(v.getSpecialty());
            resultVendor.setContactInfo(v.getContactInfo());
            resultVendor.setTenantId(MOCK_TENANT_ID); // Ensure tenantId is set
            return resultVendor;
        });

        Vendor result = vendorService.createVendor(vendorToSave);
        assertNotNull(result);
        assertEquals(MOCK_TENANT_ID, result.getTenantId());
        assertEquals(1L, result.getId()); // Check ID if mock returns it
        verify(vendorRepository).save(vendorToSave); // Verify interaction
    }

    @Test
    void whenUpdateVendor_givenValidIdAndTenant_asUser_thenUpdatesAndSaves() {
        setupMockSecurityContext(User.Role.USER);
        Long vendorId = 1L;
        Vendor existingVendor = new Vendor(); existingVendor.setId(vendorId); existingVendor.setTenantId(MOCK_TENANT_ID); existingVendor.setName("Old Name");
        Vendor updatedDetails = new Vendor(); updatedDetails.setName("New Name");
        when(vendorRepository.findByIdAndTenantId(vendorId, MOCK_TENANT_ID)).thenReturn(Optional.of(existingVendor));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Return the saved entity
        Optional<Vendor> result = vendorService.updateVendor(vendorId, updatedDetails);
        assertTrue(result.isPresent());
        assertEquals("New Name", result.get().getName());
        verify(vendorRepository).findByIdAndTenantId(vendorId, MOCK_TENANT_ID);
        verify(vendorRepository).save(existingVendor);
    }


    // --- Tests for Delete Authorization (Should Pass Now) ---

    @Test
    void whenDeleteVendor_givenValidIdAndTenant_asAdmin_thenDeletesAndReturnsTrue() {
        setupMockSecurityContext(User.Role.ADMIN); // Set context for ADMIN
        // Arrange
        Long vendorId = 1L;
        when(vendorRepository.existsByIdAndTenantId(vendorId, MOCK_TENANT_ID)).thenReturn(true);
        // No need to explicitly mock deleteById if it returns void

        // Act & Assert
        assertDoesNotThrow(() -> { // Expect no security exception
            boolean result = vendorService.deleteVendor(vendorId);
            assertTrue(result);
            verify(vendorRepository).existsByIdAndTenantId(vendorId, MOCK_TENANT_ID);
            verify(vendorRepository).deleteById(vendorId);
        });
    }

    @Test
    void whenDeleteVendor_givenValidIdAndTenant_asUser_thenThrowsAccessDenied() {
        setupMockSecurityContext(User.Role.USER); // Set context for USER
        // Arrange
        Long vendorId = 1L;
        // Mock existence check - delete should not be reached due to security
        // No need to mock this if security happens first, but can be useful
        when(vendorRepository.existsByIdAndTenantId(vendorId, MOCK_TENANT_ID)).thenReturn(true);

        // Act & Assert
        // Expect AccessDeniedException because @PreAuthorize is now active
        assertThrows(AccessDeniedException.class, () -> {
            vendorService.deleteVendor(vendorId);
        }, "Should throw AccessDeniedException for USER role trying to delete");

        // Verify delete was NOT called because security blocked it
        verify(vendorRepository, never()).deleteById(anyLong());
    }

    @Test
    void whenDeleteVendor_givenInvalidIdOrTenant_asAdmin_thenReturnsFalse() {
        setupMockSecurityContext(User.Role.ADMIN); // Set context for ADMIN
        // Arrange
        Long vendorId = 99L;
        when(vendorRepository.existsByIdAndTenantId(vendorId, MOCK_TENANT_ID)).thenReturn(false);

        // Act & Assert
        assertDoesNotThrow(() -> { // No security exception expected for ADMIN
            boolean result = vendorService.deleteVendor(vendorId);
            assertFalse(result); // Returns false because vendor doesn't exist for tenant
            verify(vendorRepository).existsByIdAndTenantId(vendorId, MOCK_TENANT_ID);
            verify(vendorRepository, never()).deleteById(anyLong());
        });
    }

    @Test
    void whenDeleteVendor_givenInvalidIdOrTenant_asUser_thenThrowsAccessDenied() {
        setupMockSecurityContext(User.Role.USER); // Set context for USER
        // Arrange
        Long vendorId = 99L;
        // No need to mock existsByIdAndTenantId if security prevents call

        // Act & Assert
        // @PreAuthorize runs before method body, so AccessDenied should happen first
        assertThrows(AccessDeniedException.class, () -> {
            vendorService.deleteVendor(vendorId);
        }, "Should throw AccessDeniedException for USER role even if vendor doesn't exist");

        // Verify delete was NOT called
        verify(vendorRepository, never()).deleteById(anyLong());
        // Verify existence check might not be called either
        verify(vendorRepository, never()).existsByIdAndTenantId(anyLong(), anyLong());
    }

}

