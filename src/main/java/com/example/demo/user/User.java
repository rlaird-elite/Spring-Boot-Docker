package com.example.demo.user;

// --- ADD THIS IMPORT ---
import com.example.demo.permission.Permission; // Import Permission
// --- END ADD IMPORT ---
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode; // Import for Set equality
import lombok.NoArgsConstructor;
import lombok.ToString; // Import for avoiding recursion in toString
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet; // Import HashSet
import java.util.Set;     // Import Set
import java.util.stream.Collectors; // Import Collectors

@Entity
@Table(name = "app_user")
@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = "permissions") // Exclude collections from equals/hashCode
@ToString(exclude = "permissions") // Exclude collections from toString to prevent recursion
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username (email) is mandatory")
    @Email(message = "Username must be a valid email")
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank(message = "Password is mandatory")
    @Column(nullable = false)
    private String password;

    // --- Permissions Relationship ---
    @ManyToMany(fetch = FetchType.EAGER) // Load permissions eagerly with the user
    @JoinTable(
            name = "user_permissions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>(); // Use Set to avoid duplicates
    // --- End Permissions ---


    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    // --- UserDetails Methods ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Map permissions to Spring Security GrantedAuthority objects
        return permissions.stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                .collect(Collectors.toList());
    }


    // getPassword(), getUsername(), isAccountNonExpired(), etc. remain the same
    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

