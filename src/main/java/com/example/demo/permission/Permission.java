package com.example.demo.permission;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode; // Import for Set equality

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // Important for Set operations
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Include // Use name for equality checks
    @Column(unique = true, nullable = false)
    private String name; // e.g., "PERMISSION_DELETE_VENDOR", "PERMISSION_CREATE_WORK_ORDER"

    public Permission(String name) {
        this.name = name;
    }

    // We'll likely need a way to pre-populate permissions later (e.g., DataLoader)
}
