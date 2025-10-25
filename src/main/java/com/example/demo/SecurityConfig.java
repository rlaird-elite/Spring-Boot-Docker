package com.example.demo;

import com.example.demo.user.CustomUserDetailsService;
import com.example.demo.user.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * This is the central configuration for all security in the application.
 * It enables web security, configures the JWT filter, and sets up authorization rules.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PasswordEncoder passwordEncoder;

    // We inject all our custom security components
    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          PasswordEncoder passwordEncoder) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Exposes the AuthenticationManager as a Bean.
     * This is required for our AuthController to perform the login operation.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Defines the main AuthenticationProvider.
     * This tells Spring Security to use our CustomUserDetailsService for finding users
     * and the BCryptPasswordEncoder for checking passwords.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * This is the main security filter chain that protects our API.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (Cross-Site Request Forgery)
                // This is safe because we are using stateless JWT, not cookies/sessions
                .csrf(csrf -> csrf.disable())

                // Set session management to STATELESS. Spring Security will not create any HttpSession.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure authorization rules for all HTTP requests
                .authorizeHttpRequests(auth -> auth
                        // Allow all requests to our authentication endpoints (login and register)
                        .requestMatchers("/api/auth/**").permitAll()
                        // All other requests (e.g., /api/properties) must be authenticated
                        .anyRequest().authenticated()
                )

                // Set our custom authentication provider
                .authenticationProvider(authenticationProvider())

                // Add our custom JWT filter *before* the standard UsernamePasswordAuthenticationFilter
                // This is what intercepts the request, validates the token, and sets the user.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

