package com.example.demo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Includes getters, setters, toString, equals, hashCode
@NoArgsConstructor // Needed for Jackson deserialization if used
public class LoginResponse {

    private String token;
    // Add other fields you might want to return after login, e.g., username, roles
    // private String username;
    // private User.Role role;
    // private Long tenantId;

    // --- Constructor needed by AuthController ---
    public LoginResponse(String token) {
        this.token = token;
    }

    // --- You might still need an AllArgsConstructor for other purposes ---
    // Or remove this if you only ever construct it with the token
    // public LoginResponse(String token, String username, User.Role role, Long tenantId) {
    //    this.token = token;
    //    this.username = username;
    //    this.role = role;
    //    this.tenantId = tenantId;
    // }
}

