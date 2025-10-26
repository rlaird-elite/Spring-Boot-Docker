package com.example.demo.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List; // Import List
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Finds a user by their username (email)
    Optional<User> findByUsername(String username);

    // --- NEW METHODS FOR ADMIN SERVICE ---

    /**
     * Finds all users belonging to a specific tenant.
     */
    List<User> findAllByTenantId(Long tenantId);

    /**
     * Finds a single user by their ID *and* tenant ID.
     * This ensures an admin from one tenant cannot access a user from another.
     */
    Optional<User> findByIdAndTenantId(Long id, Long tenantId);
}

