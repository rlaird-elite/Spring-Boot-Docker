package com.example.demo.workorder;

import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.permission.Permission; // Import Permission
import com.example.demo.property.Property;
import com.example.demo.user.User;
import com.example.demo.vendor.Vendor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@WebMvcTest(WorkOrderController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
public class WorkOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkOrderService workOrderService;
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
    private Permission mockAdminDeleteWorkOrderPermission;

    @BeforeEach
    void setupUserDetails() {
        // --- Setup mock Permissions ---
        mockUserPermission = new Permission("PERMISSION_READ_OWN_DATA");
        mockUserPermission.setId(100L);
        mockAdminDeleteWorkOrderPermission = new Permission("PERMISSION_DELETE_WORK_ORDER");
        mockAdminDeleteWorkOrderPermission.setId(102L);

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
        adminPermissions.add(mockAdminDeleteWorkOrderPermission);
        mockAdminDetails.setPermissions(adminPermissions);
        when(customUserDetailsService.loadUserByUsername("admin")).thenReturn(mockAdminDetails);
    }

    // Test for Unauthenticated Access
    @Test
    void whenGetAllWorkOrders_withoutAuthentication_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/workorders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser // Simulate default USER
    void whenCreateWorkOrder_thenReturnsCreatedWorkOrder() throws Exception {
        Long propertyId = 10L;
        Long vendorId = 20L;
        WorkOrder requestBody = new WorkOrder();
        requestBody.setDescription("Fix leaky faucet");

        Property mockProperty = new Property(); mockProperty.setId(propertyId);
        Vendor mockVendor = new Vendor(); mockVendor.setId(vendorId);

        WorkOrder savedWorkOrder = new WorkOrder();
        savedWorkOrder.setId(1L);
        savedWorkOrder.setDescription("Fix leaky faucet");
        savedWorkOrder.setStatus("PENDING");
        savedWorkOrder.setProperty(mockProperty);
        savedWorkOrder.setVendor(mockVendor);
        savedWorkOrder.setTenantId(MOCK_TENANT_ID);

        when(workOrderService.createWorkOrder(any(WorkOrder.class), eq(propertyId), eq(vendorId)))
                .thenReturn(savedWorkOrder);

        mockMvc.perform(post("/api/workorders")
                        .param("propertyId", String.valueOf(propertyId))
                        .param("vendorId", String.valueOf(vendorId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.tenantId").value(MOCK_TENANT_ID));
        // Add more assertions as needed
    }


    @Test
    @WithMockUser // Simulate default USER
    void whenGetAllWorkOrders_thenReturnsWorkOrderList() throws Exception {
        WorkOrder wo1 = new WorkOrder(); wo1.setId(1L); wo1.setTenantId(MOCK_TENANT_ID); wo1.setDescription("Task 1");
        WorkOrder wo2 = new WorkOrder(); wo2.setId(2L); wo2.setTenantId(MOCK_TENANT_ID); wo2.setDescription("Task 2");

        when(workOrderService.getAllWorkOrders()).thenReturn(List.of(wo1, wo2));

        mockMvc.perform(get("/api/workorders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        // Add more assertions
    }

    @Test
    @WithMockUser // Simulate default USER
    void whenGetWorkOrderById_givenWorkOrderExists_thenReturnsWorkOrder() throws Exception {
        Long workOrderId = 1L;
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(workOrderId);
        workOrder.setTenantId(MOCK_TENANT_ID);

        when(workOrderService.getWorkOrderById(workOrderId)).thenReturn(Optional.of(workOrder));

        mockMvc.perform(get("/api/workorders/{id}", workOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workOrderId))
                .andExpect(jsonPath("$.tenantId").value(MOCK_TENANT_ID));
    }

    @Test
    @WithMockUser // Simulate default USER
    void whenGetWorkOrderById_givenWorkOrderDoesNotExistOrDifferentTenant_thenReturnsNotFound() throws Exception {
        Long workOrderId = 99L;
        when(workOrderService.getWorkOrderById(workOrderId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/workorders/{id}", workOrderId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser // Simulate default USER
    void whenUpdateWorkOrder_givenWorkOrderExists_thenReturnsUpdatedWorkOrder() throws Exception {
        Long workOrderId = 1L;
        Long newPropertyId = 11L;
        Long newVendorId = 21L;

        WorkOrder requestBody = new WorkOrder();
        requestBody.setDescription("Updated Description");
        requestBody.setStatus("IN_PROGRESS");

        Property mockProperty = new Property(); mockProperty.setId(newPropertyId);
        Vendor mockVendor = new Vendor(); mockVendor.setId(newVendorId);

        WorkOrder updatedWorkOrder = new WorkOrder();
        updatedWorkOrder.setId(workOrderId);
        updatedWorkOrder.setDescription("Updated Description");
        updatedWorkOrder.setStatus("IN_PROGRESS");
        updatedWorkOrder.setProperty(mockProperty);
        updatedWorkOrder.setVendor(mockVendor);
        updatedWorkOrder.setTenantId(MOCK_TENANT_ID);

        when(workOrderService.updateWorkOrder(eq(workOrderId), any(WorkOrder.class), eq(newPropertyId), eq(newVendorId)))
                .thenReturn(Optional.of(updatedWorkOrder));

        mockMvc.perform(put("/api/workorders/{id}", workOrderId)
                        .param("propertyId", String.valueOf(newPropertyId))
                        .param("vendorId", String.valueOf(newVendorId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workOrderId))
                .andExpect(jsonPath("$.tenantId").value(MOCK_TENANT_ID));
        // Add more assertions
    }

    @Test
    @WithMockUser // Simulate default USER
    void whenUpdateWorkOrderStatus_givenWorkOrderExists_thenReturnsUpdatedWorkOrder() throws Exception {
        Long workOrderId = 1L;
        String newStatus = "COMPLETE";

        WorkOrder updatedWorkOrder = new WorkOrder();
        updatedWorkOrder.setId(workOrderId);
        updatedWorkOrder.setStatus(newStatus);
        updatedWorkOrder.setTenantId(MOCK_TENANT_ID);

        when(workOrderService.updateWorkOrderStatus(eq(workOrderId), eq(newStatus)))
                .thenReturn(Optional.of(updatedWorkOrder));

        mockMvc.perform(put("/api/workorders/{id}/status", workOrderId)
                        .param("status", newStatus) // Send status as request parameter
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workOrderId))
                .andExpect(jsonPath("$.status").value(newStatus))
                .andExpect(jsonPath("$.tenantId").value(MOCK_TENANT_ID));
    }

    // --- Tests for Delete Authorization ---
    @Test
    @WithMockUser(username = "admin", authorities = {"PERMISSION_DELETE_WORK_ORDER"}) // Simulate ADMIN with permission
    void whenDeleteWorkOrder_givenWorkOrderExists_asAdmin_thenReturnsNoContent() throws Exception {
        Long workOrderId = 1L;
        when(workOrderService.deleteWorkOrder(workOrderId)).thenReturn(true);

        mockMvc.perform(delete("/api/workorders/{id}", workOrderId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(workOrderService).deleteWorkOrder(workOrderId);
    }

    @Test
    @WithMockUser // Simulate default USER (lacks delete permission)
    void whenDeleteWorkOrder_givenWorkOrderExists_asUser_thenReturnsForbidden() throws Exception {
        Long workOrderId = 1L;
        // Mock service to throw exception WHEN it is called
        when(workOrderService.deleteWorkOrder(workOrderId)).thenThrow(new AccessDeniedException("Access Denied - Should not be called"));

        mockMvc.perform(delete("/api/workorders/{id}", workOrderId)
                        .with(csrf()))
                .andExpect(status().isForbidden()); // User should get 403 Forbidden

        // --- FIX: Verify the service *was* called, as the controller delegates ---
        verify(workOrderService).deleteWorkOrder(workOrderId);
        // --- END FIX ---
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"PERMISSION_DELETE_WORK_ORDER"}) // Simulate ADMIN
    void whenDeleteWorkOrder_givenWorkOrderDoesNotExist_asAdmin_thenReturnsNotFound() throws Exception {
        Long workOrderId = 99L;
        when(workOrderService.deleteWorkOrder(workOrderId)).thenReturn(false);

        mockMvc.perform(delete("/api/workorders/{id}", workOrderId)
                        .with(csrf()))
                .andExpect(status().isNotFound()); // Admin gets 404 if not found

        verify(workOrderService).deleteWorkOrder(workOrderId);
    }

    @Test
    @WithMockUser // Still need authentication for validation endpoint
    void whenCreateWorkOrder_withInvalidData_thenReturnsBadRequest() throws Exception {
        Long propertyId = 10L;
        WorkOrder invalidWorkOrder = new WorkOrder();
        invalidWorkOrder.setDescription(""); // Invalid description

        // No service mock needed

        mockMvc.perform(post("/api/workorders")
                        .param("propertyId", String.valueOf(propertyId)) // Property ID is still required
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidWorkOrder)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.description").value("Description is mandatory")); // Check validation error
    }
}

