package com.example.demo.property;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Address is mandatory")
    @Column(nullable = false)
    private String address;

    @NotBlank(message = "Type is mandatory")
    @Column(nullable = false)
    private String type; // e.g., Single Family, Condo, Apartment

    @Min(value = 0, message = "Bedrooms must be non-negative")
    private int bedrooms;

    @Min(value = 0, message = "Bathrooms must be non-negative")
    private int bathrooms;

    // --- NEW: Add Tenant ID ---
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    // --- End Tenant ID ---

}

