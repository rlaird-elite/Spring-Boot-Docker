package com.example.demo.admin;

import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.permission.Permission;
import com.example.demo.user.CustomUserDetailsService;
import com.example.demo.user.JwtTokenProvider;
import com.example.demo.user.User;
import com.example.demo.user.UpdateUserPermissionsRequest;
import com.example.demo.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class) // Target the (not-yet-existing) AdminController
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService; // Mock the (not-yet-existing) AdminService

    // --- Mock security dependencies ---
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private static final Long MOCK_TENANT_ID = 1L;
    private User mockAdminUser;
    private Permission manageUsersPermission;

    @BeforeEach
    void setupUserDetails() {
        manageUsersPermission = new Permission("PERMISSION_MANAGE_USERS");
        manageUsersPermission.setId(101L);

        mockAdminUser = new User();
        mockAdminUser.setId(1L);
        mockAdminUser.setUsername("admin@example.com");
        mockAdminUser.setTenantId(MOCK_TENANT_ID);
        mockAdminUser.setPermissions(Set.of(manageUsersPermission));

        // Mock the UserDetailsService to load our admin user
        when(customUserDetailsService.loadUserByUsername("admin")).thenReturn(mockAdminUser);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"PERMISSION_MANAGE_USERS"})
    void whenGetUsersInTenant_asAdmin_thenReturnsUserList() throws Exception {
        // Arrange
        User user1 = new User();
        user1.setId(10L);
        user1.setUsername("user1@example.com");
        user1.setTenantId(MOCK_TENANT_ID);

        when(adminService.listUsersInTenant()).thenReturn(List.of(user1));

        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                        .with(csrf())) // Add CSRF for safety, though GETs might not need it
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("user1@example.com"));
    }

    @Test
    @WithMockUser // Simulate a regular user *without* the permission
    void whenGetUsersInTenant_asUser_thenReturnsForbidden() throws Exception {
        // Arrange (no mock for customUserDetailsService.loadUserByUsername("user") needed)
        // The @WithMockUser annotation creates a principal, but it won't have the authority.
        // The @PreAuthorize check will fail.

        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"PERMISSION_MANAGE_USERS"})
    void whenUpdateUserPermissions_asAdmin_thenReturnsUpdatedUser() throws Exception {
        // Arrange
        Long userIdToUpdate = 10L;
        Set<String> newPermissions = Set.of("PERMISSION_READ_OWN_DATA", "PERMISSION_DELETE_VENDOR");
        UpdateUserPermissionsRequest requestBody = new UpdateUserPermissionsRequest(newPermissions);

        User updatedUser = new User();
        updatedUser.setId(userIdToUpdate);
        updatedUser.setUsername("user1@example.com");
        updatedUser.setTenantId(MOCK_TENANT_ID);
        // Simulate the service returning the user with the new permissions
        Set<Permission> permissionObjects = new HashSet<>();
        permissionObjects.add(new Permission("PERMISSION_READ_OWN_DATA"));
        permissionObjects.add(new Permission("PERMISSION_DELETE_VENDOR"));
        updatedUser.setPermissions(permissionObjects);


        when(adminService.updateUserPermissions(eq(userIdToUpdate), eq(newPermissions)))
                .thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(put("/api/admin/users/{userId}/permissions", userIdToUpdate)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userIdToUpdate))
                .andExpect(jsonPath("$.permissions.length()").value(2))
                .andExpect(jsonPath("$.permissions[?(@.name == 'PERMISSION_DELETE_VENDOR')]").exists());
    }
}
