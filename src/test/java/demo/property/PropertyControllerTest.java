package com.example.demo.property;

import com.example.demo.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach; // Keep BeforeEach for consistency if needed, but not for MockMvc setup
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser; // Import WithMockUser
import org.springframework.test.web.servlet.MockMvc;

// Import SecurityConfig and UserDetailsService for context
import com.example.demo.SecurityConfig;
import com.example.demo.user.CustomUserDetailsService;
import com.example.demo.user.JwtTokenProvider; // Corrected import path if needed

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*; // Import security post processors


@WebMvcTest(PropertyController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class}) // Import SecurityConfig and Exception Handler
public class PropertyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PropertyService propertyService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider; // Needed by JwtAuthenticationFilter
    @MockBean
    private CustomUserDetailsService customUserDetailsService; // Needed by SecurityConfig/Filter

    @Autowired
    private ObjectMapper objectMapper;

    // Optional: Use BeforeEach for common setup if needed, but not MockMvc
    // @BeforeEach
    // void setUp() { ... }

    @Test
    void whenGetAllProperties_withoutAuthentication_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isForbidden()); // Expect 403 Forbidden
    }

    @Test
    @WithMockUser // Simulate an authenticated user
    void whenCreateProperty_thenReturnsCreatedProperty() throws Exception {
        Property property = new Property();
        property.setAddress("123 Main St");
        // --- THIS IS THE FIX ---
        property.setType("Single Family"); // Add a valid type
        // --- END FIX ---
        property.setBedrooms(3);
        property.setBathrooms(2);

        Property savedProperty = new Property();
        savedProperty.setId(1L);
        savedProperty.setAddress("123 Main St");
        savedProperty.setType("Single Family");
        savedProperty.setBedrooms(3);
        savedProperty.setBathrooms(2);


        when(propertyService.createProperty(any(Property.class))).thenReturn(savedProperty);

        mockMvc.perform(post("/api/properties")
                        .with(csrf()) // Add CSRF token for POST/PUT/DELETE
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(property)))
                .andExpect(status().isCreated()) // Expect 201 Created
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.address").value("123 Main St"))
                .andExpect(jsonPath("$.type").value("Single Family")); // Assert type
    }

    @Test
    @WithMockUser // Simulate an authenticated user
    void whenGetAllProperties_thenReturnsPropertyList() throws Exception {
        Property prop1 = new Property();
        prop1.setId(1L);
        prop1.setAddress("111 First St");
        prop1.setType("Condo"); // Add type for consistency

        Property prop2 = new Property();
        prop2.setId(2L);
        prop2.setAddress("222 Second St");
        prop2.setType("Apartment"); // Add type for consistency

        when(propertyService.getAllProperties()).thenReturn(List.of(prop1, prop2));

        mockMvc.perform(get("/api/properties")) // GET requests don't need CSRF by default
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].address").value("111 First St"))
                .andExpect(jsonPath("$[1].address").value("222 Second St"));
    }

    @Test
    @WithMockUser
    void whenGetPropertyById_givenPropertyExists_thenReturnsProperty() throws Exception {
        Property property = new Property();
        property.setId(1L);
        property.setAddress("456 Oak Ave");
        property.setType("Condo");

        when(propertyService.getPropertyById(1L)).thenReturn(Optional.of(property));

        mockMvc.perform(get("/api/properties/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.address").value("456 Oak Ave"))
                .andExpect(jsonPath("$.type").value("Condo"));
    }

    @Test
    @WithMockUser
    void whenGetPropertyById_givenPropertyDoesNotExist_thenReturnsNotFound() throws Exception {
        when(propertyService.getPropertyById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/properties/99"))
                .andExpect(status().isNotFound());
    }


    @Test
    @WithMockUser
    void whenUpdateProperty_givenPropertyExists_thenReturnsUpdatedProperty() throws Exception {
        Long propertyId = 1L;
        Property updatedDetails = new Property();
        updatedDetails.setAddress("123 Updated St");
        // --- THIS IS THE FIX ---
        updatedDetails.setType("Duplex"); // Add a valid type
        // --- END FIX ---
        updatedDetails.setBedrooms(4);
        updatedDetails.setBathrooms(3);


        Property returnedProperty = new Property();
        returnedProperty.setId(propertyId);
        returnedProperty.setAddress("123 Updated St");
        returnedProperty.setType("Duplex");
        returnedProperty.setBedrooms(4);
        returnedProperty.setBathrooms(3);

        when(propertyService.updateProperty(anyLong(), any(Property.class))).thenReturn(Optional.of(returnedProperty));

        mockMvc.perform(put("/api/properties/{id}", propertyId)
                        .with(csrf()) // Add CSRF
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDetails)))
                .andExpect(status().isOk()) // Update returns 200 OK
                .andExpect(jsonPath("$.id").value(propertyId))
                .andExpect(jsonPath("$.address").value("123 Updated St"))
                .andExpect(jsonPath("$.type").value("Duplex")); // Assert type
    }

    @Test
    @WithMockUser
    void whenUpdateProperty_givenPropertyDoesNotExist_thenReturnsNotFound() throws Exception {
        Long propertyId = 99L;
        Property updatedDetails = new Property();
        updatedDetails.setAddress("123 Updated St");
        // --- THIS IS THE FIX ---
        updatedDetails.setType("Townhouse"); // Add a valid type (still needed for validation)
        // --- END FIX ---

        when(propertyService.updateProperty(anyLong(), any(Property.class))).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/properties/{id}", propertyId)
                        .with(csrf()) // Add CSRF
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDetails)))
                .andExpect(status().isNotFound()); // Expect 404 Not Found
    }


    @Test
    @WithMockUser
    void whenDeleteProperty_givenPropertyExists_thenReturnsNoContent() throws Exception {
        Long propertyId = 1L;
        when(propertyService.deleteProperty(propertyId)).thenReturn(true);

        mockMvc.perform(delete("/api/properties/{id}", propertyId)
                        .with(csrf())) // Add CSRF
                .andExpect(status().isNoContent()); // Expect 204 No Content
    }

    @Test
    @WithMockUser
    void whenDeleteProperty_givenPropertyDoesNotExist_thenReturnsNotFound() throws Exception {
        Long propertyId = 99L;
        when(propertyService.deleteProperty(propertyId)).thenReturn(false);

        mockMvc.perform(delete("/api/properties/{id}", propertyId)
                        .with(csrf())) // Add CSRF
                .andExpect(status().isNotFound()); // Expect 404 Not Found
    }


    @Test
    @WithMockUser // This test runs validation before the service is called
    void whenCreateProperty_withInvalidData_thenReturnsBadRequest() throws Exception {
        Property invalidProperty = new Property();
        invalidProperty.setAddress("Test Address"); // Need address
        invalidProperty.setType(""); // Invalid type (blank)

        // No service mock needed as validation happens first

        mockMvc.perform(post("/api/properties")
                        .with(csrf()) // Add CSRF
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidProperty)))
                .andExpect(status().isBadRequest()) // Expect 400 Bad Request
                .andExpect(jsonPath("$.type").value("Type is mandatory")); // Check validation error message
    }

}

