package com.smartvillage.authservice.controller;

import com.smartvillage.authservice.dto.ApiResponse;
import com.smartvillage.authservice.entity.Permission;
import com.smartvillage.authservice.entity.Role;
import com.smartvillage.authservice.entity.User;
import com.smartvillage.authservice.repository.PermissionRepository;
import com.smartvillage.authservice.repository.RoleRepository;
import com.smartvillage.authservice.security.JwtUtil;
import com.smartvillage.authservice.service.AuditService;
import com.smartvillage.authservice.service.RBACService;
import com.smartvillage.authservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/rbac")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RBACController {

    private final RBACService rbacService;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    public RBACController(RBACService rbacService, RoleRepository roleRepository,
                        PermissionRepository permissionRepository, UserService userService,
                        JwtUtil jwtUtil, AuditService auditService) {
        this.rbacService = rbacService;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
    }

    // ==================== PERMISSIONS ====================

    /**
     * POST /rbac/permissions - Create permission (Super Admin Only)
     */
    @PostMapping("/permissions")
    public ResponseEntity<ApiResponse<?>> createPermission(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        try {
            if (!validateSuperAdmin(authHeader)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>(false, "Permission denied", "PERMISSION_DENIED")
                );
            }

            String name = request.get("name");
            String description = request.get("description");

            if (name == null || name.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Permission name is required", "INVALID_INPUT")
                );
            }

            Permission permission = rbacService.createPermission(name, description);

            Map<String, Object> data = buildPermissionResponse(permission);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(true, "Permission created successfully", data)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiResponse<>(false, e.getMessage(), "PERMISSION_EXISTS")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to create permission: " + e.getMessage(), "CREATE_ERROR")
            );
        }
    }

    /**
     * GET /rbac/permissions - List all permissions
     */
    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<?>> listPermissions(@RequestHeader("Authorization") String authHeader) {
        try {
            if (!validateSuperAdmin(authHeader)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>(false, "Permission denied", "PERMISSION_DENIED")
                );
            }

            List<Map<String, Object>> permissions = permissionRepository.findAll().stream()
                    .map(this::buildPermissionResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Permissions retrieved successfully", permissions)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to retrieve permissions: " + e.getMessage(), "LIST_ERROR")
            );
        }
    }

    /**
     * DELETE /rbac/permissions/{permissionId} - Delete permission
     */
    @DeleteMapping("/permissions/{permissionId}")
    public ResponseEntity<ApiResponse<?>> deletePermission(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String permissionId) {
        try {
            if (!validateSuperAdmin(authHeader)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>(false, "Permission denied", "PERMISSION_DENIED")
                );
            }

            permissionRepository.deleteById(UUID.fromString(permissionId));
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Permission deleted successfully", null)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to delete permission: " + e.getMessage(), "DELETE_ERROR")
            );
        }
    }

    // ==================== ROLES ====================

    /**
     * POST /rbac/roles - Create role (Super Admin Only)
     */
    @PostMapping("/roles")
    public ResponseEntity<ApiResponse<?>> createRole(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        try {
            if (!validateSuperAdmin(authHeader)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>(false, "Permission denied", "PERMISSION_DENIED")
                );
            }

            String name = (String) request.get("name");
            String description = (String) request.get("description");
            Boolean isSystemRole = (Boolean) request.getOrDefault("is_system_role", false);

            if (name == null || name.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Role name is required", "INVALID_INPUT")
                );
            }

            Role role = rbacService.createRole(name, description, isSystemRole);

            Map<String, Object> data = buildRoleResponse(role);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(true, "Role created successfully", data)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiResponse<>(false, e.getMessage(), "ROLE_EXISTS")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to create role: " + e.getMessage(), "CREATE_ERROR")
            );
        }
    }

    /**
     * GET /rbac/roles - List all roles
     */
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<?>> listRoles(@RequestHeader("Authorization") String authHeader) {
        try {
            if (!validateSuperAdmin(authHeader)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>(false, "Permission denied", "PERMISSION_DENIED")
                );
            }

            List<Map<String, Object>> roles = roleRepository.findAll().stream()
                    .map(this::buildRoleResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Roles retrieved successfully", roles)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to retrieve roles: " + e.getMessage(), "LIST_ERROR")
            );
        }
    }

    /**
     * DELETE /rbac/roles/{roleId} - Delete role
     */
    @DeleteMapping("/roles/{roleId}")
    public ResponseEntity<ApiResponse<?>> deleteRole(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String roleId) {
        try {
            if (!validateSuperAdmin(authHeader)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>(false, "Permission denied", "PERMISSION_DENIED")
                );
            }

            roleRepository.deleteById(UUID.fromString(roleId));
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Role deleted successfully", null)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to delete role: " + e.getMessage(), "DELETE_ERROR")
            );
        }
    }

    // ==================== ROLE PERMISSIONS ====================

    /**
     * POST /rbac/roles/{roleId}/permissions - Assign permissions to role
     */
    @PostMapping("/roles/{roleId}/permissions")
    public ResponseEntity<ApiResponse<?>> assignPermissionsToRole(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String roleId,
            @RequestBody Map<String, Object> request) {
        try {
            if (!validateSuperAdmin(authHeader)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>(false, "Permission denied", "PERMISSION_DENIED")
                );
            }

            @SuppressWarnings("unchecked")
            List<String> permissionIds = (List<String>) request.get("permission_ids");

            if (permissionIds == null || permissionIds.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Permission IDs are required", "INVALID_INPUT")
                );
            }

            Set<UUID> permSet = permissionIds.stream().map(UUID::fromString).collect(Collectors.toSet());
            rbacService.assignPermissionsToRole(UUID.fromString(roleId), permSet);

            Role role = roleRepository.findById(UUID.fromString(roleId)).orElse(null);
            Map<String, Object> data = buildRoleResponse(role);

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Permissions assigned to role successfully", data)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to assign permissions: " + e.getMessage(), "ASSIGN_ERROR")
            );
        }
    }

    /**
     * DELETE /rbac/roles/{roleId}/permissions/{permissionId} - Remove permission from role
     */
    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    public ResponseEntity<ApiResponse<?>> removePermissionFromRole(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String roleId,
            @PathVariable String permissionId) {
        try {
            if (!validateSuperAdmin(authHeader)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>(false, "Permission denied", "PERMISSION_DENIED")
                );
            }

            rbacService.removePermissionFromRole(UUID.fromString(roleId), UUID.fromString(permissionId));
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Permission removed from role successfully", null)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to remove permission: " + e.getMessage(), "REMOVE_ERROR")
            );
        }
    }

    // ==================== USER ROLES ====================

    /**
     * POST /rbac/users/{userId}/roles - Assign roles to user
     */
    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<ApiResponse<?>> assignRolesToUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String userId,
            @RequestBody Map<String, Object> request) {
        try {
            if (!validateSuperAdmin(authHeader)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>(false, "Permission denied", "PERMISSION_DENIED")
                );
            }

            @SuppressWarnings("unchecked")
            List<String> roleIds = (List<String>) request.get("role_ids");

            if (roleIds == null || roleIds.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Role IDs are required", "INVALID_INPUT")
                );
            }

            Set<UUID> roleSet = roleIds.stream().map(UUID::fromString).collect(Collectors.toSet());
            userService.assignRolesToUser(UUID.fromString(userId), roleSet);

            User user = userService.findById(UUID.fromString(userId)).orElse(null);
            Set<String> permissions = rbacService.getPermissionsForUser(user.getId());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("user_id", user.getId().toString());
            data.put("email", user.getEmail());
            data.put("roles", user.getRoles().stream()
                    .map(r -> new LinkedHashMap<String, String>() {{
                        put("id", r.getId().toString());
                        put("name", r.getName());
                    }})
                    .collect(Collectors.toList()));
            data.put("all_permissions", permissions);

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Roles assigned to user successfully", data)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to assign roles: " + e.getMessage(), "ASSIGN_ERROR")
            );
        }
    }

    /**
     * DELETE /rbac/users/{userId}/roles/{roleId} - Remove role from user
     */
    @DeleteMapping("/users/{userId}/roles/{roleId}")
    public ResponseEntity<ApiResponse<?>> removeRoleFromUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String userId,
            @PathVariable String roleId) {
        try {
            if (!validateSuperAdmin(authHeader)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>(false, "Permission denied", "PERMISSION_DENIED")
                );
            }

            User user = userService.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Role role = roleRepository.findById(UUID.fromString(roleId))
                    .orElseThrow(() -> new IllegalArgumentException("Role not found"));

            user.getRoles().remove(role);
            userService.assignRolesToUser(user.getId(), user.getRoles().stream().map(Role::getId).collect(Collectors.toSet()));

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Role removed from user successfully", null)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to remove role: " + e.getMessage(), "REMOVE_ERROR")
            );
        }
    }

    // Helper methods

    private boolean validateSuperAdmin(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return false;
            }
            String token = authHeader.replace("Bearer ", "");
            if (!jwtUtil.validateToken(token)) {
                return false;
            }
            String userId = jwtUtil.getSubjectFromToken(token);
            return rbacService.isSuperAdmin(UUID.fromString(userId));
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> buildPermissionResponse(Permission permission) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", permission.getId().toString());
        data.put("name", permission.getName());
        data.put("description", permission.getDescription());
        data.put("created_at", permission.getCreatedAt());
        return data;
    }

    private Map<String, Object> buildRoleResponse(Role role) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", role.getId().toString());
        data.put("name", role.getName());
        data.put("description", role.getDescription());
        data.put("is_system_role", role.getIsSystemRole());
        data.put("permissions", role.getPermissions().stream()
                .map(p -> new LinkedHashMap<String, String>() {{
                    put("id", p.getId().toString());
                    put("name", p.getName());
                }})
                .collect(Collectors.toList()));
        data.put("created_at", role.getCreatedAt());
        return data;
    }
}
