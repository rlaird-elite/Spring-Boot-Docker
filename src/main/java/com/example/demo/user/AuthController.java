package com.example.demo.user;

import com.example.demo.exception.UserAlreadyExistsException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map; // Import Map

@RestController
@RequestMapping("/api/auth") // Base path for authentication endpoints
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    // --- UserRepository dependency REMOVED ---

    // Updated Constructor (UserRepository removed)
    public AuthController(AuthService authService,
                          AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@Valid @RequestBody UserRegistrationRequest registrationRequest) {
        User registeredUser = authService.registerNewUser(registrationRequest);
        // Typically return 200 OK or 201 Created for registration
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        // Authenticate the user using Spring Security's AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // If authentication is successful, set it in the SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate the JWT token
        String jwt = jwtTokenProvider.generateToken(authentication);

        // Return the token in the response body
        return ResponseEntity.ok(new LoginResponse(jwt));
    }

    // Exception handler specific to this controller (optional, can be global)
    // Updated to return a JSON object (Map) instead of a String
    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT) // Set HTTP status to 409 Conflict
    public Map<String, String> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return Map.of("message", ex.getMessage());
    }
}

