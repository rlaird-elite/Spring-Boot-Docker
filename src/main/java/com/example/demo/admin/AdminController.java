package com.example.demo.admin;

import com.example.demo.user.UpdateUserPermissionsRequest;
import com.example.demo.user.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
// Secure the entire controller. Only users with this permission can access any endpoint here.
@PreAuthorize("hasAuthority('PERMISSION_MANAGE_USERS')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Gets a list of all users within the admin's own tenant.
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsersInTenant() {
        List<User> users = adminService.listUsersInTenant();
        return ResponseEntity.ok(users);
    }

    /**
     * Updates the permissions for a specific user within the admin's tenant.
     */
    @PutMapping("/users/{userId}/permissions")
    public ResponseEntity<User> updateUserPermissions(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserPermissionsRequest request) {

        User updatedUser = adminService.updateUserPermissions(userId, request.getPermissionNames());
        return ResponseEntity.ok(updatedUser);
    }

    // Note: We've imported GlobalExceptionHandler in the test, so any
    // exceptions (like UserNotFound or AccessDenied if a user tries to
    // update someone in *another* tenant) will be handled.
}
