package com.example.demo.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.demo.user.User.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {

    @NotBlank(message = "Email is required.")
    @Email(message = "Must be a valid email address.")
    private String username;

    @NotBlank(message = "Password is required.")
    private String password;

    private Role role;
    private Long tenantId;

    public UserRegistrationRequest(String username, String password) {
        this.username = username;
        this.password = password;
        this.role = Role.USER;
        this.tenantId = 1L;
    }
}
