package com.example.demo.workorder;

import com.example.demo.property.Property;
import com.example.demo.property.PropertyRepository;
import com.example.demo.user.User;
import com.example.demo.vendor.Vendor;
import com.example.demo.vendor.VendorRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

// --- Use @SpringBootTest to load context and enable AOP/Security ---
@SpringBootTest // Loads the full application context
public class WorkOrderServiceTest {

    @MockBean // Use @MockBean for dependencies when using @SpringBootTest
    private WorkOrderRepository workOrderRepository;
    @MockBean // Mock new dependency
    private PropertyRepository propertyRepository;
    @MockBean // Mock new dependency
    private VendorRepository vendorRepository;

    @Autowired // Inject the actual service bean from the Spring context
    private WorkOrderService workOrderService;

    // Define mock user and tenant ID
    private static final Long MOCK_TENANT_ID = 1L;

    // Helper to set up security context
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

    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext(); // Clean up security context after each test
    }


    @Test
    void whenGetAllWorkOrders_asUser_thenReturnsTenantWorkOrders() {
        setupMockSecurityContext(User.Role.USER); // Set context for USER
        // Arrange
        WorkOrder wo1 = new WorkOrder(); wo1.setTenantId(MOCK_TENANT_ID);
        WorkOrder wo2 = new WorkOrder(); wo2.setTenantId(MOCK_TENANT_ID);
        when(workOrderRepository.findAllByTenantId(MOCK_TENANT_ID)).thenReturn(List.of(wo1, wo2));

        // Act
        List<WorkOrder> workOrders = workOrderService.getAllWorkOrders();

        // Assert
        assertEquals(2, workOrders.size());
        verify(workOrderRepository).findAllByTenantId(MOCK_TENANT_ID);
    }

    @Test
    void whenGetWorkOrderById_givenValidIdAndTenant_asUser_thenReturnsWorkOrder() {
        setupMockSecurityContext(User.Role.USER);
        Long workOrderId = 1L;
        WorkOrder workOrder = new WorkOrder(); workOrder.setId(workOrderId); workOrder.setTenantId(MOCK_TENANT_ID);
        when(workOrderRepository.findByIdAndTenantId(workOrderId, MOCK_TENANT_ID)).thenReturn(Optional.of(workOrder));
        Optional<WorkOrder> foundWorkOrder = workOrderService.getWorkOrderById(workOrderId);
        assertTrue(foundWorkOrder.isPresent());
        verify(workOrderRepository).findByIdAndTenantId(workOrderId, MOCK_TENANT_ID);
    }


    @Test
    void whenCreateWorkOrder_givenValidPropertyAndOptionalVendor_asUser_thenSetsTenantIdAndSaves() {
        setupMockSecurityContext(User.Role.USER);
        // Arrange
        Long propertyId = 10L;
        Long vendorId = 20L;
        WorkOrder workOrderToSave = new WorkOrder();
        workOrderToSave.setDescription("New task");

        Property mockProperty = new Property(); mockProperty.setId(propertyId); mockProperty.setTenantId(MOCK_TENANT_ID);
        Vendor mockVendor = new Vendor(); mockVendor.setId(vendorId); mockVendor.setTenantId(MOCK_TENANT_ID);

        when(propertyRepository.findByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(Optional.of(mockProperty));
        when(vendorRepository.findByIdAndTenantId(vendorId, MOCK_TENANT_ID)).thenReturn(Optional.of(mockVendor));

        // Mock the save operation
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> {
            WorkOrder wo = invocation.getArgument(0);
            assertEquals(MOCK_TENANT_ID, wo.getTenantId());
            assertEquals("PENDING", wo.getStatus());
            assertNotNull(wo.getCreatedAt());
            assertNotNull(wo.getUpdatedAt());
            // Simulate saving by returning a copy with an ID
            WorkOrder saved = new WorkOrder();
            saved.setId(1L); // Simulate ID generation
            saved.setDescription(wo.getDescription());
            saved.setStatus(wo.getStatus());
            saved.setTenantId(wo.getTenantId());
            saved.setProperty(wo.getProperty());
            saved.setVendor(wo.getVendor());
            saved.setCreatedAt(wo.getCreatedAt());
            saved.setUpdatedAt(wo.getUpdatedAt());
            return saved;
        });

        // Act
        WorkOrder result = workOrderService.createWorkOrder(workOrderToSave, propertyId, vendorId);

        // Assert
        assertNotNull(result);
        assertEquals(MOCK_TENANT_ID, result.getTenantId());
        assertEquals(1L, result.getId());
        assertEquals(mockProperty, result.getProperty());
        assertEquals(mockVendor, result.getVendor());
        verify(propertyRepository).findByIdAndTenantId(propertyId, MOCK_TENANT_ID);
        verify(vendorRepository).findByIdAndTenantId(vendorId, MOCK_TENANT_ID);
        verify(workOrderRepository).save(workOrderToSave);
    }

    @Test
    void whenCreateWorkOrder_givenValidPropertyNoVendor_asUser_thenSetsTenantIdAndSaves() {
        setupMockSecurityContext(User.Role.USER);
        // Arrange
        Long propertyId = 10L;
        WorkOrder workOrderToSave = new WorkOrder();
        workOrderToSave.setDescription("New task");

        Property mockProperty = new Property(); mockProperty.setId(propertyId); mockProperty.setTenantId(MOCK_TENANT_ID);

        when(propertyRepository.findByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(Optional.of(mockProperty));

        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> {
            WorkOrder wo = invocation.getArgument(0);
            assertEquals(MOCK_TENANT_ID, wo.getTenantId());
            assertNull(wo.getVendor()); // Verify vendor is null
            // Simulate saving
            WorkOrder saved = new WorkOrder();
            saved.setId(1L);
            saved.setDescription(wo.getDescription());
            saved.setStatus(wo.getStatus());
            saved.setTenantId(wo.getTenantId());
            saved.setProperty(wo.getProperty());
            saved.setCreatedAt(wo.getCreatedAt());
            saved.setUpdatedAt(wo.getUpdatedAt());
            return saved;
        });

        // Act
        WorkOrder result = workOrderService.createWorkOrder(workOrderToSave, propertyId, null); // Pass null for vendorId

        // Assert
        assertNotNull(result);
        assertEquals(MOCK_TENANT_ID, result.getTenantId());
        assertEquals(mockProperty, result.getProperty());
        assertNull(result.getVendor());
        verify(propertyRepository).findByIdAndTenantId(propertyId, MOCK_TENANT_ID);
        verify(vendorRepository, never()).findByIdAndTenantId(anyLong(), anyLong()); // Verify vendor repo not called
        verify(workOrderRepository).save(workOrderToSave);
    }


    @Test
    void whenCreateWorkOrder_givenInvalidProperty_asUser_thenThrowsAccessDenied() {
        setupMockSecurityContext(User.Role.USER);
        // Arrange
        Long propertyId = 99L; // Non-existent or wrong tenant
        WorkOrder workOrderToSave = new WorkOrder();
        when(propertyRepository.findByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> {
            workOrderService.createWorkOrder(workOrderToSave, propertyId, null);
        });
        verify(propertyRepository).findByIdAndTenantId(propertyId, MOCK_TENANT_ID);
        verify(workOrderRepository, never()).save(any(WorkOrder.class));
    }

    @Test
    void whenCreateWorkOrder_givenInvalidVendor_asUser_thenThrowsAccessDenied() {
        setupMockSecurityContext(User.Role.USER);
        // Arrange
        Long propertyId = 10L;
        Long vendorId = 99L; // Non-existent or wrong tenant
        WorkOrder workOrderToSave = new WorkOrder();
        Property mockProperty = new Property(); mockProperty.setId(propertyId); mockProperty.setTenantId(MOCK_TENANT_ID);

        when(propertyRepository.findByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(Optional.of(mockProperty));
        when(vendorRepository.findByIdAndTenantId(vendorId, MOCK_TENANT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> {
            workOrderService.createWorkOrder(workOrderToSave, propertyId, vendorId);
        });
        verify(propertyRepository).findByIdAndTenantId(propertyId, MOCK_TENANT_ID);
        verify(vendorRepository).findByIdAndTenantId(vendorId, MOCK_TENANT_ID);
        verify(workOrderRepository, never()).save(any(WorkOrder.class));
    }

    @Test
    void whenUpdateWorkOrder_givenValidData_asUser_thenUpdatesAndReturns() {
        setupMockSecurityContext(User.Role.USER);
        // Arrange
        Long workOrderId = 1L;
        Long propertyId = 10L;
        Long vendorId = 20L;
        WorkOrder existingWorkOrder = new WorkOrder();
        existingWorkOrder.setId(workOrderId);
        existingWorkOrder.setTenantId(MOCK_TENANT_ID);
        existingWorkOrder.setDescription("Old Description");

        WorkOrder updatedDetails = new WorkOrder();
        updatedDetails.setDescription("New Description");
        updatedDetails.setStatus("COMPLETE");

        Property newProperty = new Property(); newProperty.setId(propertyId); newProperty.setTenantId(MOCK_TENANT_ID);
        Vendor newVendor = new Vendor(); newVendor.setId(vendorId); newVendor.setTenantId(MOCK_TENANT_ID);

        when(workOrderRepository.findByIdAndTenantId(workOrderId, MOCK_TENANT_ID)).thenReturn(Optional.of(existingWorkOrder));
        when(propertyRepository.findByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(Optional.of(newProperty));
        when(vendorRepository.findByIdAndTenantId(vendorId, MOCK_TENANT_ID)).thenReturn(Optional.of(newVendor));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<WorkOrder> result = workOrderService.updateWorkOrder(workOrderId, updatedDetails, propertyId, vendorId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("New Description", result.get().getDescription());
        assertEquals("COMPLETE", result.get().getStatus());
        assertEquals(newProperty, result.get().getProperty());
        assertEquals(newVendor, result.get().getVendor());
        verify(workOrderRepository).findByIdAndTenantId(workOrderId, MOCK_TENANT_ID);
        verify(propertyRepository).findByIdAndTenantId(propertyId, MOCK_TENANT_ID);
        verify(vendorRepository).findByIdAndTenantId(vendorId, MOCK_TENANT_ID);
        verify(workOrderRepository).save(existingWorkOrder);
    }

    @Test
    void whenUpdateWorkOrderStatus_givenValidIdAndTenant_asUser_thenUpdatesStatus() {
        setupMockSecurityContext(User.Role.USER);
        // Arrange
        Long workOrderId = 1L;
        String newStatus = "COMPLETE";
        WorkOrder existingWorkOrder = new WorkOrder();
        existingWorkOrder.setId(workOrderId);
        existingWorkOrder.setTenantId(MOCK_TENANT_ID);
        existingWorkOrder.setStatus("PENDING");

        when(workOrderRepository.findByIdAndTenantId(workOrderId, MOCK_TENANT_ID)).thenReturn(Optional.of(existingWorkOrder));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<WorkOrder> result = workOrderService.updateWorkOrderStatus(workOrderId, newStatus);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(newStatus, result.get().getStatus());
        verify(workOrderRepository).findByIdAndTenantId(workOrderId, MOCK_TENANT_ID);
        verify(workOrderRepository).save(existingWorkOrder);
    }

    // --- NEW: Tests for Delete Authorization ---

    @Test
    void whenDeleteWorkOrder_givenValidIdAndTenant_asAdmin_thenDeletesAndReturnsTrue() {
        setupMockSecurityContext(User.Role.ADMIN); // Set context for ADMIN
        // Arrange
        Long workOrderId = 1L;
        when(workOrderRepository.existsByIdAndTenantId(workOrderId, MOCK_TENANT_ID)).thenReturn(true);
        // No need to explicitly mock deleteById if it returns void

        // Act & Assert
        assertDoesNotThrow(() -> { // Expect no security exception
            boolean result = workOrderService.deleteWorkOrder(workOrderId);
            assertTrue(result);
            verify(workOrderRepository).existsByIdAndTenantId(workOrderId, MOCK_TENANT_ID);
            verify(workOrderRepository).deleteById(workOrderId);
        });
    }

    @Test
    void whenDeleteWorkOrder_givenValidIdAndTenant_asUser_thenThrowsAccessDenied() {
        setupMockSecurityContext(User.Role.USER); // Set context for USER
        // Arrange
        Long workOrderId = 1L;
        // Mock existence check - delete should not be reached due to security
        when(workOrderRepository.existsByIdAndTenantId(workOrderId, MOCK_TENANT_ID)).thenReturn(true);

        // Act & Assert
        // Expect AccessDeniedException because @PreAuthorize is now active
        assertThrows(AccessDeniedException.class, () -> {
            workOrderService.deleteWorkOrder(workOrderId);
        }, "Should throw AccessDeniedException for USER role trying to delete");

        // Verify delete was NOT called because security blocked it
        verify(workOrderRepository, never()).deleteById(anyLong());
    }

    @Test
    void whenDeleteWorkOrder_givenInvalidIdOrTenant_asAdmin_thenReturnsFalse() {
        setupMockSecurityContext(User.Role.ADMIN); // Set context for ADMIN
        // Arrange
        Long workOrderId = 99L;
        when(workOrderRepository.existsByIdAndTenantId(workOrderId, MOCK_TENANT_ID)).thenReturn(false);

        // Act & Assert
        assertDoesNotThrow(() -> { // No security exception expected for ADMIN
            boolean result = workOrderService.deleteWorkOrder(workOrderId);
            assertFalse(result); // Returns false because work order doesn't exist for tenant
            verify(workOrderRepository).existsByIdAndTenantId(workOrderId, MOCK_TENANT_ID);
            verify(workOrderRepository, never()).deleteById(anyLong());
        });
    }

    @Test
    void whenDeleteWorkOrder_givenInvalidIdOrTenant_asUser_thenThrowsAccessDenied() {
        setupMockSecurityContext(User.Role.USER); // Set context for USER
        // Arrange
        Long workOrderId = 99L;
        // No need to mock existsByIdAndTenantId if security prevents call

        // Act & Assert
        // @PreAuthorize runs before method body, so AccessDenied should happen first
        assertThrows(AccessDeniedException.class, () -> {
            workOrderService.deleteWorkOrder(workOrderId);
        }, "Should throw AccessDeniedException for USER role even if work order doesn't exist");

        // Verify delete was NOT called
        verify(workOrderRepository, never()).deleteById(anyLong());
        // Verify existence check might not be called either
        verify(workOrderRepository, never()).existsByIdAndTenantId(anyLong(), anyLong());
    }
}

