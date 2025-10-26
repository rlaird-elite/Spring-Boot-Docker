package com.example.demo.user;

import com.example.demo.exception.UserAlreadyExistsException;
import com.example.demo.permission.Permission; // Import Permission
import com.example.demo.permission.PermissionRepository; // Import PermissionRepository
import com.example.demo.tenant.Tenant; // Import Tenant
import com.example.demo.tenant.TenantRepository; // Import TenantRepository
import org.junit.jupiter.api.AfterEach; // Import AfterEach
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor; // Import ArgumentCaptor
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections; // Import Collections
import java.util.HashSet; // Import HashSet
import java.util.List; // Import List
import java.util.Optional;
import java.util.Set; // Import Set

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private PermissionRepository permissionRepository; // Mock PermissionRepository

    @InjectMocks
    private AuthServiceImpl authService; // Test the implementation

    private AutoCloseable closeable;

    // Define mock permissions
    private Permission mockUserPermission;
    private Permission mockAdminPermission1;
    private Permission mockAdminPermission2;
    private Permission mockAdminPermission3;
    private Permission mockDeletePropertyPermission; // Added


    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        // Setup mock permissions
        mockUserPermission = new Permission(AuthServiceImpl.DEFAULT_USER_PERMISSION);
        mockUserPermission.setId(100L);
        mockAdminPermission1 = new Permission(AuthServiceImpl.PERMISSION_MANAGE_USERS);
        mockAdminPermission1.setId(101L);
        mockAdminPermission2 = new Permission(AuthServiceImpl.PERMISSION_DELETE_VENDOR);
        mockAdminPermission2.setId(102L);
        mockAdminPermission3 = new Permission(AuthServiceImpl.PERMISSION_DELETE_WORK_ORDER);
        mockAdminPermission3.setId(104L);
        mockDeletePropertyPermission = new Permission(AuthServiceImpl.PERMISSION_DELETE_PROPERTY); // Added for init test consistency
        mockDeletePropertyPermission.setId(105L);


        // --- Mock Permission Repository Behavior ---
        // Mock findOrCreatePermission logic used internally
        when(permissionRepository.findByName(AuthServiceImpl.DEFAULT_USER_PERMISSION))
                .thenReturn(Optional.of(mockUserPermission));
        when(permissionRepository.findByName(AuthServiceImpl.PERMISSION_MANAGE_USERS))
                .thenReturn(Optional.of(mockAdminPermission1));
        when(permissionRepository.findByName(AuthServiceImpl.PERMISSION_DELETE_VENDOR))
                .thenReturn(Optional.of(mockAdminPermission2));
        when(permissionRepository.findByName(AuthServiceImpl.PERMISSION_DELETE_WORK_ORDER))
                .thenReturn(Optional.of(mockAdminPermission3));
        // FIX: Add mock for the missing permission checked by initPermissions
        when(permissionRepository.findByName(AuthServiceImpl.PERMISSION_DELETE_PROPERTY))
                .thenReturn(Optional.of(mockDeletePropertyPermission));
        // --- END FIX ---


        // Mock findAll used when assigning admin permissions
        when(permissionRepository.findAll()).thenReturn(List.of(
                mockUserPermission, mockAdminPermission1, mockAdminPermission2, mockAdminPermission3, mockDeletePropertyPermission
        ));

        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> {
            Permission p = invocation.getArgument(0);
            return p;
        });
        // --- End Permission Repo Mock ---


        // Mock password encoder
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

        // Mock tenant repository count for initial setup
        when(tenantRepository.count()).thenReturn(1L);
        Tenant defaultTenant = new Tenant(); defaultTenant.setId(1L); defaultTenant.setName("Default Tenant");
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(defaultTenant));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userToSave = invocation.getArgument(0);
            User savedUser = new User();
            savedUser.setId(userToSave.getId() == null ? 1L : userToSave.getId());
            savedUser.setUsername(userToSave.getUsername());
            savedUser.setPassword(userToSave.getPassword());
            savedUser.setTenantId(userToSave.getTenantId());
            savedUser.setPermissions(new HashSet<>(userToSave.getPermissions()));
            return savedUser;
        });

    }

    @AfterEach
    void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    void whenRegisterNewUser_withValidData_andTenantExists_thenSavesUserWithDefaultPermissions() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest("new@example.com", "Password123");
        when(userRepository.findByUsername("new@example.com")).thenReturn(Optional.empty());
        // FIX: Mock userRepository.count() to avoid first user logic
        when(userRepository.count()).thenReturn(1L); // Ensure it's not the first user ever
        // --- END FIX ---
        // Tenant setup from @BeforeEach is sufficient

        // Act
        User registeredUser = authService.registerNewUser(request);

        // Assert
        assertNotNull(registeredUser);
        assertEquals("new@example.com", registeredUser.getUsername());
        assertEquals(1L, registeredUser.getTenantId());
        assertNotNull(registeredUser.getPermissions());
        assertEquals(1, registeredUser.getPermissions().size());
        assertTrue(registeredUser.getPermissions().contains(mockUserPermission));

        // Verify interactions
        verify(userRepository).findByUsername("new@example.com");
        verify(tenantRepository).count();
        verify(userRepository).count();
        verify(tenantRepository).findById(1L);
        verify(permissionRepository).findByName(AuthServiceImpl.DEFAULT_USER_PERMISSION);
        verify(permissionRepository, never()).findAll();
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(1, userCaptor.getValue().getPermissions().size());
        assertNull(userCaptor.getValue().getId());
    }

    @Test
    void whenRegisterNewUser_asFirstUserEver_thenSavesUserWithAllPermissions() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest("admin@first.com", "Password123");
        when(userRepository.findByUsername("admin@first.com")).thenReturn(Optional.empty());
        when(tenantRepository.count()).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);

        Tenant newTenant = new Tenant(); newTenant.setId(1L); newTenant.setName("First Tenant");
        when(tenantRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(newTenant);

        // Act
        User registeredUser = authService.registerNewUser(request);

        // Assert
        assertNotNull(registeredUser);
        assertEquals("admin@first.com", registeredUser.getUsername());
        assertEquals(1L, registeredUser.getTenantId());
        assertNotNull(registeredUser.getPermissions());
        assertEquals(5, registeredUser.getPermissions().size()); // Expecting 5
        assertTrue(registeredUser.getPermissions().contains(mockUserPermission));
        assertTrue(registeredUser.getPermissions().contains(mockAdminPermission1));
        assertTrue(registeredUser.getPermissions().contains(mockAdminPermission2));
        assertTrue(registeredUser.getPermissions().contains(mockAdminPermission3));
        assertTrue(registeredUser.getPermissions().contains(mockDeletePropertyPermission));


        // Verify interactions
        verify(userRepository).findByUsername("admin@first.com");
        verify(tenantRepository).count();
        verify(userRepository).count();
        verify(tenantRepository).save(any(Tenant.class));
        verify(permissionRepository).findByName(AuthServiceImpl.DEFAULT_USER_PERMISSION);
        verify(permissionRepository).findAll();
        // FIX: Use ArgumentCaptor for verification
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        // Assert on the *captured* object
        User userPassedToSave = userCaptor.getValue();
        assertEquals(5, userPassedToSave.getPermissions().size());
        assertNull(userPassedToSave.getId());
        // --- END FIX ---
    }


    @Test
    void whenRegisterNewUser_withExistingUsername_thenThrowsUserAlreadyExistsException() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest("existing@example.com", "Password123");
        User existingUser = new User();
        when(userRepository.findByUsername("existing@example.com")).thenReturn(Optional.of(existingUser));

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            authService.registerNewUser(request);
        });

        // Verify interactions
        verify(userRepository).findByUsername("existing@example.com");
        verifyNoInteractions(tenantRepository);
        verifyNoInteractions(permissionRepository);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void whenInitPermissions_andPermissionExists_thenDoesNotSave() {
        // Arrange (mocks for findByName return Optional.of(...) from @BeforeEach, includes all 5)

        // Act
        authService.initPermissions(); // Manually call @PostConstruct method

        // Assert
        verify(permissionRepository).findByName(AuthServiceImpl.DEFAULT_USER_PERMISSION);
        verify(permissionRepository).findByName(AuthServiceImpl.PERMISSION_MANAGE_USERS);
        verify(permissionRepository).findByName(AuthServiceImpl.PERMISSION_DELETE_VENDOR);
        verify(permissionRepository).findByName(AuthServiceImpl.PERMISSION_DELETE_WORK_ORDER);
        verify(permissionRepository).findByName(AuthServiceImpl.PERMISSION_DELETE_PROPERTY); // Verify check

        // FIX: Verification remains the same, mock was fixed
        verify(permissionRepository, never()).save(any(Permission.class));
        // --- END FIX ---
    }

    @Test
    void whenInitPermissions_andPermissionDoesNotExist_thenSavesPermission() {
        // Arrange
        when(permissionRepository.findByName(AuthServiceImpl.PERMISSION_MANAGE_USERS)).thenReturn(Optional.empty());
        Permission savedManageUsers = new Permission(AuthServiceImpl.PERMISSION_MANAGE_USERS);
        when(permissionRepository.save(argThat(p -> p.getName().equals(AuthServiceImpl.PERMISSION_MANAGE_USERS))))
                .thenReturn(savedManageUsers);

        // Act
        authService.initPermissions();

        // Assert
        verify(permissionRepository).findByName(AuthServiceImpl.DEFAULT_USER_PERMISSION);
        verify(permissionRepository).findByName(AuthServiceImpl.PERMISSION_MANAGE_USERS);
        verify(permissionRepository).findByName(AuthServiceImpl.PERMISSION_DELETE_VENDOR);
        verify(permissionRepository).findByName(AuthServiceImpl.PERMISSION_DELETE_WORK_ORDER);
        verify(permissionRepository).findByName(AuthServiceImpl.PERMISSION_DELETE_PROPERTY);

        verify(permissionRepository, times(1)).save(argThat(p -> p.getName().equals(AuthServiceImpl.PERMISSION_MANAGE_USERS)));
        verify(permissionRepository, never()).save(argThat(p -> p.getName().equals(AuthServiceImpl.DEFAULT_USER_PERMISSION)));
        verify(permissionRepository, never()).save(argThat(p -> p.getName().equals(AuthServiceImpl.PERMISSION_DELETE_VENDOR)));
        verify(permissionRepository, never()).save(argThat(p -> p.getName().equals(AuthServiceImpl.PERMISSION_DELETE_WORK_ORDER)));
        verify(permissionRepository, never()).save(argThat(p -> p.getName().equals(AuthServiceImpl.PERMISSION_DELETE_PROPERTY)));
    }
}

