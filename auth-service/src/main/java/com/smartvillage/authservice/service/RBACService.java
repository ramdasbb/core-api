package com.smartvillage.authservice.service;

import com.smartvillage.authservice.entity.Permission;
import com.smartvillage.authservice.entity.Role;
import com.smartvillage.authservice.entity.User;
import com.smartvillage.authservice.repository.PermissionRepository;
import com.smartvillage.authservice.repository.RoleRepository;
import com.smartvillage.authservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class RBACService {
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public RBACService(RoleRepository roleRepository, PermissionRepository permissionRepository,
                      UserRepository userRepository, AuditService auditService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // Permission Management
    public Permission createPermission(String name, String description) {
        if (permissionRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Permission already exists: " + name);
        }
        Permission permission = new Permission(name, description);
        Permission saved = permissionRepository.save(permission);
        auditService.logAction(null, "permission:create", "permission", saved.getId().toString(), name);
        return saved;
    }

    public Set<String> getPermissionsForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Set<String> permissions = new HashSet<>();
        
        // Check if super_admin
        boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(role -> "super_admin".equals(role.getName()));
        
        if (isSuperAdmin) {
            // Super admin gets all permissions
            return permissionRepository.findAll().stream()
                    .map(Permission::getName)
                    .collect(Collectors.toSet());
        }
        
        // Collect permissions from all user roles
        user.getRoles().forEach(role -> {
            role.getPermissions().forEach(perm -> {
                permissions.add(perm.getName());
            });
        });
        
        return permissions;
    }

    // Role Management
    public Role createRole(String name, String description, Boolean isSystemRole) {
        if (roleRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Role already exists: " + name);
        }
        Role role = new Role(name, description, isSystemRole);
        Role saved = roleRepository.save(role);
        auditService.logAction(null, "role:create", "role", saved.getId().toString(), name);
        return saved;
    }

    public void assignPermissionsToRole(UUID roleId, Set<UUID> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        Set<Permission> permissions = new HashSet<>();
        for (UUID permId : permissionIds) {
            Permission perm = permissionRepository.findById(permId)
                    .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permId));
            permissions.add(perm);
        }
        
        role.setPermissions(permissions);
        roleRepository.save(role);
        auditService.logAction(null, "role:assign-permissions", "role", roleId.toString(), null);
    }

    public void removePermissionFromRole(UUID roleId, UUID permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found"));
        
        role.getPermissions().remove(permission);
        roleRepository.save(role);
        auditService.logAction(null, "role:remove-permission", "role", roleId.toString(), null);
    }

    public boolean hasPermission(UUID userId, String permissionName) {
        Set<String> permissions = getPermissionsForUser(userId);
        return permissions.contains(permissionName);
    }

    public boolean isSuperAdmin(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        return user.getRoles().stream()
                .anyMatch(role -> "super_admin".equals(role.getName()));
    }
}
