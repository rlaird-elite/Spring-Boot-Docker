package com.example.demo.workorder;

import com.example.demo.property.Property;
import com.example.demo.vendor.Vendor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class WorkOrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WorkOrderService workOrderService;

    @InjectMocks
    private WorkOrderController workOrderController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(workOrderController)
                .build();
    }

    @Test
    void whenCreateWorkOrder_thenReturnsCreatedWorkOrder() throws Exception {
        // --- Mocking Data ---
        Property property = new Property();
        property.setId(1L);

        Vendor vendor = new Vendor();
        vendor.setId(2L);

        // This is the object we will send in the request body
        WorkOrder requestBody = new WorkOrder();
        requestBody.setDescription("Fix leaky faucet");
        // --- FIX: We no longer send the status. The service will set it. ---

        // This is the full object we expect back from the service
        WorkOrder savedWorkOrder = new WorkOrder();
        savedWorkOrder.setId(1L);
        savedWorkOrder.setDescription("Fix leaky faucet");
        savedWorkOrder.setStatus("PENDING"); // Service sets this
        savedWorkOrder.setProperty(property);
        savedWorkOrder.setVendor(vendor);

        // --- Mocking Service ---
        when(workOrderService.createWorkOrder(any(WorkOrder.class), eq(1L), eq(2L)))
                .thenReturn(savedWorkOrder);

        // --- Perform the Test ---
        mockMvc.perform(post("/api/workorders")
                        .param("propertyId", "1")
                        .param("vendorId", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))) // Body no longer has status
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.description").value("Fix leaky faucet"))
                .andExpect(jsonPath("$.status").value("PENDING")) // We verify the service set it
                .andExpect(jsonPath("$.property.id").value(1L))
                .andExpect(jsonPath("$.vendor.id").value(2L));
    }
}

