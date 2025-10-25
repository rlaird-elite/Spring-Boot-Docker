package com.example.demo.tenant;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tenants")
@Data // Adds getters, setters, toString, equals, hashCode
@NoArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // Add other tenant-specific fields here later if needed
    // (e.g., address, subscription level, etc.)

    public Tenant(String name) {
        this.name = name;
    }
}
