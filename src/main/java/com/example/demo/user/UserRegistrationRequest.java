package com.example.demo.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor; // Keep if needed for tests, or use specific constructor
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // Needed for Jackson/JSON binding
// @AllArgsConstructor // We might not need this if we define specific constructors
public class UserRegistrationRequest {

    @NotBlank(message = "Username (email) is mandatory")
    @Email(message = "Username must be a valid email")
    private String username;

    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    // --- REMOVE Role and TenantId ---
    // These are assigned by the backend service, not sent by the client during registration
    // private User.Role role;
    // private Long tenantId;
    // --- End Remove ---

    // --- Constructor used by tests and potentially internally ---
    public UserRegistrationRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Lombok's @AllArgsConstructor would generate this, but defining it
    // explicitly makes the dependencies clearer if fields change.
    // If you add more fields *sent by the client*, update this constructor
    // or rely on @AllArgsConstructor if appropriate.
}

