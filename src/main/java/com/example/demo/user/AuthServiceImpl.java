package com.example.demo.user;

import com.example.demo.exception.UserAlreadyExistsException;
import com.example.demo.tenant.Tenant; // Import Tenant
import com.example.demo.tenant.TenantRepository; // Import TenantRepository
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository; // Inject TenantRepository

    // Updated Constructor
    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           TenantRepository tenantRepository) { // Add tenantRepository
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantRepository = tenantRepository; // Assign tenantRepository
    }

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
        long tenantCount = tenantRepository.count();

        if (tenantCount == 0) {
            // First user registration - create a new tenant
            String tenantName = extractTenantName(registrationRequest.getUsername());
            Optional<Tenant> existingTenant = tenantRepository.findByName(tenantName);
            if(existingTenant.isPresent()) {
                tenantIdToAssign = existingTenant.get().getId();
            } else {
                Tenant newTenant = new Tenant();
                newTenant.setName(tenantName);
                Tenant savedTenant = tenantRepository.save(newTenant);
                tenantIdToAssign = savedTenant.getId();
            }

        } else {
            // Subsequent registrations - assign to a default tenant (ID 1L)
            tenantIdToAssign = tenantRepository.findById(1L)
                    .orElseThrow(() -> new IllegalStateException("Default tenant with ID 1 not found. Ensure it exists or handle this case."))
                    .getId();
        }
        // --- End Tenant Logic ---


        // 2. Create new user entity
        User newUser = new User();
        newUser.setUsername(registrationRequest.getUsername());
        newUser.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        newUser.setTenantId(tenantIdToAssign); // Use the determined tenant ID

        // --- Ensure Role is set ---
        // Explicitly set the Role for the new user
        newUser.setRole(User.Role.USER); // Default new users to USER role
        // --- End Role setting ---


        // 3. Save the new user
        // The save method MUST return the entity with the role set
        User savedUser = userRepository.save(newUser);

        // Defensive check (optional, for debugging):
        // if (savedUser.getRole() == null) {
        //     System.err.println("!!! Role is null AFTER save for user: " + savedUser.getUsername());
        // }

        return savedUser; // Return the user saved by the repository
    }

    // Helper method to extract a tenant name (example implementation)
    private String extractTenantName(String email) {
        if (email != null && email.contains("@")) {
            String domain = email.substring(email.indexOf('@') + 1);
            return domain.split("\\.")[0]; // e.g., "example" from "user@example.com"
        }
        return "DefaultTenant"; // Fallback name
    }

}

