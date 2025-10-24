package com.example.demo.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void whenRegisterUser_givenNewUser_thenReturnsUser() {
        // GIVEN
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("newuser@example.com");
        request.setPassword("securepassword");
        request.setTenantId(1L);
        request.setRole(User.Role.USER);

        User savedUser = new User();
        savedUser.setUsername(request.getUsername());
        savedUser.setPassword("encodedpassword"); // Mock the encoded password
        savedUser.setRole(request.getRole());

        // Mocks
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedpassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // WHEN
        User result = authService.registerNewUser(request);

        // THEN
        assertNotNull(result);
        assertEquals("encodedpassword", result.getPassword());
        assertEquals(User.Role.USER, result.getRole());
    }

    @Test
    void whenRegisterUser_givenExistingUser_thenThrowsException() {
        // GIVEN
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("existing@example.com");
        request.setPassword("securepassword");

        User existingUser = new User();

        // Mocks
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.of(existingUser));

        // WHEN / THEN
        assertThrows(ResponseStatusException.class, () -> authService.registerNewUser(request));
    }
}
