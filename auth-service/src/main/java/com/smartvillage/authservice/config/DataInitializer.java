package com.smartvillage.authservice.config;

import com.smartvillage.authservice.entity.Role;
import com.smartvillage.authservice.entity.User;
import com.smartvillage.authservice.repository.RoleRepository;
import com.smartvillage.authservice.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

/**
 * DataInitializer - Bootstraps superadmin user on application startup.
 * 
 * Creates a superadmin account with the following credentials:
 * - Email: superadmin@villageorbit.com
 * - Password: SuperAdmin@123!
 * 
 * This ensures there's always an admin account available to approve users.
 */
@Configuration
public class DataInitializer {

    /**
     * Initializes the superadmin user if not already present.
     */
    @Bean
    public ApplicationRunner initializeData(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            String superadminEmail = "superadmin@villageorbit.com".toLowerCase().trim();
            
            // Check if superadmin already exists
            if (userRepository.findByEmail(superadminEmail).isPresent()) {
                System.out.println("ℹ️  Superadmin user already exists: " + superadminEmail);
                return;
            }

            System.out.println("🔧 Initializing superadmin user...");

            try {
                // Get the super_admin role
                Role superAdminRole = roleRepository.findByName("super_admin")
                        .orElseThrow(() -> new RuntimeException("super_admin role not found"));

                // Create superadmin user
                User superadmin = new User();
                superadmin.setEmail(superadminEmail);
                superadmin.setFullName("Super Administrator");
                superadmin.setMobile("9876543210");
                superadmin.setAadharNumber("123456789012");
                
                // Encode password
                String encodedPassword = passwordEncoder.encode("SuperAdmin@123!");
                superadmin.setPasswordHash(encodedPassword);
                
                // Set approval status to approved so it can login immediately
                superadmin.setApprovalStatus("approved");
                superadmin.setApprovedAt(Instant.now());
                superadmin.setApprovedBy(null); // Self-approved on initialization
                
                // Assign super_admin role
                superadmin.getRoles().add(superAdminRole);

                // Save the user
                User savedUser = userRepository.save(superadmin);

                System.out.println("✅ Superadmin created successfully!");
                System.out.println("   Email: " + savedUser.getEmail());
                System.out.println("   Role: super_admin");
                System.out.println("   Status: approved");
                System.out.println("");
                System.out.println("⚠️  IMPORTANT:");
                System.out.println("   Change the default password immediately in production!");
                System.out.println("   Default credentials are for development only.");
                System.out.println("");

            } catch (Exception e) {
                System.err.println("❌ Error initializing superadmin: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}
