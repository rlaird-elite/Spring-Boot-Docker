package com.example.demo.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "app_user") // "user" is often a reserved keyword in SQL
@Data
@NoArgsConstructor
public class User implements UserDetails { // Implement UserDetails for Spring Security

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username (email) is mandatory")
    @Email(message = "Username must be a valid email")
    @Column(unique = true, nullable = false)
    private String username; // Using email as username

    @NotBlank(message = "Password is mandatory")
    @Column(nullable = false)
    private String password;

    // --- Add the Role Enum Here ---
    public enum Role {
        USER, ADMIN // Add more roles as needed
    }

    @Enumerated(EnumType.STRING) // Store role as string in DB
    @Column(nullable = false)
    private Role role; // User's role (e.g., USER, ADMIN)
    // --- End Role Enum ---

    @Column(name = "tenant_id", nullable = false) // Add tenant ID
    private Long tenantId;

    // --- UserDetails Methods ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Return a collection containing the user's role
        // Spring Security expects roles prefixed with "ROLE_"
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    // --- Account status methods (implement as needed) ---

    @Override
    public boolean isAccountNonExpired() {
        return true; // Or implement logic
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Or implement logic
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Or implement logic
    }

    @Override
    public boolean isEnabled() {
        return true; // Or implement logic
    }
}

