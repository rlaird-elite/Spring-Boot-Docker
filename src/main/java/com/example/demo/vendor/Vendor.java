package com.example.demo.vendor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
// --- Add imports for Validation ---
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data; // Import @Data

@Data
@Entity
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // --- VALIDATION RULES ---
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    private String name; // e.g., "A1 Plumbing"
    
    @NotBlank(message = "Trade is required")
    private String trade; // e.g., "Plumbing", "Electrical"
    
    private String phoneNumber;
    
    @NotBlank(message = "Service Type is required")
    private String serviceType;

}
