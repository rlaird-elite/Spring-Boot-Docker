package com.example.demo.vendor;

import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.user.User; // Import User
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException; // Import AccessDeniedException
import org.springframework.security.test.context.support.WithMockUser; // Import WithMockUser
import org.springframework.test.web.servlet.MockMvc;

// Import SecurityConfig and UserDetailsService for context
import com.example.demo.SecurityConfig;
import com.example.demo.user.CustomUserDetailsService;
import com.example.demo.user.JwtTokenProvider;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*; // Import security post processors

@WebMvcTest(VendorController.class) // Specify the controller to test
@Import({SecurityConfig.class, GlobalExceptionHandler.class}) // Import SecurityConfig and Exception Handler
public class VendorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VendorService vendorService; // Use VendorService
    @MockBean
    private JwtTokenProvider jwtTokenProvider; // Needed by JwtAuthenticationFilter
    @MockBean
    private CustomUserDetailsService customUserDetailsService; // Needed by SecurityConfig/Filter & @WithMockUser

    @Autowired
    private ObjectMapper objectMapper;

    // Define mock user and tenant ID
    private static final Long MOCK_TENANT_ID = 1L;
    private User mockUserDetails;

    @BeforeEach
    void setupUserDetails() {
        // --- Setup mock UserDetails for @WithMockUser ---
        mockUserDetails = new User();
        mockUserDetails.setId(1L);
        mockUserDetails.setUsername("user"); // Default username for @WithMockUser
        mockUserDetails.setPassword("password");
        mockUserDetails.setTenantId(MOCK_TENANT_ID);
        mockUserDetails.setRole(User.Role.USER); // Default to USER for most tests

        // Mock the userDetailsService to return our mock user
        when(customUserDetailsService.loadUserByUsername("user")).thenReturn(mockUserDetails);

        // --- Setup mock UserDetails for ADMIN tests ---
        User mockAdminDetails = new User();
        mockAdminDetails.setId(2L);
        mockAdminDetails.setUsername("admin"); // Username used in @WithMockUser(username="admin")
        mockAdminDetails.setPassword("password");
        mockAdminDetails.setTenantId(MOCK_TENANT_ID);
        mockAdminDetails.setRole(User.Role.ADMIN); // Set role to ADMIN

        // Mock the userDetailsService to return the admin user when requested
        when(customUserDetailsService.loadUserByUsername("admin")).thenReturn(mockAdminDetails);

    }

    // Test for Unauthenticated Access
    @Test
    void whenGetAllVendors_withoutAuthentication_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/vendors"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser // Default user (ROLE_USER)
    void whenCreateVendor_thenReturnsCreatedVendor() throws Exception {
        Vendor vendor = new Vendor();
        vendor.setName("Reliable Plumbing");
        vendor.setSpecialty("Plumbing");
        // No need to set tenantId here, service does it

        Vendor savedVendor = new Vendor();
        savedVendor.setId(1L);
        savedVendor.setName("Reliable Plumbing");
        savedVendor.setSpecialty("Plumbing");
        savedVendor.setTenantId(MOCK_TENANT_ID); // Service should set this

        when(vendorService.createVendor(any(Vendor.class))).thenReturn(savedVendor);

        mockMvc.perform(post("/api/vendors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendor)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Reliable Plumbing"))
                .andExpect(jsonPath("$.tenantId").value(MOCK_TENANT_ID));
    }


    @Test
    @WithMockUser // Default user (ROLE_USER)
    void whenGetAllVendors_thenReturnsVendorList() throws Exception {
        Vendor vendor1 = new Vendor();
        vendor1.setId(1L);
        vendor1.setName("Plumber One");
        vendor1.setTenantId(MOCK_TENANT_ID);

        Vendor vendor2 = new Vendor();
        vendor2.setId(2L);
        vendor2.setName("Electrician Two");
        vendor2.setTenantId(MOCK_TENANT_ID);

        when(vendorService.getAllVendors()).thenReturn(List.of(vendor1, vendor2));

        mockMvc.perform(get("/api/vendors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Plumber One"))
                .andExpect(jsonPath("$[1].name").value("Electrician Two"));
    }

    @Test
    @WithMockUser // Default user (ROLE_USER)
    void whenGetVendorById_givenVendorExists_thenReturnsVendor() throws Exception {
        Long vendorId = 1L;
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setName("Test Vendor");
        vendor.setTenantId(MOCK_TENANT_ID);

        when(vendorService.getVendorById(vendorId)).thenReturn(Optional.of(vendor));

        mockMvc.perform(get("/api/vendors/{id}", vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendorId))
                .andExpect(jsonPath("$.name").value("Test Vendor"))
                .andExpect(jsonPath("$.tenantId").value(MOCK_TENANT_ID));
    }

    @Test
    @WithMockUser // Default user (ROLE_USER)
    void whenGetVendorById_givenVendorDoesNotExistOrDifferentTenant_thenReturnsNotFound() throws Exception {
        Long vendorId = 99L;
        when(vendorService.getVendorById(vendorId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/vendors/{id}", vendorId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser // Default user (ROLE_USER)
    void whenUpdateVendor_givenVendorExists_thenReturnsUpdatedVendor() throws Exception {
        Long vendorId = 1L;
        Vendor updatedDetails = new Vendor();
        updatedDetails.setName("Updated Vendor Name");
        updatedDetails.setSpecialty("Updated Specialty");

        Vendor returnedVendor = new Vendor();
        returnedVendor.setId(vendorId);
        returnedVendor.setName("Updated Vendor Name");
        returnedVendor.setSpecialty("Updated Specialty");
        returnedVendor.setTenantId(MOCK_TENANT_ID);

        when(vendorService.updateVendor(eq(vendorId), any(Vendor.class))).thenReturn(Optional.of(returnedVendor));

        mockMvc.perform(put("/api/vendors/{id}", vendorId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendorId))
                .andExpect(jsonPath("$.name").value("Updated Vendor Name"))
                .andExpect(jsonPath("$.tenantId").value(MOCK_TENANT_ID));
    }

    @Test
    @WithMockUser // Default user (ROLE_USER)
    void whenUpdateVendor_givenVendorDoesNotExistOrDifferentTenant_thenReturnsNotFound() throws Exception {
        Long vendorId = 99L;
        Vendor updatedDetails = new Vendor();
        updatedDetails.setName("Updated Name"); // Still need valid data for request body

        when(vendorService.updateVendor(eq(vendorId), any(Vendor.class))).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/vendors/{id}", vendorId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDetails)))
                .andExpect(status().isNotFound());
    }

    // --- NEW: Tests for Delete Authorization ---
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"}) // Simulate ADMIN user
    void whenDeleteVendor_givenVendorExists_asAdmin_thenReturnsNoContent() throws Exception {
        Long vendorId = 1L;
        // Mock service layer (which now handles role check)
        when(vendorService.deleteVendor(vendorId)).thenReturn(true);

        mockMvc.perform(delete("/api/vendors/{id}", vendorId)
                        .with(csrf()))
                .andExpect(status().isNoContent()); // Admin should succeed (204)

        // Verify service method was called
        verify(vendorService).deleteVendor(vendorId);
    }

    @Test
    @WithMockUser // Simulate default USER
    void whenDeleteVendor_givenVendorExists_asUser_thenReturnsForbidden() throws Exception {
        Long vendorId = 1L;
        // Mocking the service to throw AccessDeniedException when called by USER
        // This simulates the @PreAuthorize check failing
        when(vendorService.deleteVendor(vendorId)).thenThrow(new AccessDeniedException("Access Denied"));

        mockMvc.perform(delete("/api/vendors/{id}", vendorId)
                        .with(csrf()))
                .andExpect(status().isForbidden()); // User should get 403 Forbidden

        // Verify service method was called (security might block before/after depending on proxy)
        verify(vendorService).deleteVendor(vendorId);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"}) // Simulate ADMIN user
    void whenDeleteVendor_givenVendorDoesNotExist_asAdmin_thenReturnsNotFound() throws Exception {
        Long vendorId = 99L;
        // Mock service layer returning false (not found for tenant)
        when(vendorService.deleteVendor(vendorId)).thenReturn(false);

        mockMvc.perform(delete("/api/vendors/{id}", vendorId)
                        .with(csrf()))
                .andExpect(status().isNotFound()); // Admin gets 404 if not found

        verify(vendorService).deleteVendor(vendorId);
    }


    @Test
    @WithMockUser // Still need authentication for validation endpoint
    void whenCreateVendor_withInvalidData_thenReturnsBadRequest() throws Exception {
        Vendor invalidVendor = new Vendor();
        invalidVendor.setName(""); // Invalid name (blank)
        invalidVendor.setSpecialty("Plumbing");

        // No service mock needed as validation happens first

        mockMvc.perform(post("/api/vendors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidVendor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Vendor name is mandatory")); // Check validation error message
    }
}

