package com.example.demo.workorder;

import com.example.demo.property.Property;
import com.example.demo.vendor.Vendor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime; // Import LocalDateTime

@Entity
@Data
@NoArgsConstructor
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // Many WorkOrders to One Property
    @JoinColumn(name = "property_id", nullable = false)
    private Property property; // Associated Property

    @ManyToOne(fetch = FetchType.LAZY) // Many WorkOrders to One Vendor (optional)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor; // Assigned Vendor (can be null)

    @NotBlank(message = "Description is mandatory")
    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String status = "PENDING"; // Default status set by service

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // Automatically set creation time

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now(); // Automatically set update time

    // --- NEW: Add Tenant ID ---
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    // --- End Tenant ID ---


    // --- Lifecycle Callbacks for Timestamps ---
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

