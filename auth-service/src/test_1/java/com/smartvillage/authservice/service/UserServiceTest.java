package com.smartvillage.authservice.service;

import com.smartvillage.authservice.dto.AuthRequest;
import com.smartvillage.authservice.entity.User;
import com.smartvillage.authservice.entity.Role;
import com.smartvillage.authservice.entity.Permission;
import com.smartvillage.authservice.repository.UserRepository;
import com.smartvillage.authservice.repository.RoleRepository;
import com.smartvillage.authservice.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private UserService userService;

    private BCryptPasswordEncoder passwordEncoder;
    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        passwordEncoder = new BCryptPasswordEncoder(12);

        // Setup test data
        userRole = new Role("user", "Regular user", false);
        userRole.setId(UUID.randomUUID());

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setMobile("9876543210");
        testUser.setAadharNumber("123456789012");
        testUser.setApprovalStatus("pending");
        testUser.setIsActive(true);
    }

    @Test
    @DisplayName("Should register user successfully with valid data")
    void testRegisterUserSuccess() {
        // Arrange
        AuthRequest request = new AuthRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("SecurePass123!");
        request.setFullName("New User");
        request.setMobile("9876543211");
        request.setAadharNumber("123456789013");

        when(userRepository.findByEmail(request.getEmail().toLowerCase())).thenReturn(Optional.empty());
        when(roleRepository.findByName("user")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User registeredUser = userService.register(request);

        // Assert
        assertNotNull(registeredUser);
        assertEquals(request.getEmail().toLowerCase(), registeredUser.getEmail());
        assertEquals(request.getFullName(), registeredUser.getFullName());
        assertEquals("pending", registeredUser.getApprovalStatus());
        assertTrue(registeredUser.getIsActive());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void testRegisterUserEmailExists() {
        // Arrange
        AuthRequest request = new AuthRequest();
        request.setEmail(testUser.getEmail());
        request.setPassword("password");
        request.setFullName("Test");
        request.setMobile("9876543210");
        request.setAadharNumber("123456789012");

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.register(request);
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should find user by email successfully")
    void testFindByEmailSuccess() {
        // Arrange
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> foundUser = userService.findByEmail(testUser.getEmail());

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals(testUser.getEmail(), foundUser.get().getEmail());
        verify(userRepository, times(1)).findByEmail(testUser.getEmail());
    }

    @Test
    @DisplayName("Should return empty when user not found")
    void testFindByEmailNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act
        Optional<User> foundUser = userService.findByEmail("nonexistent@example.com");

        // Assert
        assertFalse(foundUser.isPresent());
    }

    @Test
    @DisplayName("Should approve pending user successfully")
    void testApproveUserSuccess() {
        // Arrange
        UUID userId = testUser.getId();
        UUID approverId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(approverId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User approvedUser = userService.approveUser(userId, approverId);

        // Assert
        assertNotNull(approvedUser);
        assertEquals("approved", approvedUser.getApprovalStatus());
        assertNotNull(approvedUser.getApprovedAt());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject user successfully")
    void testRejectUserSuccess() {
        // Arrange
        UUID userId = testUser.getId();
        String rejectionReason = "Invalid aadhar number";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User rejectedUser = userService.rejectUser(userId, rejectionReason);

        // Assert
        assertNotNull(rejectedUser);
        assertEquals("rejected", rejectedUser.getApprovalStatus());
        assertFalse(rejectedUser.getIsActive());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should soft delete user")
    void testDeleteUserSoft() {
        // Arrange
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should assign roles to user")
    void testAssignRolesToUser() {
        // Arrange
        UUID userId = testUser.getId();
        Set<UUID> roleIds = Set.of(userRole.getId());

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findById(userRole.getId())).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.assignRolesToUser(userId, roleIds);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found for approval")
    void testApproveUserNotFound() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.approveUser(userId, UUID.randomUUID());
        });
    }

    @Test
    @DisplayName("Should throw exception when approver not found")
    void testApproveUserApproverNotFound() {
        // Arrange
        UUID userId = testUser.getId();
        UUID approverId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(approverId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.approveUser(userId, approverId);
        });
    }
}
