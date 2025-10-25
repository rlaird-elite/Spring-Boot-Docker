package com.example.demo.user;

import com.example.demo.exception.UserAlreadyExistsException;
import com.example.demo.tenant.Tenant;
import com.example.demo.tenant.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock // Mock the new dependency
    private TenantRepository tenantRepository;

    @InjectMocks
    private AuthServiceImpl authService; // Test the implementation

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void whenRegisterNewUser_givenFirstUser_thenCreatesTenantAndUser() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest("test@example.com", "Password123");
        String expectedTenantName = "example Company";
        Tenant newTenant = new Tenant(expectedTenantName);
        newTenant.setId(1L); // Assume save returns an ID

        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(tenantRepository.count()).thenReturn(0L); // Simulate no existing tenants
        when(tenantRepository.findByName(expectedTenantName)).thenReturn(Optional.empty()); // Simulate derived tenant name is new
        when(tenantRepository.save(any(Tenant.class))).thenReturn(newTenant); // Mock saving the new tenant
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername(request.getUsername());
        savedUser.setPassword("encodedPassword");
        savedUser.setRole(User.Role.USER);
        savedUser.setTenantId(newTenant.getId()); // Expect tenant ID 1
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = authService.registerNewUser(request);

        // Assert
        assertNotNull(result);
        assertEquals(request.getUsername(), result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals(User.Role.USER, result.getRole());
        assertEquals(1L, result.getTenantId()); // Verify correct tenant ID
        verify(tenantRepository, times(1)).save(any(Tenant.class)); // Verify tenant was created
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void whenRegisterNewUser_givenSubsequentUser_thenAssignsToExistingTenant() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest("another@example.com", "Password456");
        Tenant existingTenant = new Tenant("Existing Company");
        existingTenant.setId(1L); // The default tenant

        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(tenantRepository.count()).thenReturn(1L); // Simulate tenants already exist
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(existingTenant)); // Mock finding the default tenant
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword2");

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setUsername(request.getUsername());
        savedUser.setPassword("encodedPassword2");
        savedUser.setRole(User.Role.USER);
        savedUser.setTenantId(existingTenant.getId()); // Expect tenant ID 1
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = authService.registerNewUser(request);

        // Assert
        assertNotNull(result);
        assertEquals(request.getUsername(), result.getUsername());
        assertEquals("encodedPassword2", result.getPassword());
        assertEquals(User.Role.USER, result.getRole());
        assertEquals(1L, result.getTenantId()); // Verify correct tenant ID
        verify(tenantRepository, never()).save(any(Tenant.class)); // Verify NO new tenant was created
        verify(userRepository, times(1)).save(any(User.class));
    }


    @Test
    void whenRegisterNewUser_withExistingUsername_thenThrowsUserAlreadyExistsException() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest("existing@example.com", "Password123");
        User existingUser = new User(); // Mock the existing user found in the DB
        existingUser.setUsername(request.getUsername());

        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.of(existingUser));

        // Act & Assert
        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> {
            authService.registerNewUser(request);
        });

        assertEquals("User with email " + request.getUsername() + " already exists.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class)); // Ensure save was never called
        verify(tenantRepository, never()).save(any(Tenant.class)); // Ensure tenant save was never called
    }
}

