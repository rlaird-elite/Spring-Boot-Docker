package com.example.demo.vendor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class VendorControllerTest {

    private MockMvc mockMvc;

    // --- Mocks the Service, not the Repository ---
    @Mock
    private VendorService vendorService;

    @InjectMocks
    private VendorController vendorController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(vendorController).build();
    }

    @Test
    void whenCreateVendor_thenReturnsCreatedVendor() throws Exception {
        Vendor vendor = new Vendor();
        vendor.setName("Vendor Name");
        vendor.setServiceType("Electrical");
        vendor.setPhoneNumber("1234");
        vendor.setTrade("Electrician");

        // --- Mock the service method ---
        when(vendorService.createVendor(any(Vendor.class))).thenReturn(vendor);

        mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendor)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Vendor Name"))
                .andExpect(jsonPath("$.serviceType").value("Electrical"))
                .andExpect(jsonPath("$.phoneNumber").value("1234"));
    }

    @Test
    void whenGetAllVendors_thenReturnsVendorList() throws Exception {
        Vendor prop1 = new Vendor();
        prop1.setName("First Vendor");

        Vendor prop2 = new Vendor();
        prop2.setName("Second Vendor");

        // --- Mock the service method ---
        when(vendorService.getAllVendors()).thenReturn(List.of(prop1, prop2));

        mockMvc.perform(get("/api/vendors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("First Vendor"))
                .andExpect(jsonPath("$[1].name").value("Second Vendor"));
    }

    @Test
    void whenGetVendorById_givenVendorExists_thenReturnsVendor() throws Exception {
        Vendor vendor = new Vendor();
        vendor.setId(1L);
        vendor.setName("Vendor Name");
        vendor.setServiceType("Electrical");
        vendor.setTrade("Electrician");

        // --- Mock the service method ---
        when(vendorService.getVendorById(1L)).thenReturn(Optional.of(vendor));

        mockMvc.perform(get("/api/vendors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Vendor Name"))
                .andExpect(jsonPath("$.serviceType").value("Electrical"));
    }

    @Test
    void whenGetVendorById_givenVendorDoesNotExist_thenReturnsNotFound() throws Exception {
        // --- Mock the service method ---
        when(vendorService.getVendorById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/vendors/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenUpdateVendor_givenVendorExists_thenReturnsUpdatedVendor() throws Exception {
        Vendor updatedInfo = new Vendor();
        updatedInfo.setName("Updated Vendor");
        updatedInfo.setServiceType("Carpentry");
        updatedInfo.setPhoneNumber("1234");
        updatedInfo.setTrade("Carpentry");

        Vendor savedVendor = new Vendor();
        savedVendor.setId(1L);
        savedVendor.setName("Updated Vendor");
        savedVendor.setServiceType("Carpentry");
        savedVendor.setPhoneNumber("1234");
        savedVendor.setTrade("Carpentry");

        // --- Mock the service method ---
        when(vendorService.updateVendor(eq(1L), any(Vendor.class))).thenReturn(Optional.of(savedVendor));

        mockMvc.perform(put("/api/vendors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Vendor"))
                .andExpect(jsonPath("$.serviceType").value("Carpentry"))
                .andExpect(jsonPath("$.phoneNumber").value("1234"))
                .andExpect(jsonPath("$.trade").value("Carpentry"));
    }

    @Test
    void whenUpdateVendor_givenVendorDoesNotExist_thenReturnsNotFound() throws Exception {
        Vendor updatedInfo = new Vendor();
        updatedInfo.setName("Vendor Name");

        // --- Mock the service method ---
        when(vendorService.updateVendor(eq(99L), any(Vendor.class))).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/vendors/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedInfo)))
                .andExpect(status().is(400));
    }

    @Test
    void whenDeleteVendor_givenVendorExists_thenReturnsNoContent() throws Exception {
        // --- Mock the service method ---
        when(vendorService.deleteVendor(1L)).thenReturn(true); // Service returns true if successful

        mockMvc.perform(delete("/api/vendors/1"))
                .andExpect(status().isNoContent());

        verify(vendorService).deleteVendor(1L);
    }

    @Test
    void whenDeleteVendor_givenVendorDoesNotExist_thenReturnsNotFound() throws Exception {
        // --- Mock the service method ---
        when(vendorService.deleteVendor(99L)).thenReturn(false); // Service returns false if not found

        mockMvc.perform(delete("/api/vendors/99"))
                .andExpect(status().isNotFound());
    }

}

