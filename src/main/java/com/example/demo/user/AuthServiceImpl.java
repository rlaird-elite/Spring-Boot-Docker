package com.example.demo.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User register(UserRegistrationRequest request) {
        // 1. Check if user already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            // Throw the exception expected by the test
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User with this username already exists.");
        }

        // 2. Create the new User entity
        User newUser = new User();
        newUser.setUsername(request.getUsername());

        // 3. Encode the password before saving
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));

        // 4. Set Tenant ID and Role
        newUser.setTenantId(request.getTenantId());
        newUser.setRole(request.getRole());

        // 5. Save and return the new user
        return userRepository.save(newUser);
    }
}
