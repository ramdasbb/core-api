package com.smartvillage.authservice.controller;

import com.smartvillage.authservice.dto.ApiResponse;
import com.smartvillage.authservice.dto.AuthRequest;
import com.smartvillage.authservice.dto.AuthResponse;
import com.smartvillage.authservice.dto.UserProfileResponse;
import com.smartvillage.authservice.entity.RefreshToken;
import com.smartvillage.authservice.entity.Role;
import com.smartvillage.authservice.entity.User;
import com.smartvillage.authservice.security.JwtUtil;
import com.smartvillage.authservice.service.AuthService;
import com.smartvillage.authservice.service.RBACService;
import com.smartvillage.authservice.service.UserService;
import com.smartvillage.authservice.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

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
     * POST /auth/login - Authenticate user
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
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
     * POST /auth/logout - Revoke refresh token
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(@RequestHeader("Authorization") String authHeader,
                                                  @RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refresh_token");
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Refresh token is required", "INVALID_INPUT")
                );
            }

            authService.revokeRefreshToken(refreshToken);

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
    public ResponseEntity<ApiResponse<?>> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiResponse<>(false, "Authorization header missing", "UNAUTHORIZED")
                );
            }

            String token = authHeader.replace("Bearer ", "");
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiResponse<>(false, "Invalid or expired token", "UNAUTHORIZED")
                );
            }

            String userId = jwtUtil.getSubjectFromToken(token);
            Optional<User> userOpt = userService.findById(UUID.fromString(userId));

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ApiResponse<>(false, "User not found", "USER_NOT_FOUND")
                );
            }

            User user = userOpt.get();
            Set<String> permissions = rbacService.getPermissionsForUser(user.getId());

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

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Profile retrieved successfully", profile)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to retrieve profile: " + e.getMessage(), "PROFILE_ERROR")
            );
        }
    }
}
