package com.smartvillage.authservice.controller;

import com.smartvillage.authservice.dto.AuthRequest;
import com.smartvillage.authservice.dto.AuthResponse;
import com.smartvillage.authservice.entity.User;
import com.smartvillage.authservice.security.JwtUtil;
import com.smartvillage.authservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody AuthRequest req) {
        User u = userService.register(req);
        String token = jwtUtil.generateToken(u.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, u.getEmail(), u.getName()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        return userService.findByEmail(req.getEmail())
                .map(u -> {
                    if (passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
                        String token = jwtUtil.generateToken(u.getEmail());
                        return ResponseEntity.ok(new AuthResponse(token, u.getEmail(), u.getName()));
                    }
                    return ResponseEntity.status(401).body("Invalid credentials");
                })
                .orElse(ResponseEntity.status(401).body("Invalid credentials"));
    }
}
