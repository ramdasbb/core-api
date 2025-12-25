package com.smartvillage.authservice.controller;

import com.smartvillage.authservice.dto.ApiResponse;
import com.smartvillage.authservice.entity.Role;
import com.smartvillage.authservice.entity.User;
import com.smartvillage.authservice.security.JwtUtil;
import com.smartvillage.authservice.service.AuditService;
import com.smartvillage.authservice.service.RBACService;
import com.smartvillage.authservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin Users", description = "Admin user management endpoints for approval, rejection, and user listing")
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
    @Operation(summary = "List Users", description = "List all active users with pagination and optional approval status filter")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiResponse<?>> listUsers(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @RequestParam(name = "approval_status", required = false) String approval_status) {
        try {
            // Validate token and check permission
            if (!validateAndCheckPermission(authHeader, "users:view")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>(false, "Permission denied", "PERMISSION_DENIED")
                );
            }

            Pageable pageable = PageRequest.of(page, limit);
            Page<User> usersPage;
            
            // Filter by approval status if provided
            if (approval_status != null && !approval_status.isEmpty()) {
                usersPage = userService.findByApprovalStatus(approval_status, pageable);
            } else {
                usersPage = userService.findAllActive(pageable);
            }
            
            // Build user list response
            List<Map<String, Object>> usersList = usersPage.getContent().stream()
                    .map(this::buildUserResponse)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("users", usersList);
            response.put("pagination", new LinkedHashMap<String, Object>() {{
                put("page", page);
                put("limit", limit);
                put("total", usersPage.getTotalElements());
                put("total_pages", usersPage.getTotalPages());
                put("has_next", usersPage.hasNext());
                put("has_previous", usersPage.hasPrevious());
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
    @Operation(summary = "Get User Details", description = "Retrieve details of a specific user by ID")
    @SecurityRequirement(name = "Bearer Authentication")
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
     * GET /admin/users/{userId}/approve - Approve a pending user
     */
    @GetMapping("/{userId}/approve")
    @Operation(
        summary = "Approve User",
        description = "Approve a pending user by their ID. Requires 'users:approve' permission. No request body needed."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "User approved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "Permission denied - user lacks 'users:approve' permission"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "User not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500", 
            description = "Internal server error"
        )
    })
    public ResponseEntity<ApiResponse<?>> approveUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable(name = "userId") 
            @Parameter(description = "UUID of the user to approve", example = "33d56a2e-ae9d-43b5-8a86-788378d12d2c")
            String userId) {
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
     * GET /admin/users/{userId}/reject - Reject a pending user
     */
    @GetMapping("/{userId}/reject")
    @Operation(
        summary = "Reject User",
        description = "Reject a pending user by their ID with optional rejection reason as query parameter. Requires 'users:reject' permission."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "User rejected successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "Permission denied - user lacks 'users:reject' permission"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "User not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500", 
            description = "Internal server error"
        )
    })
    public ResponseEntity<ApiResponse<?>> rejectUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable(name = "userId")
            @Parameter(description = "UUID of the user to reject", example = "33d56a2e-ae9d-43b5-8a86-788378d12d2c")
            String userId,
            @RequestParam(name = "reason", required = false)
            @Parameter(description = "Rejection reason (optional)", example = "Documents not verified")
            String rejectionReason) {
        try {
            // Validate token and check permission
            if (!validateAndCheckPermission(authHeader, "users:reject")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>(false, "Permission denied", "PERMISSION_DENIED")
                );
            }

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
    @Operation(summary = "Delete User", description = "Soft delete a user by marking as inactive")
    @SecurityRequirement(name = "Bearer Authentication")
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
