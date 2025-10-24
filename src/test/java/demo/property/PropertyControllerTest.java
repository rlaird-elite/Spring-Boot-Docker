package com.example.demo.property;

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

public class PropertyControllerTest {

    private MockMvc mockMvc;

    // --- Mocks the Service, not the Repository ---
    @Mock
    private PropertyService propertyService;

    @InjectMocks
    private PropertyController propertyController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(propertyController).build();
    }

    @Test
    void whenCreateProperty_thenReturnsCreatedProperty() throws Exception {
        Property property = new Property();
        property.setAddress("123 Main St");
        property.setType("Single Family");
        property.setBedrooms(3);
        property.setBathrooms(2);

        // --- Mock the service method ---
        when(propertyService.createProperty(any(Property.class))).thenReturn(property);

        mockMvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(property)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.address").value("123 Main St"))
                .andExpect(jsonPath("$.type").value("Single Family"))
                .andExpect(jsonPath("$.bedrooms").value(3));
    }

    @Test
    void whenGetAllProperties_thenReturnsPropertyList() throws Exception {
        Property prop1 = new Property();
        prop1.setAddress("111 First St");

        Property prop2 = new Property();
        prop2.setAddress("222 Second St");

        // --- Mock the service method ---
        when(propertyService.getAllProperties()).thenReturn(List.of(prop1, prop2));

        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].address").value("111 First St"))
                .andExpect(jsonPath("$[1].address").value("222 Second St"));
    }

    @Test
    void whenGetPropertyById_givenPropertyExists_thenReturnsProperty() throws Exception {
        Property property = new Property();
        property.setId(1L);
        property.setAddress("456 Oak Ave");
        property.setType("Condo");

        // --- Mock the service method ---
        when(propertyService.getPropertyById(1L)).thenReturn(Optional.of(property));

        mockMvc.perform(get("/api/properties/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.address").value("456 Oak Ave"))
                .andExpect(jsonPath("$.type").value("Condo"));
    }

    @Test
    void whenGetPropertyById_givenPropertyDoesNotExist_thenReturnsNotFound() throws Exception {
        // --- Mock the service method ---
        when(propertyService.getPropertyById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/properties/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenUpdateProperty_givenPropertyExists_thenReturnsUpdatedProperty() throws Exception {
        Property updatedInfo = new Property();
        updatedInfo.setAddress("123 New St");
        updatedInfo.setType("Duplex");

        Property savedProperty = new Property();
        savedProperty.setId(1L);
        savedProperty.setAddress("123 New St");
        savedProperty.setType("Duplex");

        // --- Mock the service method ---
        when(propertyService.updateProperty(eq(1L), any(Property.class))).thenReturn(Optional.of(savedProperty));

        mockMvc.perform(put("/api/properties/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("123 New St"))
                .andExpect(jsonPath("$.type").value("Duplex"));
    }

    @Test
    void whenUpdateProperty_givenPropertyDoesNotExist_thenReturnsNotFound() throws Exception {
        Property updatedInfo = new Property();
        updatedInfo.setAddress("123 New St");

        // --- Mock the service method ---
        when(propertyService.updateProperty(eq(99L), any(Property.class))).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/properties/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedInfo)))
                .andExpect(status().is(400));
    }

    @Test
    void whenDeleteProperty_givenPropertyExists_thenReturnsNoContent() throws Exception {
        // --- Mock the service method ---
        when(propertyService.deleteProperty(1L)).thenReturn(true); // Service returns true if successful

        mockMvc.perform(delete("/api/properties/1"))
                .andExpect(status().isNoContent());

        verify(propertyService).deleteProperty(1L);
    }

    @Test
    void whenDeleteProperty_givenPropertyDoesNotExist_thenReturnsNotFound() throws Exception {
        // --- Mock the service method ---
        when(propertyService.deleteProperty(99L)).thenReturn(false); // Service returns false if not found

        mockMvc.perform(delete("/api/properties/99"))
                .andExpect(status().isNotFound());
    }

}

