package com.smartvillage.authservice.service;

import com.smartvillage.authservice.entity.RefreshToken;
import com.smartvillage.authservice.entity.User;
import com.smartvillage.authservice.repository.RefreshTokenRepository;
import com.smartvillage.authservice.repository.UserRepository;
import com.smartvillage.authservice.security.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    public AuthService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository,
                      JwtUtil jwtUtil, AuditService auditService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
    }

    public RefreshToken createRefreshToken(User user) {
        String token = jwtUtil.generateRefreshToken(user.getId().toString());
        Instant expiresAt = Instant.now().plusSeconds(7 * 24 * 60 * 60); // 7 days
        
        RefreshToken refreshToken = new RefreshToken(user, token, expiresAt);
        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        
        auditService.logAction(user, "auth:refresh-token-created", "refresh_token", saved.getId().toString(), null);
        
        return saved;
    }

    public Optional<RefreshToken> validateRefreshToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        
        if (refreshToken.isEmpty()) {
            return Optional.empty();
        }
        
        RefreshToken rt = refreshToken.get();
        
        // Check if token is revoked or expired
        if (rt.getRevoked() || rt.getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        
        return refreshToken;
    }

    public void revokeRefreshToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        
        if (refreshToken.isPresent()) {
            RefreshToken rt = refreshToken.get();
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            
            auditService.logAction(rt.getUser(), "auth:refresh-token-revoked", "refresh_token", rt.getId().toString(), null);
        }
    }

    public void revokeUserRefreshTokens(UUID userId) {
        refreshTokenRepository.deleteByUserIdAndRevoked(userId, false);
    }
}
