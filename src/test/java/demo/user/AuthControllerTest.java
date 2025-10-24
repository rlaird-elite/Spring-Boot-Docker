package com.example.demo.user;

import com.example.demo.exception.UserAlreadyExistsException;
import com.example.demo.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    // --- New Mocks for Login ---
    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider; // We will create this class next

    @Mock
    private UserRepository userRepository;
    // --- End New Mocks ---

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // --- Existing Registration Tests ---

    @Test
    void whenRegisterUser_withValidData_thenReturnsCreated() throws Exception {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest("test@example.com", "Password123");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("test@example.com");
        savedUser.setRole(User.Role.USER);
        savedUser.setTenantId(1L);

        when(authService.registerNewUser(any(UserRegistrationRequest.class))).thenReturn(savedUser);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void whenRegisterUser_withExistingUsername_thenReturnsConflict() throws Exception {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest("existing@example.com", "Password123");

        doThrow(new UserAlreadyExistsException("Must be a valid email address."))
                .when(authService).registerNewUser(any(UserRegistrationRequest.class));

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Must be a valid email address."));
    }

    @Test
    void whenRegisterUser_withInvalidData_thenReturnsBadRequest() throws Exception {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest("", "Password123");

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- New Failing Test for Login ---

    @Test
    void whenLoginUser_withValidCredentials_thenReturnsToken() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("test@example.com", "Password123");

        // Mock Authentication
        Authentication authentication = new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        // Mock Token Generation
        String fakeToken = "fake.jwt.token";
        when(jwtTokenProvider.generateToken(authentication)).thenReturn(fakeToken);

        // Mock User details lookup
        User user = new User();
        user.setId(1L);
        user.setUsername("test@example.com");
        user.setRole(User.Role.USER);
        user.setTenantId(1L);
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(user));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk()) // This will fail, expecting 404
                .andExpect(jsonPath("$.token").value(fakeToken))
                .andExpect(jsonPath("$.username").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}

