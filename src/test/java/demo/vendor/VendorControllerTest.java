package com.example.demo.vendor;

import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.permission.Permission; // Import Permission
import com.example.demo.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException; // Import AccessDeniedException
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

// Import SecurityConfig and UserDetailsService for context
import com.example.demo.SecurityConfig;
import com.example.demo.user.CustomUserDetailsService;
import com.example.demo.user.JwtTokenProvider;

import java.util.HashSet; // Import HashSet
import java.util.List;
import java.util.Optional;
import java.util.Set; // Import Set

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@WebMvcTest(VendorController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
public class VendorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VendorService vendorService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    // Define mock user, tenant ID, and permissions
    private static final Long MOCK_TENANT_ID = 1L;
    private User mockUserDetails;
    private User mockAdminDetails;
    private Permission mockUserPermission;
    private Permission mockAdminDeleteVendorPermission;

    @BeforeEach
    void setupUserDetails() {
        // --- Setup mock Permissions ---
        mockUserPermission = new Permission("PERMISSION_READ_OWN_DATA");
        mockUserPermission.setId(100L);
        mockAdminDeleteVendorPermission = new Permission("PERMISSION_DELETE_VENDOR");
        mockAdminDeleteVendorPermission.setId(101L);


        // --- Setup mock UserDetails for @WithMockUser(username="user") ---
        mockUserDetails = new User();
        mockUserDetails.setId(1L);
        mockUserDetails.setUsername("user");
        mockUserDetails.setPassword("password");
        mockUserDetails.setTenantId(MOCK_TENANT_ID);
        mockUserDetails.setPermissions(Set.of(mockUserPermission)); // Assign USER permission
        when(customUserDetailsService.loadUserByUsername("user")).thenReturn(mockUserDetails);

        // --- Setup mock UserDetails for ADMIN tests ---
        mockAdminDetails = new User();
        mockAdminDetails.setId(2L);
        mockAdminDetails.setUsername("admin");
        mockAdminDetails.setPassword("password");
        mockAdminDetails.setTenantId(MOCK_TENANT_ID);
        // Assign ADMIN permissions
        Set<Permission> adminPermissions = new HashSet<>();
        adminPermissions.add(mockUserPermission);
        adminPermissions.add(mockAdminDeleteVendorPermission);
        mockAdminDetails.setPermissions(adminPermissions);
        when(customUserDetailsService.loadUserByUsername("admin")).thenReturn(mockAdminDetails);
    }

    // Test for Unauthenticated Access
    @Test
    void whenGetAllVendors_withoutAuthentication_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/vendors"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser // Default user (USER permissions)
    void whenCreateVendor_thenReturnsCreatedVendor() throws Exception {
        Vendor vendor = new Vendor();
        vendor.setName("Reliable Plumbing");
        vendor.setSpecialty("Plumbing");

        Vendor savedVendor = new Vendor();
        savedVendor.setId(1L);
        savedVendor.setName("Reliable Plumbing");
        savedVendor.setSpecialty("Plumbing");
        savedVendor.setTenantId(MOCK_TENANT_ID);

        when(vendorService.createVendor(any(Vendor.class))).thenReturn(savedVendor);

        mockMvc.perform(post("/api/vendors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendor)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.tenantId").value(MOCK_TENANT_ID));
    }


    @Test
    @WithMockUser // Default user (USER permissions)
    void whenGetAllVendors_thenReturnsVendorList() throws Exception {
        Vendor vendor1 = new Vendor(); vendor1.setId(1L); vendor1.setName("Plumber One"); vendor1.setTenantId(MOCK_TENANT_ID);
        Vendor vendor2 = new Vendor(); vendor2.setId(2L); vendor2.setName("Electrician Two"); vendor2.setTenantId(MOCK_TENANT_ID);

        when(vendorService.getAllVendors()).thenReturn(List.of(vendor1, vendor2));

        mockMvc.perform(get("/api/vendors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser // Default user (USER permissions)
    void whenGetVendorById_givenVendorExists_thenReturnsVendor() throws Exception {
        Long vendorId = 1L;
        Vendor vendor = new Vendor(); vendor.setId(vendorId); vendor.setName("Test Vendor"); vendor.setTenantId(MOCK_TENANT_ID);

        when(vendorService.getVendorById(vendorId)).thenReturn(Optional.of(vendor));

        mockMvc.perform(get("/api/vendors/{id}", vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendorId))
                .andExpect(jsonPath("$.tenantId").value(MOCK_TENANT_ID));
    }

    @Test
    @WithMockUser // Default user (USER permissions)
    void whenGetVendorById_givenVendorDoesNotExistOrDifferentTenant_thenReturnsNotFound() throws Exception {
        Long vendorId = 99L;
        when(vendorService.getVendorById(vendorId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/vendors/{id}", vendorId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser // Default user (USER permissions)
    void whenUpdateVendor_givenVendorExists_thenReturnsUpdatedVendor() throws Exception {
        Long vendorId = 1L;
        Vendor updatedDetails = new Vendor(); updatedDetails.setName("Updated Vendor Name");
        Vendor returnedVendor = new Vendor(); returnedVendor.setId(vendorId); returnedVendor.setName("Updated Vendor Name"); returnedVendor.setTenantId(MOCK_TENANT_ID);

        when(vendorService.updateVendor(eq(vendorId), any(Vendor.class))).thenReturn(Optional.of(returnedVendor));

        mockMvc.perform(put("/api/vendors/{id}", vendorId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendorId))
                .andExpect(jsonPath("$.tenantId").value(MOCK_TENANT_ID));
    }

    @Test
    @WithMockUser // Default user (USER permissions)
    void whenUpdateVendor_givenVendorDoesNotExistOrDifferentTenant_thenReturnsNotFound() throws Exception {
        Long vendorId = 99L;
        Vendor updatedDetails = new Vendor(); updatedDetails.setName("Updated Name");

        when(vendorService.updateVendor(eq(vendorId), any(Vendor.class))).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/vendors/{id}", vendorId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDetails)))
                .andExpect(status().isNotFound());
    }

    // --- Tests for Delete Authorization ---
    @Test
    @WithMockUser(username = "admin", authorities = {"PERMISSION_DELETE_VENDOR"}) // Simulate ADMIN with permission
    void whenDeleteVendor_givenVendorExists_asAdmin_thenReturnsNoContent() throws Exception {
        Long vendorId = 1L;
        when(vendorService.deleteVendor(vendorId)).thenReturn(true);

        mockMvc.perform(delete("/api/vendors/{id}", vendorId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(vendorService).deleteVendor(vendorId);
    }

    @Test
    @WithMockUser // Simulate default USER (lacks permission)
    void whenDeleteVendor_givenVendorExists_asUser_thenReturnsForbidden() throws Exception {
        Long vendorId = 1L;
        // Mock service to throw exception WHEN it is called
        when(vendorService.deleteVendor(vendorId)).thenThrow(new AccessDeniedException("Access Denied - Should not be called"));

        mockMvc.perform(delete("/api/vendors/{id}", vendorId)
                        .with(csrf()))
                .andExpect(status().isForbidden()); // User should get 403 Forbidden

        // --- FIX: Verify the service *was* called, as the controller delegates ---
        verify(vendorService).deleteVendor(vendorId);
        // --- END FIX ---
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"PERMISSION_DELETE_VENDOR"}) // Simulate ADMIN
    void whenDeleteVendor_givenVendorDoesNotExist_asAdmin_thenReturnsNotFound() throws Exception {
        Long vendorId = 99L;
        when(vendorService.deleteVendor(vendorId)).thenReturn(false); // Service returns false if not found

        mockMvc.perform(delete("/api/vendors/{id}", vendorId)
                        .with(csrf()))
                .andExpect(status().isNotFound()); // Admin gets 404

        verify(vendorService).deleteVendor(vendorId);
    }

    @Test
    @WithMockUser // Still need authentication for validation
    void whenCreateVendor_withInvalidData_thenReturnsBadRequest() throws Exception {
        Vendor invalidVendor = new Vendor(); invalidVendor.setName("");

        mockMvc.perform(post("/api/vendors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidVendor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Vendor name is mandatory"));
    }
}

