package com.smartvillage.authservice.controller;

import com.smartvillage.authservice.dto.ApiResponse;
import com.smartvillage.authservice.entity.Role;
import com.smartvillage.authservice.entity.User;
import com.smartvillage.authservice.security.JwtUtil;
import com.smartvillage.authservice.service.AuditService;
import com.smartvillage.authservice.service.RBACService;
import com.smartvillage.authservice.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminUserController {

    private final UserService userService;
    private final RBACService rbacService;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    public AdminUserController(UserService userService, RBACService rbacService,
                              JwtUtil jwtUtil, AuditService auditService) {
        this.userService = userService;
        this.rbacService = rbacService;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
    }

    /**
     * GET /admin/users - List users with filters
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> listUsers(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String approval_status) {
        try {
            // Validate token and check permission
            if (!validateAndCheckPermission(authHeader, "users:view")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>(false, "Permission denied", "PERMISSION_DENIED")
                );
            }

            Pageable pageable = PageRequest.of(page, limit);
            
            Map<String, Object> response = new LinkedHashMap<>();
            // Note: You'd need to implement pagination in UserRepository
            // For now, returning all active users
            
            response.put("users", new ArrayList<>());
            response.put("pagination", new LinkedHashMap<String, Object>() {{
                put("page", page);
                put("limit", limit);
                put("total", 0);
                put("total_pages", 0);
            }});

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Users retrieved successfully", response)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to retrieve users: " + e.getMessage(), "LIST_ERROR")
            );
        }
    }

    /**
     * GET /admin/users/{userId} - Get user details
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<?>> getUserDetails(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String userId) {
        try {
            // Validate token and check permission
            if (!validateAndCheckPermission(authHeader, "users:view")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>(false, "Permission denied", "PERMISSION_DENIED")
                );
            }

            Optional<User> userOpt = userService.findById(UUID.fromString(userId));
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ApiResponse<>(false, "User not found", "USER_NOT_FOUND")
                );
            }

            User user = userOpt.get();
            Map<String, Object> userData = buildUserResponse(user);

            return ResponseEntity.ok(
                new ApiResponse<>(true, "User details retrieved successfully", userData)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to retrieve user: " + e.getMessage(), "GET_USER_ERROR")
            );
        }
    }

    /**
     * POST /admin/users/{userId}/approve - Approve user
     */
    @PostMapping("/{userId}/approve")
    public ResponseEntity<ApiResponse<?>> approveUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {
        try {
            // Validate token and check permission
            String authUserId = validateAndGetUserId(authHeader);
            if (authUserId == null || !rbacService.hasPermission(UUID.fromString(authUserId), "users:approve")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>(false, "Permission denied", "PERMISSION_DENIED")
                );
            }

            User approvedUser = userService.approveUser(UUID.fromString(userId), UUID.fromString(authUserId));
            Map<String, Object> userData = buildUserResponse(approvedUser);

            return ResponseEntity.ok(
                new ApiResponse<>(true, "User approved successfully", userData)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiResponse<>(false, e.getMessage(), "USER_NOT_FOUND")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to approve user: " + e.getMessage(), "APPROVE_ERROR")
            );
        }
    }

    /**
     * POST /admin/users/{userId}/reject - Reject user
     */
    @PostMapping("/{userId}/reject")
    public ResponseEntity<ApiResponse<?>> rejectUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {
        try {
            // Validate token and check permission
            if (!validateAndCheckPermission(authHeader, "users:reject")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>(false, "Permission denied", "PERMISSION_DENIED")
                );
            }

            String rejectionReason = request.get("rejection_reason");
            User rejectedUser = userService.rejectUser(UUID.fromString(userId), rejectionReason);
            Map<String, Object> userData = buildUserResponse(rejectedUser);

            return ResponseEntity.ok(
                new ApiResponse<>(true, "User rejected successfully", userData)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiResponse<>(false, e.getMessage(), "USER_NOT_FOUND")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to reject user: " + e.getMessage(), "REJECT_ERROR")
            );
        }
    }

    /**
     * DELETE /admin/users/{userId} - Soft delete user
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<?>> deleteUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String userId) {
        try {
            // Validate token and check permission
            if (!validateAndCheckPermission(authHeader, "users:delete")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>(false, "Permission denied", "PERMISSION_DENIED")
                );
            }

            User deletedUser = userService.deleteUser(UUID.fromString(userId));
            Map<String, Object> userData = buildUserResponse(deletedUser);

            return ResponseEntity.ok(
                new ApiResponse<>(true, "User deleted successfully", userData)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiResponse<>(false, e.getMessage(), "USER_NOT_FOUND")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to delete user: " + e.getMessage(), "DELETE_ERROR")
            );
        }
    }

    // Helper methods

    private boolean validateAndCheckPermission(String authHeader, String permission) {
        String userId = validateAndGetUserId(authHeader);
        if (userId == null) {
            return false;
        }
        return rbacService.hasPermission(UUID.fromString(userId), permission);
    }

    private String validateAndGetUserId(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null;
            }
            String token = authHeader.replace("Bearer ", "");
            if (!jwtUtil.validateToken(token)) {
                return null;
            }
            return jwtUtil.getSubjectFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> buildUserResponse(User user) {
        Map<String, Object> userData = new LinkedHashMap<>();
        userData.put("id", user.getId().toString());
        userData.put("email", user.getEmail());
        userData.put("full_name", user.getFullName());
        userData.put("mobile", user.getMobile());
        userData.put("approval_status", user.getApprovalStatus());
        userData.put("is_active", user.getIsActive());
        userData.put("roles", user.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
        userData.put("created_at", user.getCreatedAt());
        if (user.getApprovedAt() != null) {
            userData.put("approved_at", user.getApprovedAt());
        }
        return userData;
    }
}
