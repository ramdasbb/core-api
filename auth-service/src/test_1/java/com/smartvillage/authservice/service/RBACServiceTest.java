package com.smartvillage.authservice.service;

import com.smartvillage.authservice.entity.Permission;
import com.smartvillage.authservice.entity.Role;
import com.smartvillage.authservice.entity.User;
import com.smartvillage.authservice.repository.PermissionRepository;
import com.smartvillage.authservice.repository.RoleRepository;
import com.smartvillage.authservice.repository.UserRepository;
import com.smartvillage.authservice.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RBACService Tests")
class RBACServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private RBACService rbacService;

    private Role superAdminRole;
    private Role userRole;
    private Permission usersViewPermission;
    private Permission usersCreatePermission;
    private User superAdminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup permissions
        usersViewPermission = new Permission("users:view", "View users");
        usersViewPermission.setId(UUID.randomUUID());

        usersCreatePermission = new Permission("users:create", "Create users");
        usersCreatePermission.setId(UUID.randomUUID());

        // Setup roles
        superAdminRole = new Role("super_admin", "System administrator", true);
        superAdminRole.setId(UUID.randomUUID());
        superAdminRole.setPermissions(new HashSet<>(Arrays.asList(usersViewPermission, usersCreatePermission)));

        userRole = new Role("user", "Regular user", true);
        userRole.setId(UUID.randomUUID());
        userRole.setPermissions(new HashSet<>(Collections.singletonList(usersViewPermission)));

        // Setup users
        superAdminUser = new User();
        superAdminUser.setId(UUID.randomUUID());
        superAdminUser.setEmail("admin@example.com");
        superAdminUser.setRoles(new HashSet<>(Collections.singletonList(superAdminRole)));
        superAdminUser.setApprovalStatus("approved");

        regularUser = new User();
        regularUser.setId(UUID.randomUUID());
        regularUser.setEmail("user@example.com");
        regularUser.setRoles(new HashSet<>(Collections.singletonList(userRole)));
        regularUser.setApprovalStatus("approved");
    }

    @Test
    @DisplayName("Should create permission successfully")
    void testCreatePermissionSuccess() {
        // Arrange
        String permissionName = "services:view";
        String description = "View services";
        when(permissionRepository.findByName(permissionName)).thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Permission createdPermission = rbacService.createPermission(permissionName, description);

        // Assert
        assertNotNull(createdPermission);
        assertEquals(permissionName, createdPermission.getName());
        assertEquals(description, createdPermission.getDescription());
        verify(permissionRepository, times(1)).save(any(Permission.class));
    }

    @Test
    @DisplayName("Should throw exception when permission already exists")
    void testCreatePermissionExists() {
        // Arrange
        when(permissionRepository.findByName("users:view")).thenReturn(Optional.of(usersViewPermission));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            rbacService.createPermission("users:view", "View users");
        });
    }

    @Test
    @DisplayName("Should get all permissions for user including from all roles")
    void testGetPermissionsForUser() {
        // Arrange
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));

        // Act
        Set<String> permissions = rbacService.getPermissionsForUser(regularUser.getId());

        // Assert
        assertNotNull(permissions);
        assertTrue(permissions.contains("users:view"));
        assertEquals(1, permissions.size());
    }

    @Test
    @DisplayName("Should return all permissions for super admin")
    void testGetPermissionsForSuperAdmin() {
        // Arrange
        List<Permission> allPermissions = Arrays.asList(usersViewPermission, usersCreatePermission);
        when(userRepository.findById(superAdminUser.getId())).thenReturn(Optional.of(superAdminUser));
        when(permissionRepository.findAll()).thenReturn(allPermissions);

        // Act
        Set<String> permissions = rbacService.getPermissionsForUser(superAdminUser.getId());

        // Assert
        assertNotNull(permissions);
        assertTrue(permissions.contains("users:view"));
        assertTrue(permissions.contains("users:create"));
    }

    @Test
    @DisplayName("Should create role successfully")
    void testCreateRoleSuccess() {
        // Arrange
        String roleName = "moderator";
        String description = "Content moderator";
        when(roleRepository.findByName(roleName)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Role createdRole = rbacService.createRole(roleName, description, false);

        // Assert
        assertNotNull(createdRole);
        assertEquals(roleName, createdRole.getName());
        assertEquals(description, createdRole.getDescription());
        assertFalse(createdRole.getIsSystemRole());
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    @DisplayName("Should throw exception when role already exists")
    void testCreateRoleExists() {
        // Arrange
        when(roleRepository.findByName("user")).thenReturn(Optional.of(userRole));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            rbacService.createRole("user", "Regular user", true);
        });
    }

    @Test
    @DisplayName("Should assign permissions to role")
    void testAssignPermissionsToRole() {
        // Arrange
        Set<UUID> permissionIds = new HashSet<>(Arrays.asList(usersViewPermission.getId(), usersCreatePermission.getId()));
        when(roleRepository.findById(userRole.getId())).thenReturn(Optional.of(userRole));
        when(permissionRepository.findById(usersViewPermission.getId())).thenReturn(Optional.of(usersViewPermission));
        when(permissionRepository.findById(usersCreatePermission.getId())).thenReturn(Optional.of(usersCreatePermission));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        rbacService.assignPermissionsToRole(userRole.getId(), permissionIds);

        // Assert
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    @DisplayName("Should check if user has permission")
    void testHasPermissionTrue() {
        // Arrange
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));

        // Act
        boolean hasPermission = rbacService.hasPermission(regularUser.getId(), "users:view");

        // Assert
        assertTrue(hasPermission);
    }

    @Test
    @DisplayName("Should return false when user doesn't have permission")
    void testHasPermissionFalse() {
        // Arrange
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));

        // Act
        boolean hasPermission = rbacService.hasPermission(regularUser.getId(), "users:create");

        // Assert
        assertFalse(hasPermission);
    }

    @Test
    @DisplayName("Should identify super admin correctly")
    void testIsSuperAdmin() {
        // Arrange
        when(userRepository.findById(superAdminUser.getId())).thenReturn(Optional.of(superAdminUser));

        // Act
        boolean isSuperAdmin = rbacService.isSuperAdmin(superAdminUser.getId());

        // Assert
        assertTrue(isSuperAdmin);
    }

    @Test
    @DisplayName("Should return false for non super admin user")
    void testIsNotSuperAdmin() {
        // Arrange
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));

        // Act
        boolean isSuperAdmin = rbacService.isSuperAdmin(regularUser.getId());

        // Assert
        assertFalse(isSuperAdmin);
    }

    @Test
    @DisplayName("Should remove permission from role")
    void testRemovePermissionFromRole() {
        // Arrange
        when(roleRepository.findById(userRole.getId())).thenReturn(Optional.of(userRole));
        when(permissionRepository.findById(usersViewPermission.getId())).thenReturn(Optional.of(usersViewPermission));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        rbacService.removePermissionFromRole(userRole.getId(), usersViewPermission.getId());

        // Assert
        verify(roleRepository, times(1)).save(any(Role.class));
    }
}
