package com.example.demo.user;

import com.example.demo.exception.UserAlreadyExistsException;
import com.example.demo.permission.Permission; // Import Permission
import com.example.demo.permission.PermissionRepository; // Import PermissionRepository
import com.example.demo.tenant.Tenant;
import com.example.demo.tenant.TenantRepository;
import jakarta.annotation.PostConstruct; // Import PostConstruct
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet; // Import HashSet
import java.util.Optional;
import java.util.Set; // Import Set
import java.util.List; // Import List

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository;
    private final PermissionRepository permissionRepository; // Inject PermissionRepository

    // Constants for default permissions
    public static final String DEFAULT_USER_PERMISSION = "PERMISSION_READ_OWN_DATA";
    public static final String PERMISSION_MANAGE_USERS = "PERMISSION_MANAGE_USERS";
    public static final String PERMISSION_DELETE_VENDOR = "PERMISSION_DELETE_VENDOR";
    public static final String PERMISSION_DELETE_WORK_ORDER = "PERMISSION_DELETE_WORK_ORDER";
    public static final String PERMISSION_DELETE_PROPERTY = "PERMISSION_DELETE_PROPERTY"; // Add constant


    // Updated Constructor
    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           TenantRepository tenantRepository,
                           PermissionRepository permissionRepository) { // Add permissionRepository
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantRepository = tenantRepository;
        this.permissionRepository = permissionRepository; // Assign permissionRepository
    }

    // --- Ensure default permissions exist on startup ---
    @PostConstruct
    @Transactional
    public void initPermissions() {
        findOrCreatePermission(DEFAULT_USER_PERMISSION);
        // Add other essential permissions
        findOrCreatePermission(PERMISSION_MANAGE_USERS);
        findOrCreatePermission(PERMISSION_DELETE_VENDOR);
        findOrCreatePermission(PERMISSION_DELETE_WORK_ORDER);
        // --- ADD THE MISSING PERMISSION CHECK ---
        findOrCreatePermission(PERMISSION_DELETE_PROPERTY);
        // --- END FIX ---
    }

    private Permission findOrCreatePermission(String name) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(new Permission(name)));
    }
    // --- End permission initialization ---


    @Override
    @Transactional
    public User registerNewUser(UserRegistrationRequest registrationRequest) {
        // 1. Check if user already exists
        Optional<User> existingUser = userRepository.findByUsername(registrationRequest.getUsername());
        if (existingUser.isPresent()) {
            throw new UserAlreadyExistsException("User with email " + registrationRequest.getUsername() + " already exists.");
        }

        // --- Determine or Create Tenant ---
        Long tenantIdToAssign;
        Tenant tenantToAssign; // Keep track of the tenant object
        long tenantCount = tenantRepository.count();

        if (tenantCount == 0) {
            // First user registration - create a new tenant
            String tenantName = extractTenantName(registrationRequest.getUsername());
            Optional<Tenant> existingTenantOpt = tenantRepository.findByName(tenantName);
            if(existingTenantOpt.isPresent()) {
                tenantToAssign = existingTenantOpt.get();
            } else {
                Tenant newTenant = new Tenant();
                newTenant.setName(tenantName);
                tenantToAssign = tenantRepository.save(newTenant);
            }
            tenantIdToAssign = tenantToAssign.getId();

        } else {
            // Subsequent registrations - assign to default tenant (ID 1L)
            tenantToAssign = tenantRepository.findById(1L)
                    .orElseThrow(() -> new IllegalStateException("Default tenant with ID 1 not found. Ensure it exists or handle this case."));
            tenantIdToAssign = tenantToAssign.getId();
        }
        // --- End Tenant Logic ---


        // 2. Create new user entity
        User newUser = new User();
        newUser.setUsername(registrationRequest.getUsername());
        newUser.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        newUser.setTenantId(tenantIdToAssign);

        // --- Assign Default Permissions ---
        Set<Permission> defaultPermissions = new HashSet<>();
        // Find the essential 'user' permission
        Permission userPermission = findOrCreatePermission(DEFAULT_USER_PERMISSION);
        defaultPermissions.add(userPermission);

        // If this is the very first user ever (implicitly the first admin)
        // Or if the tenant was just created for this user (first user of tenant)
        // Give them admin permissions as well
        if (userRepository.count() == 0 || (tenantCount == 0 && tenantToAssign.getId().equals(tenantIdToAssign))) {
            defaultPermissions.addAll(permissionRepository.findAll()); // Give all defined permissions
        }

        newUser.setPermissions(defaultPermissions);
        // --- End Assign Permissions ---


        // 3. Save the new user
        User savedUser = userRepository.save(newUser);

        return savedUser;
    }

    // Helper method to extract a tenant name
    private String extractTenantName(String email) {
        if (email != null && email.contains("@")) {
            String domain = email.substring(email.indexOf('@') + 1);
            String namePart = domain.split("\\.")[0];
            return namePart.substring(0, 1).toUpperCase() + namePart.substring(1) + " Tenant";
        }
        return "DefaultTenant"; // Fallback name
    }

}

