package com.example.demo.property;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data; // Import @Data

// @Data is a Lombok annotation that bundles @Getter, @Setter,
// @ToString, @EqualsAndHashCode, and @RequiredArgsConstructor
@Entity
@Data // THIS IS THE FIX: Replaces @Setter and adds @Getter
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String address;
    private String type;
    private int bedrooms;
    private int bathrooms;

    // With @Data, Lombok automatically generates:
    // public Long getId() { ... }
    // public String getAddress() { ... }
    // public void setAddress(String address) { ... }
    // public String getType() { ... }
    // public void setType(String type) { ... }
    // ...and so on for all fields.
}

