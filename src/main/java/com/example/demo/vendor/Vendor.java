package com.example.demo.vendor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Vendor name is mandatory")
    @Column(nullable = false)
    private String name;

    private String specialty; // e.g., Plumbing, Electrical, HVAC

    private String contactInfo;

    // --- NEW: Add Tenant ID ---
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    // --- End Tenant ID ---

}

