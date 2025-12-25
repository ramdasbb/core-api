package com.smartvillage.authservice.controller;

import com.smartvillage.authservice.AuthServiceApplication;
import com.smartvillage.authservice.entity.User;
import com.smartvillage.authservice.repository.UserRepository;
import com.smartvillage.authservice.repository.RoleRepository;
import com.smartvillage.authservice.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(classes = AuthServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Auth Controller Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String testAccessToken;
    private String testRefreshToken;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /auth/signup - Should register user successfully")
    @Transactional
    void testSignupSuccess() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("email", "newuser@example.com");
        request.put("password", "SecurePass123!");
        request.put("full_name", "Test User");
        request.put("mobile", "9876543210");
        request.put("aadhar_number", "123456789012");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("registered")))
                .andExpect(jsonPath("$.data.email", is("newuser@example.com")))
                .andExpect(jsonPath("$.data.approval_status", is("pending")));
    }

    @Test
    @DisplayName("POST /auth/signup - Should fail with duplicate email")
    @Transactional
    void testSignupDuplicateEmail() throws Exception {
        // First signup
        Map<String, String> firstRequest = new HashMap<>();
        firstRequest.put("email", "duplicate@example.com");
        firstRequest.put("password", "SecurePass123!");
        firstRequest.put("full_name", "Test User");
        firstRequest.put("mobile", "9876543210");
        firstRequest.put("aadhar_number", "123456789012");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // Second signup with same email
        Map<String, String> secondRequest = new HashMap<>();
        secondRequest.put("email", "duplicate@example.com");
        secondRequest.put("password", "AnotherPass123!");
        secondRequest.put("full_name", "Another User");
        secondRequest.put("mobile", "9876543211");
        secondRequest.put("aadhar_number", "123456789013");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error_code", is("EMAIL_EXISTS")));
    }

    @Test
    @DisplayName("POST /auth/login - Should login and return tokens")
    @Transactional
    void testLoginSuccess() throws Exception {
        // First signup
        Map<String, String> signupRequest = new HashMap<>();
        signupRequest.put("email", "logintest@example.com");
        signupRequest.put("password", "SecurePass123!");
        signupRequest.put("full_name", "Test User");
        signupRequest.put("mobile", "9876543210");
        signupRequest.put("aadhar_number", "123456789012");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        // Approve the user
        User user = userRepository.findByEmail("logintest@example.com").orElse(null);
        assert user != null;
        user.setApprovalStatus("approved");
        userRepository.save(user);

        // Now login
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "logintest@example.com");
        loginRequest.put("password", "SecurePass123!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.access_token", notNullValue()))
                .andExpect(jsonPath("$.data.refresh_token", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is("logintest@example.com")));
    }

    @Test
    @DisplayName("POST /auth/login - Should fail with unapproved user")
    @Transactional
    void testLoginUnapprovedUser() throws Exception {
        // Signup
        Map<String, String> signupRequest = new HashMap<>();
        signupRequest.put("email", "unapproved@example.com");
        signupRequest.put("password", "SecurePass123!");
        signupRequest.put("full_name", "Test User");
        signupRequest.put("mobile", "9876543210");
        signupRequest.put("aadhar_number", "123456789012");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        // Try to login without approval
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "unapproved@example.com");
        loginRequest.put("password", "SecurePass123!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error_code", is("USER_NOT_APPROVED")));
    }

    @Test
    @DisplayName("GET /auth/me - Should return user profile with valid token")
    @Transactional
    void testGetProfileSuccess() throws Exception {
        // Setup: Create and approve user
        Map<String, String> signupRequest = new HashMap<>();
        signupRequest.put("email", "profile@example.com");
        signupRequest.put("password", "SecurePass123!");
        signupRequest.put("full_name", "Profile User");
        signupRequest.put("mobile", "9876543210");
        signupRequest.put("aadhar_number", "123456789012");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("profile@example.com").orElse(null);
        assert user != null;
        user.setApprovalStatus("approved");
        userRepository.save(user);

        String token = jwtUtil.generateAccessToken(user.getId().toString(), new HashSet<>());

        // Test get profile
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("profile@example.com")))
                .andExpect(jsonPath("$.data.full_name", is("Profile User")));
    }

    @Test
    @DisplayName("GET /auth/me - Should fail without token")
    void testGetProfileNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("POST /auth/logout - Should revoke refresh token")
    @Transactional
    void testLogoutSuccess() throws Exception {
        // Create user and get tokens
        Map<String, String> signupRequest = new HashMap<>();
        signupRequest.put("email", "logout@example.com");
        signupRequest.put("password", "SecurePass123!");
        signupRequest.put("full_name", "Logout User");
        signupRequest.put("mobile", "9876543210");
        signupRequest.put("aadhar_number", "123456789012");

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("logout@example.com").orElse(null);
        assert user != null;
        user.setApprovalStatus("approved");
        userRepository.save(user);

        String refreshToken = jwtUtil.generateRefreshToken(user.getId().toString());

        // Logout
        Map<String, String> logoutRequest = new HashMap<>();
        logoutRequest.put("refresh_token", refreshToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}
