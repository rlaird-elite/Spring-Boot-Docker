package com.example.demo.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Fetch the User entity from the repository using the username (email)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with username: " + username));

        // The User entity itself now implements UserDetails correctly.
        // It maps its Set<Permission> to Collection<GrantedAuthority> internally.
        // So, we can just return the User object directly.
        return user;

        // --- OLD CODE (Removed) ---
        // No longer need to manually build UserDetails here.
        // return org.springframework.security.core.userdetails.User
        //         .withUsername(user.getUsername())
        //         .password(user.getPassword())
        //         .authorities(user.getAuthorities()) // User.getAuthorities() handles the mapping
        //         // Add account status flags if needed from User entity
        //         .accountExpired(!user.isAccountNonExpired())
        //         .accountLocked(!user.isAccountNonLocked())
        //         .credentialsExpired(!user.isCredentialsNonExpired())
        //         .disabled(!user.isEnabled())
        //         .build();
        // --- END OLD CODE ---
    }
}

