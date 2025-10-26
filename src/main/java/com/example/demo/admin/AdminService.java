package com.example.demo.admin;

import com.example.demo.user.User;
import java.util.List;
import java.util.Set;

public interface AdminService {
    /**
     * Lists all users within the calling admin's tenant.
     * Requires PERMISSION_MANAGE_USERS.
     */
    List<User> listUsersInTenant();

    /**
     * Updates the permissions for a specific user within the admin's tenant.
     * Requires PERMISSION_MANAGE_USERS.
     */
    User updateUserPermissions(Long userId, Set<String> permissionNames);
}
