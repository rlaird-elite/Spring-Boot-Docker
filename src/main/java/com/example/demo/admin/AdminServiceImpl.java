package com.example.demo.admin;

import com.example.demo.permission.Permission;
import com.example.demo.permission.PermissionRepository;
import com.example.demo.user.User;
import com.example.demo.user.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    public AdminServiceImpl(UserRepository userRepository, PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
    }

    // Helper to get the currently authenticated user
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new IllegalStateException("User must be authenticated.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        } else {
            // This might happen if the principal is just a username string
            // In our setup, CustomUserDetailsService returns the full User object
            throw new IllegalStateException("Authentication principal is not the expected User object.");
        }
    }

    @Override
    @Transactional(readOnly = true) // This operation is read-only
    public List<User> listUsersInTenant() {
        Long adminTenantId = getCurrentUser().getTenantId();
        // Return all users that match the admin's tenantId
        return userRepository.findAllByTenantId(adminTenantId);
    }

    @Override
    @Transactional
    public User updateUserPermissions(Long userId, Set<String> permissionNames) {
        Long adminTenantId = getCurrentUser().getTenantId();

        // 1. Find the user to be updated.
        //    Ensure the user exists AND belongs to the admin's tenant.
        User userToUpdate = userRepository.findByIdAndTenantId(userId, adminTenantId)
                .orElseThrow(() -> new AccessDeniedException("User not found or not in your tenant."));

        // 2. Find the permission objects from the database
        //    This validates that the permissions actually exist.
        Set<Permission> newPermissions = permissionRepository.findByNameIn(permissionNames);

        // Optional: Check if all requested permission names were found
        if (newPermissions.size() != permissionNames.size()) {
            // This means the admin tried to assign a permission that doesn't exist
            // You could throw a custom exception here (e.g., PermissionNotFoundException)
            // For now, we'll just assign the ones we found.
            // Or, throw an error:
            throw new IllegalArgumentException("One or more permissions not found.");
        }

        // 3. Prevent an admin from stripping their own admin-management permission
        User adminUser = getCurrentUser();
        if (adminUser.getId().equals(userToUpdate.getId())) {
            // Check if the admin is trying to remove their own PERMISSION_MANAGE_USERS
            boolean isLosingAdminAccess = newPermissions.stream()
                    .noneMatch(p -> p.getName().equals("PERMISSION_MANAGE_USERS"));

            if(isLosingAdminAccess) {
                throw new AccessDeniedException("Admin cannot remove their own user management permission.");
            }
        }

        // 4. Set the new permissions and save the user
        userToUpdate.setPermissions(newPermissions);
        return userRepository.save(userToUpdate);
    }

    // We're missing a method in UserRepository, let's add it.
    // Go to UserRepository.java and add:
    // List<User> findAllByTenantId(Long tenantId);
    // Optional<User> findByIdAndTenantId(Long id, Long tenantId);
}
