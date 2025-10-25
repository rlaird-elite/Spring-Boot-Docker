package com.example.demo.user;

import com.example.demo.exception.UserAlreadyExistsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import this

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional // Add Transactional to ensure save operation is atomic
    public User registerNewUser(UserRegistrationRequest registrationRequest) {
        // 1. Check if user already exists
        Optional<User> existingUser = userRepository.findByUsername(registrationRequest.getUsername());
        if (existingUser.isPresent()) {
            throw new UserAlreadyExistsException("User with email " + registrationRequest.getUsername() + " already exists.");
        }

        // 2. Create new user entity
        User newUser = new User();
        newUser.setUsername(registrationRequest.getUsername());
        newUser.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));

        // --- THIS IS THE FIX ---
        // Assign a default Tenant ID for now. We'll replace 1L later
        // when we have a proper multi-tenancy implementation.
        newUser.setTenantId(1L);
        // --- END OF FIX ---

        // Assign a default role (e.g., USER) - ensure the Role enum exists in User.java
        newUser.setRole(User.Role.USER); // Assuming USER role exists

        // 3. Save the new user
        return userRepository.save(newUser);
    }

    // Login logic would typically be handled by Spring Security's AuthenticationManager,
    // not directly in this service, but you could add helper methods if needed.
}

