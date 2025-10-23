package com.example.demo.property;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List; // Import List

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get; // Import 'get'
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize; // Import hasSize
import static org.hamcrest.Matchers.is; // Import is

@WebMvcTest(PropertyController.class)
public class PropertyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PropertyRepository propertyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void whenCreateProperty_thenReturnsCreatedProperty() throws Exception {
        Property property = new Property();
        property.setAddress("123 Main St");
        property.setType("Single Family");
        property.setBedrooms(3);
        property.setBathrooms(2);

        // Mock the repository's save method
        when(propertyRepository.save(any(Property.class))).thenReturn(property);

        // Perform the POST request
        mockMvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(property)))
                .andExpect(status().isOk()) // Or isCreated(201) if you prefer
                .andExpect(jsonPath("$.address", is("123 Main St")));
    }

    // --- THIS IS THE NEW TEST ---
    @Test
    public void whenGetAllProperties_thenReturnsPropertyList() throws Exception {
        // 1. Setup
        Property prop1 = new Property();
        prop1.setAddress("123 Main St");
        Property prop2 = new Property();
        prop2.setAddress("456 Oak Ave");
        List<Property> properties = List.of(prop1, prop2);

        // 2. Mock the repository's findAll method
        when(propertyRepository.findAll()).thenReturn(properties);

        // 3. Perform the GET request and assert
        mockMvc.perform(get("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Check that the list has 2 items
                .andExpect(jsonPath("$[0].address", is("123 Main St")))
                .andExpect(jsonPath("$[1].address", is("456 Oak Ave")));
    }
}

