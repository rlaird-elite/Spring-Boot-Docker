package com.example.demo.property;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
// --- Add imports for Validation ---
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
@Entity
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- VALIDATION RULES ---
    @NotBlank(message = "Address is mandatory")
    @Size(min = 5, max = 255, message = "Address must be between 5 and 255 characters")
    private String address;

    @NotBlank(message = "Type is mandatory")
    private String type;

    @NotNull(message = "Bedrooms cannot be null")
    @Min(value = 0, message = "Bedrooms must be zero or more")
    private int bedrooms;

    @NotNull(message = "Bathrooms cannot be null")
    @Min(value = 0, message = "Bathrooms must be zero or more")
    private int bathrooms;
}

