package com.example.demo.workorder;

import com.example.demo.property.Property;
import com.example.demo.property.PropertyRepository;
import com.example.demo.vendor.Vendor;
import com.example.demo.vendor.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private VendorRepository vendorRepository;

    @InjectMocks
    private WorkOrderService workOrderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void whenCreateWorkOrder_withValidIds_thenReturnsSavedWorkOrder() {
        // --- Mocking Data ---
        Property property = new Property();
        property.setId(1L);

        Vendor vendor = new Vendor();
        vendor.setId(2L);

        WorkOrder request = new WorkOrder();
        request.setDescription("Test description");
        // Note: request does NOT have a status

        // --- Mocking Repositories ---
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(vendorRepository.findById(2L)).thenReturn(Optional.of(vendor));

        // --- FIX: Use ArgumentCaptor to capture the object sent to save() ---
        ArgumentCaptor<WorkOrder> workOrderCaptor = ArgumentCaptor.forClass(WorkOrder.class);
        // We need to return the object that was passed to save(), so we use 'thenAnswer'
        when(workOrderRepository.save(workOrderCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // --- Perform the Test ---
        WorkOrder result = workOrderService.createWorkOrder(request, 1L, 2L);

        // --- Assertions ---
        assertNotNull(result);
        // --- FIX: Verify the service set the status ---
        assertEquals("PENDING", result.getStatus());
        assertEquals(1L, result.getProperty().getId());
        assertEquals(2L, result.getVendor().getId());
        assertEquals("Test description", result.getDescription());

        // Also verify the captor
        assertEquals("PENDING", workOrderCaptor.getValue().getStatus());
    }

    @Test
    void whenCreateWorkOrder_withNullVendorId_thenReturnsSavedWorkOrder() {
        Property property = new Property();
        property.setId(1L);

        WorkOrder request = new WorkOrder();
        request.setDescription("Test description no vendor");

        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        ArgumentCaptor<WorkOrder> workOrderCaptor = ArgumentCaptor.forClass(WorkOrder.class);
        when(workOrderRepository.save(workOrderCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // --- Perform the Test ---
        WorkOrder result = workOrderService.createWorkOrder(request, 1L, null);

        // --- Assertions ---
        assertNotNull(result);
        assertNull(result.getVendor());
        assertEquals("PENDING", result.getStatus()); // Verify status is set
        assertEquals(1L, result.getProperty().getId());
    }

    @Test
    void whenCreateWorkOrder_withInvalidPropertyId_thenThrowsException() {
        WorkOrder request = new WorkOrder();
        request.setDescription("Test");

        when(propertyRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            workOrderService.createWorkOrder(request, 99L, null);
        });

        assertEquals("Property not found with id: 99", exception.getMessage());
    }

    @Test
    void whenCreateWorkOrder_withInvalidVendorId_thenThrowsException() {
        Property property = new Property();
        property.setId(1L);

        WorkOrder request = new WorkOrder();
        request.setDescription("Test");

        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(vendorRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            workOrderService.createWorkOrder(request, 1L, 99L);
        });

        assertEquals("Vendor not found with id: 99", exception.getMessage());
    }
}

