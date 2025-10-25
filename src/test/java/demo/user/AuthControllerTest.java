package com.example.demo.user;

import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.exception.UserAlreadyExistsException; // Import the custom exception
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication; // Import Authentication
import org.springframework.test.web.servlet.MockMvc;

// Import SecurityConfig and UserDetailsService for context
import com.example.demo.SecurityConfig;

// Import static mock method
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;
    @MockBean
    private AuthenticationManager authenticationManager; // Mock AuthenticationManager
    @MockBean
    private JwtTokenProvider jwtTokenProvider; // Mock JwtTokenProvider
    @MockBean
    private CustomUserDetailsService customUserDetailsService; // Needed by SecurityConfig

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void whenRegisterUser_withValidData_thenReturnsOk() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("test@example.com", "Password123");

        // --- THIS IS THE FIX ---
        // Ensure the User object returned by the mock has the Role set
        User registeredUser = new User();
        registeredUser.setId(1L);
        registeredUser.setUsername("test@example.com");
        registeredUser.setRole(User.Role.USER); // Set the role here!
        registeredUser.setTenantId(1L); // Also set tenantId for completeness
        // --- END FIX ---


        when(authService.registerNewUser(any(UserRegistrationRequest.class))).thenReturn(registeredUser);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf()) // Add CSRF token for POST
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // Expect 200 OK
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER")); // Can now assert role
    }

    @Test
    void whenRegisterUser_withExistingUsername_thenReturnsConflict() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("existing@example.com", "Password123");

        when(authService.registerNewUser(any(UserRegistrationRequest.class)))
                .thenThrow(new UserAlreadyExistsException("User already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf()) // Add CSRF token for POST
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict()) // Expect 409 Conflict
                .andExpect(jsonPath("$.message").value("User already exists")); // Check the error message in JSON
    }

    @Test
    void whenLoginUser_withValidCredentials_thenReturnsToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "Password123");
        String fakeToken = "fake-jwt-token";

        // Mock the Authentication object that AuthenticationManager returns
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test@example.com");

        // Mock AuthenticationManager to return the mock Authentication
        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // Mock JwtTokenProvider to return a fake token
        when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn(fakeToken);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf()) // Add CSRF token for POST
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(fakeToken));
    }

    // Add a test for invalid login credentials if needed
    // @Test
    // void whenLoginUser_withInvalidCredentials_thenReturnsUnauthorized() throws Exception { ... }

}

