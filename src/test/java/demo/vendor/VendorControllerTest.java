package com.example.demo.vendor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VendorController.class)
public class VendorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VendorRepository vendorRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void whenCreateVendor_thenReturnsCreatedVendor() throws Exception {
        Vendor vendor = new Vendor("A1 Plumbing", "Plumbing", "555-1234");

        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> {
            Vendor v = invocation.getArgument(0);
            v.setId(1L);
            return v;
        });

        mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("A1 Plumbing"));
    }
}
