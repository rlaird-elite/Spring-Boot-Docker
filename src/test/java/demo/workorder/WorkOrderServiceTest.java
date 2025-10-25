package com.example.demo.workorder;

import com.example.demo.property.Property;
import com.example.demo.property.PropertyRepository;
import com.example.demo.user.User;
import com.example.demo.vendor.Vendor;
import com.example.demo.vendor.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

public class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private PropertyRepository propertyRepository; // Mock new dependency
    @Mock
    private VendorRepository vendorRepository;     // Mock new dependency

    @InjectMocks
    private WorkOrderService workOrderService;

    // Define mock user and tenant ID
    private static final Long MOCK_TENANT_ID = 1L;
    private User mockUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // --- Mock Security Context ---
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("test@example.com");
        mockUser.setTenantId(MOCK_TENANT_ID);
        mockUser.setRole(User.Role.USER);

        Authentication authentication = new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        // --- End Mock Security Context ---
    }

    @Test
    void whenGetAllWorkOrders_thenReturnsTenantWorkOrders() {
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
    void whenGetWorkOrderById_givenValidIdAndTenant_thenReturnsWorkOrder() {
        // Arrange
        Long workOrderId = 1L;
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(workOrderId);
        workOrder.setTenantId(MOCK_TENANT_ID);
        when(workOrderRepository.findByIdAndTenantId(workOrderId, MOCK_TENANT_ID)).thenReturn(Optional.of(workOrder));

        // Act
        Optional<WorkOrder> foundWorkOrder = workOrderService.getWorkOrderById(workOrderId);

        // Assert
        assertTrue(foundWorkOrder.isPresent());
        assertEquals(workOrderId, foundWorkOrder.get().getId());
        verify(workOrderRepository).findByIdAndTenantId(workOrderId, MOCK_TENANT_ID);
    }

    @Test
    void whenGetWorkOrderById_givenInvalidTenant_thenReturnsEmpty() {
        // Arrange
        Long workOrderId = 1L;
        when(workOrderRepository.findByIdAndTenantId(workOrderId, MOCK_TENANT_ID)).thenReturn(Optional.empty());

        // Act
        Optional<WorkOrder> foundWorkOrder = workOrderService.getWorkOrderById(workOrderId);

        // Assert
        assertFalse(foundWorkOrder.isPresent());
        verify(workOrderRepository).findByIdAndTenantId(workOrderId, MOCK_TENANT_ID);
    }

    @Test
    void whenCreateWorkOrder_givenValidPropertyAndOptionalVendor_thenSetsTenantIdAndSaves() {
        // Arrange
        Long propertyId = 10L;
        Long vendorId = 20L;
        WorkOrder workOrderToSave = new WorkOrder(); // Input doesn't have tenantId etc. yet
        workOrderToSave.setDescription("New task");

        Property mockProperty = new Property(); mockProperty.setId(propertyId); mockProperty.setTenantId(MOCK_TENANT_ID);
        Vendor mockVendor = new Vendor(); mockVendor.setId(vendorId); mockVendor.setTenantId(MOCK_TENANT_ID);

        when(propertyRepository.findByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(Optional.of(mockProperty));
        when(vendorRepository.findByIdAndTenantId(vendorId, MOCK_TENANT_ID)).thenReturn(Optional.of(mockVendor));

        // Mock the save operation, ensuring tenantId is set before save
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> {
            WorkOrder wo = invocation.getArgument(0);
            assertEquals(MOCK_TENANT_ID, wo.getTenantId()); // Verify tenantId
            assertEquals("PENDING", wo.getStatus());     // Verify status
            assertNotNull(wo.getCreatedAt());           // Verify timestamps
            assertNotNull(wo.getUpdatedAt());
            wo.setId(1L); // Simulate ID generation
            return wo;
        });

        // Act
        WorkOrder result = workOrderService.createWorkOrder(workOrderToSave, propertyId, vendorId);

        // Assert
        assertNotNull(result);
        assertEquals(MOCK_TENANT_ID, result.getTenantId());
        assertEquals(mockProperty, result.getProperty());
        assertEquals(mockVendor, result.getVendor());
        verify(propertyRepository).findByIdAndTenantId(propertyId, MOCK_TENANT_ID);
        verify(vendorRepository).findByIdAndTenantId(vendorId, MOCK_TENANT_ID);
        verify(workOrderRepository).save(workOrderToSave);
    }

    @Test
    void whenCreateWorkOrder_givenValidPropertyNoVendor_thenSetsTenantIdAndSaves() {
        // Arrange
        Long propertyId = 10L;
        WorkOrder workOrderToSave = new WorkOrder();
        workOrderToSave.setDescription("New task");

        Property mockProperty = new Property(); mockProperty.setId(propertyId); mockProperty.setTenantId(MOCK_TENANT_ID);

        when(propertyRepository.findByIdAndTenantId(propertyId, MOCK_TENANT_ID)).thenReturn(Optional.of(mockProperty));
        // No mock needed for vendorRepository as ID is null

        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> {
            WorkOrder wo = invocation.getArgument(0);
            assertEquals(MOCK_TENANT_ID, wo.getTenantId());
            assertNull(wo.getVendor()); // Verify vendor is null
            wo.setId(1L);
            return wo;
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
    void whenCreateWorkOrder_givenInvalidProperty_thenThrowsAccessDenied() {
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
    void whenCreateWorkOrder_givenInvalidVendor_thenThrowsAccessDenied() {
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

    // --- Add Tests for Update Methods ---
    @Test
    void whenUpdateWorkOrder_givenValidData_thenUpdatesAndReturns() {
        // Arrange
        Long workOrderId = 1L;
        Long propertyId = 10L;
        Long vendorId = 20L;
        WorkOrder existingWorkOrder = new WorkOrder(); // Setup existing WO
        existingWorkOrder.setId(workOrderId);
        existingWorkOrder.setTenantId(MOCK_TENANT_ID);
        existingWorkOrder.setDescription("Old Description");

        WorkOrder updatedDetails = new WorkOrder(); // New details from request
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
    void whenUpdateWorkOrderStatus_givenValidIdAndTenant_thenUpdatesStatus() {
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

    // Add tests for update failures (invalid ID, wrong tenant, invalid property/vendor IDs) ...


    // --- Add Tests for Delete Method ---
    @Test
    void whenDeleteWorkOrder_givenValidIdAndTenant_thenDeletesAndReturnsTrue() {
        // Arrange
        Long workOrderId = 1L;
        when(workOrderRepository.existsByIdAndTenantId(workOrderId, MOCK_TENANT_ID)).thenReturn(true);

        // Act
        boolean result = workOrderService.deleteWorkOrder(workOrderId);

        // Assert
        assertTrue(result);
        verify(workOrderRepository).existsByIdAndTenantId(workOrderId, MOCK_TENANT_ID);
        verify(workOrderRepository).deleteById(workOrderId);
    }

    @Test
    void whenDeleteWorkOrder_givenInvalidIdOrTenant_thenReturnsFalse() {
        // Arrange
        Long workOrderId = 99L;
        when(workOrderRepository.existsByIdAndTenantId(workOrderId, MOCK_TENANT_ID)).thenReturn(false);

        // Act
        boolean result = workOrderService.deleteWorkOrder(workOrderId);

        // Assert
        assertFalse(result);
        verify(workOrderRepository).existsByIdAndTenantId(workOrderId, MOCK_TENANT_ID);
        verify(workOrderRepository, never()).deleteById(anyLong());
    }
}

