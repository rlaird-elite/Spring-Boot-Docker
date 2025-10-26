package com.example.demo.user;

import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.exception.UserAlreadyExistsException;
import com.example.demo.permission.Permission; // Import Permission
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach; // Import BeforeEach
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

// Import SecurityConfig and UserDetailsService for context
import com.example.demo.SecurityConfig;

import java.util.Set; // Import Set

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
    private AuthenticationManager authenticationManager;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private CustomUserDetailsService customUserDetailsService; // Still needed by SecurityConfig

    @Autowired
    private ObjectMapper objectMapper;

    // --- Add mock permission for tests ---
    private Permission mockUserPermission;

    @BeforeEach
    void setupPermissions() {
        // Setup mock permission object
        mockUserPermission = new Permission(AuthServiceImpl.DEFAULT_USER_PERMISSION);
        mockUserPermission.setId(100L);
    }
    // --- End permission setup ---


    @Test
    void whenRegisterUser_withValidData_thenReturnsOk() throws Exception {
        // Use the two-argument constructor we defined
        UserRegistrationRequest request = new UserRegistrationRequest("test@example.com", "Password123");

        // --- Update Mock User to use Permissions ---
        User registeredUser = new User();
        registeredUser.setId(1L);
        registeredUser.setUsername("test@example.com");
        // Create a Set containing the mock permission
        registeredUser.setPermissions(Set.of(mockUserPermission));
        registeredUser.setTenantId(1L);
        // --- End Update ---

        when(authService.registerNewUser(any(UserRegistrationRequest.class))).thenReturn(registeredUser);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // Changed expectation from 500/error to 200 OK
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("test@example.com"))
                // --- Assert Permission in response ---
                // Check if the first permission name matches (adjust if multiple permissions expected)
                .andExpect(jsonPath("$.permissions[0].name").value(AuthServiceImpl.DEFAULT_USER_PERMISSION));
        // --- End Assertion ---
    }

    @Test
    void whenRegisterUser_withExistingUsername_thenReturnsConflict() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("existing@example.com", "Password123");

        when(authService.registerNewUser(any(UserRegistrationRequest.class)))
                .thenThrow(new UserAlreadyExistsException("User already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User already exists"));
    }

    @Test
    void whenLoginUser_withValidCredentials_thenReturnsToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "Password123");
        String fakeToken = "fake-jwt-token";

        // Mock the Authentication object
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test@example.com");

        // Mock AuthenticationManager
        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // Mock JwtTokenProvider
        when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn(fakeToken);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(fakeToken));
    }
}

