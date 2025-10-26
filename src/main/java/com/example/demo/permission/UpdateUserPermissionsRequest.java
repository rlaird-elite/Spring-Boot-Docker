package com.example.demo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserPermissionsRequest {
    // A set of permission names, e.g., "PERMISSION_DELETE_VENDOR"
    private Set<String> permissionNames;
}
