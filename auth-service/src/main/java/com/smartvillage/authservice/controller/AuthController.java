package com.smartvillage.authservice.controller;

import com.smartvillage.authservice.dto.*;
import com.smartvillage.authservice.entity.RefreshToken;
import com.smartvillage.authservice.entity.Role;
import com.smartvillage.authservice.entity.User;
import com.smartvillage.authservice.security.JwtUtil;
import com.smartvillage.authservice.service.AuthService;
import com.smartvillage.authservice.service.RBACService;
import com.smartvillage.authservice.service.UserService;
import com.smartvillage.authservice.service.AuditService;
import com.smartvillage.authservice.dto.PasswordResetRequest;
import com.smartvillage.authservice.dto.PasswordResetConfirmRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(
    name = "Authentication",
    description = "User authentication endpoints - login, signup, token refresh, and profile management"
)
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final RBACService rbacService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AuthController(UserService userService, AuthService authService, RBACService rbacService,
                        JwtUtil jwtUtil, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userService = userService;
        this.authService = authService;
        this.rbacService = rbacService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /**
     * POST /auth/request-password-reset - Request password reset (send token)
     */
    @PostMapping("/request-password-reset")
    public ResponseEntity<ApiResponse<?>> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, "Email is required", "INVALID_INPUT")
            );
        }
        Optional<User> userOpt = userService.findByEmail(request.getEmail().trim().toLowerCase());
        if (userOpt.isEmpty()) {
            // Don't reveal if user exists
            return ResponseEntity.ok(new ApiResponse<>(true, "If the email exists, a reset link will be sent.", null));
        }
        User user = userOpt.get();
        String token = UUID.randomUUID().toString().replace("-", "");
        user.setResetPasswordToken(token);
        user.setResetPasswordExpiry(java.time.Instant.now().plusSeconds(3600)); // 1 hour expiry
        userService.save(user);
        // TODO: Send email with token (stub)
        System.out.println("Password reset token for " + user.getEmail() + ": " + token);
        return ResponseEntity.ok(new ApiResponse<>(true, "If the email exists, a reset link will be sent.", null));
    }

    /**
     * POST /auth/reset-password - Reset password using token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@RequestBody PasswordResetConfirmRequest request) {
        if (request.getToken() == null || request.getToken().isEmpty() || request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, "Token and new password are required", "INVALID_INPUT")
            );
        }
        Optional<User> userOpt = userService.findByResetPasswordToken(request.getToken());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiResponse<>(false, "Invalid or expired token", "INVALID_TOKEN")
            );
        }
        User user = userOpt.get();
        if (user.getResetPasswordExpiry() == null || user.getResetPasswordExpiry().isBefore(java.time.Instant.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiResponse<>(false, "Token expired", "TOKEN_EXPIRED")
            );
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiry(null);
        userService.save(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Password reset successful", null));
    }

    /**
     * POST /auth/signup - Register new user
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<?>> signup(@RequestBody AuthRequest request) {
        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Email is required", "INVALID_INPUT")
                );
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Password is required", "INVALID_INPUT")
                );
            }

            // Register user
            User user = userService.register(request);
            
            Map<String, Object> userData = new LinkedHashMap<>();
            userData.put("id", user.getId().toString());
            userData.put("email", user.getEmail());
            userData.put("full_name", user.getFullName());
            userData.put("approval_status", user.getApprovalStatus());
            userData.put("roles", user.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
            userData.put("created_at", user.getCreatedAt());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(true, "User registered successfully. Awaiting approval.", userData)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiResponse<>(false, e.getMessage(), "EMAIL_EXISTS")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Registration failed: " + e.getMessage(), "SIGNUP_ERROR")
            );
        }
    }

    /**
     * POST /auth/login - Authenticate user and get access token
     */
    @Operation(
        summary = "User Login",
        description = "Authenticate a user with email and password. Returns access token and refresh token on successful authentication."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(schema = @Schema(example = """
                {
                  "success": true,
                  "message": "Login successful",
                  "data": {
                    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                    "expires_in": 900,
                    "user": {
                      "user_id": "550e8400-e29b-41d4-a716-446655440000",
                      "email": "superadmin@villageorbit.com",
                      "full_name": "Super Administrator",
                      "roles": ["super_admin"],
                      "permissions": ["all:*"]
                    }
                  }
                }
                """))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad request - Missing or invalid email/password"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid credentials or user not approved"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Login credentials (email and password only)",
            required = true,
            content = @Content(
                schema = @Schema(
                    type = "object",
                    example = """
                        {
                          "email": "superadmin@villageorbit.com",
                          "password": "SuperAdmin@123!"
                        }
                        """
                )
            )
        )
        @RequestBody LoginRequest request, 
        HttpServletRequest httpRequest) {
        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Email is required", "INVALID_INPUT")
                );
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Password is required", "INVALID_INPUT")
                );
            }

            // Find user
            Optional<User> userOpt = userService.findByEmail(request.getEmail());
            if (userOpt.isEmpty()) {
                auditService.logAction(null, "auth:login-failed", "user", null, "User not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiResponse<>(false, "Invalid email or password", "INVALID_CREDENTIALS")
                );
            }

            User user = userOpt.get();

            // Check approval status
            if (!"approved".equals(user.getApprovalStatus())) {
                auditService.logAction(user, "auth:login-failed", "user", user.getId().toString(), "User not approved");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiResponse<>(false, "User not yet approved. Contact administrator.", "USER_PENDING_APPROVAL")
                );
            }

            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                auditService.logAction(user, "auth:login-failed", "user", user.getId().toString(), "Invalid password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiResponse<>(false, "Invalid email or password", "INVALID_CREDENTIALS")
                );
            }

            // Generate tokens
            Set<String> permissions = rbacService.getPermissionsForUser(user.getId());
            String accessToken = jwtUtil.generateAccessToken(user.getId().toString(), permissions);
            RefreshToken refreshToken = authService.createRefreshToken(user);

            // Build response
            List<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId().toString(),
                user.getEmail(),
                user.getFullName(),
                user.getApprovalStatus()
            );
            userInfo.setRoles(roles);
            userInfo.setPermissions(permissions);

            AuthResponse.AuthData data = new AuthResponse.AuthData(
                accessToken,
                refreshToken.getToken(),
                jwtUtil.getExpirationTimeInSeconds(),
                userInfo
            );

            auditService.logAction(user, "auth:login", "user", user.getId().toString(), null);

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Login successful", data)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Login failed: " + e.getMessage(), "LOGIN_ERROR")
            );
        }
    }

    /**
     * POST /auth/logout - Logout user (revoke refresh token)
     */
    @PostMapping("/logout")
    @Operation(
        summary = "User Logout",
        description = "Logout the current user. Optionally pass refresh_token in request body to revoke it. If not provided, uses the token from Authorization header."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Logged out successfully",
            content = @Content(schema = @Schema(example = """
                {
                  "success": true,
                  "message": "Logged out successfully",
                  "data": null,
                  "error_code": null
                }
                """))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing authorization token"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<ApiResponse<?>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            String refreshToken = null;
            
            // Try to get refresh token from request body first
            if (request != null && request.containsKey("refresh_token")) {
                refreshToken = request.get("refresh_token");
            }
            
            // If refresh token is provided, revoke it
            if (refreshToken != null && !refreshToken.isEmpty()) {
                authService.revokeRefreshToken(refreshToken);
            }
            
            // Extract user ID from access token for audit logging
            String userId = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String accessToken = authHeader.substring(7).trim();
                if (jwtUtil.validateToken(accessToken)) {
                    userId = jwtUtil.getSubjectFromToken(accessToken);
                    // Log audit action without requiring full user object
                }
            }

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Logged out successfully", null)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Logout failed: " + e.getMessage(), "LOGOUT_ERROR")
            );
        }
    }

    /**
     * POST /auth/refresh-token - Issue new access token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<?>> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshTokenStr = request.get("refresh_token");
            if (refreshTokenStr == null || refreshTokenStr.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Refresh token is required", "INVALID_INPUT")
                );
            }

            // Validate refresh token
            Optional<RefreshToken> refreshTokenOpt = authService.validateRefreshToken(refreshTokenStr);
            if (refreshTokenOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiResponse<>(false, "Invalid or expired refresh token", "INVALID_REFRESH_TOKEN")
                );
            }

            RefreshToken refreshToken = refreshTokenOpt.get();
            User user = refreshToken.getUser();

            // Generate new access token
            Set<String> permissions = rbacService.getPermissionsForUser(user.getId());
            String newAccessToken = jwtUtil.generateAccessToken(user.getId().toString(), permissions);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("access_token", newAccessToken);
            data.put("token_type", "Bearer");
            data.put("expires_in", jwtUtil.getExpirationTimeInSeconds());

            auditService.logAction(user, "auth:token-refresh", "refresh_token", refreshToken.getId().toString(), null);

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Token refreshed successfully", data)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Token refresh failed: " + e.getMessage(), "REFRESH_ERROR")
            );
        }
    }

    /**
     * GET /auth/me - Get authenticated user profile
     */
    @GetMapping("/me")
    @Operation(summary = "Get User Profile", description = "Retrieve the profile of the authenticated user")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiResponse<?>> getProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            System.out.println("=== GET /auth/me Request ===");
            System.out.println("Authorization Header: " + (authHeader != null ? "Present" : "Missing"));
            
            if (authHeader == null || authHeader.trim().isEmpty()) {
                System.out.println("ERROR: Authorization header is null or empty");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiResponse<>(false, "Authorization header missing or empty", "UNAUTHORIZED")
                );
            }

            if (!authHeader.startsWith("Bearer ")) {
                System.out.println("ERROR: Invalid Authorization header format. Expected 'Bearer {token}'");
                System.out.println("Received: " + authHeader);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiResponse<>(false, "Invalid Authorization header format. Expected 'Bearer {token}'", "UNAUTHORIZED")
                );
            }

            String token = authHeader.substring(7).trim(); // Extract token after "Bearer "
            System.out.println("Token Length: " + token.length());
            System.out.println("Token Preview: " + token.substring(0, Math.min(50, token.length())) + "...");
            
            System.out.println("Validating token...");
            if (!jwtUtil.validateToken(token)) {
                System.out.println("ERROR: Token validation failed");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiResponse<>(false, "Invalid or expired token", "UNAUTHORIZED")
                );
            }

            System.out.println("Token validation successful");
            String userId = jwtUtil.getSubjectFromToken(token);
            System.out.println("User ID from token: " + userId);
            
            Optional<User> userOpt = userService.findById(UUID.fromString(userId));

            if (userOpt.isEmpty()) {
                System.out.println("ERROR: User not found with ID: " + userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ApiResponse<>(false, "User not found", "USER_NOT_FOUND")
                );
            }

            User user = userOpt.get();
            System.out.println("User found: " + user.getEmail());
            
            Set<String> permissions = rbacService.getPermissionsForUser(user.getId());
            System.out.println("Permissions fetched: " + permissions.size());

            UserProfileResponse profile = new UserProfileResponse();
            profile.setId(user.getId().toString());
            profile.setEmail(user.getEmail());
            profile.setFullName(user.getFullName());
            profile.setMobile(user.getMobile());
            profile.setApprovalStatus(user.getApprovalStatus());
            profile.setIsActive(user.getIsActive());
            profile.setCreatedAt(user.getCreatedAt());
            profile.setApprovedAt(user.getApprovedAt());
            
            if (user.getApprovedBy() != null) {
                profile.setApprovedByUserId(user.getApprovedBy().getId().toString());
            }

            List<UserProfileResponse.RoleInfo> roles = user.getRoles().stream()
                    .map(r -> new UserProfileResponse.RoleInfo(r.getId().toString(), r.getName(), r.getDescription()))
                    .collect(Collectors.toList());
            profile.setRoles(roles);
            profile.setPermissions(permissions);

            System.out.println("=== GET /auth/me Response SUCCESS ===");
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Profile retrieved successfully", profile)
            );

        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: Invalid UUID format: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiResponse<>(false, "Invalid user ID format", "INVALID_ID")
            );
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to retrieve profile: " + e.getMessage(), "PROFILE_ERROR")
            );
        }
    }
}
