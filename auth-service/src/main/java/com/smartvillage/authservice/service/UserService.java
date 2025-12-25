package com.smartvillage.authservice.service;

import com.smartvillage.authservice.dto.AuthRequest;
import com.smartvillage.authservice.entity.User;
import com.smartvillage.authservice.entity.Role;
import com.smartvillage.authservice.repository.UserRepository;
import com.smartvillage.authservice.repository.RoleRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, 
                      PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public User register(AuthRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        
        // Validate input
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (req.getPassword() == null || req.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        
        // Check if email already exists
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }
        
        // Create new user
        User user = new User();
        user.setEmail(email);
        user.setFullName(req.getFullName() != null ? req.getFullName() : email);
        user.setMobile(req.getMobile());
        user.setAadharNumber(req.getAadharNumber());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setApprovalStatus("pending");
        user.setIsActive(true);
        
        // Assign default 'user' role
        Role userRole = roleRepository.findByName("user")
                .orElseThrow(() -> new RuntimeException("Default 'user' role not found"));
        user.getRoles().add(userRole);
        
        User savedUser = userRepository.save(user);
        
        // Log signup action
        auditService.logAction(null, "user:signup", "user", savedUser.getId().toString(), "Signup");
        
        return savedUser;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    public Optional<User> findByResetPasswordToken(String token) {
        return userRepository.findAll().stream().filter(u -> token.equals(u.getResetPasswordToken())).findFirst();
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findById(UUID userId) {
        return userRepository.findById(userId);
    }

    public User approveUser(UUID userId, UUID approvedByUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        User approver = userRepository.findById(approvedByUserId)
                .orElseThrow(() -> new IllegalArgumentException("Approver not found"));
        
        user.setApprovalStatus("approved");
        user.setApprovedBy(approver);
        user.setApprovedAt(Instant.now());
        
        User saved = userRepository.save(user);
        auditService.logAction(approver, "user:approve", "user", userId.toString(), null);
        
        return saved;
    }

    public User rejectUser(UUID userId, String rejectionReason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setApprovalStatus("rejected");
        user.setRejectionReason(rejectionReason);
        
        User saved = userRepository.save(user);
        auditService.logAction(null, "user:reject", "user", userId.toString(), null);
        
        return saved;
    }

    public User deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setIsActive(false);
        User saved = userRepository.save(user);
        auditService.logAction(null, "user:delete", "user", userId.toString(), null);
        
        return saved;
    }

    public void assignRolesToUser(UUID userId, Set<UUID> roleIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Set<Role> roles = new HashSet<>();
        for (UUID roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
            roles.add(role);
        }
        
        user.setRoles(roles);
        userRepository.save(user);
        auditService.logAction(null, "rbac:user-roles-assign", "user", userId.toString(), null);
    }

    public Page<User> findByApprovalStatus(String status, Pageable pageable) {
        return userRepository.findByApprovalStatus(status, pageable);
    }

    public Page<User> findAllActive(Pageable pageable) {
        return userRepository.findAllActive(pageable);
    }
}
