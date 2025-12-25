package com.smartvillage.authservice.security;

import com.smartvillage.authservice.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    // Public endpoints that don't require JWT
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/health",
            "/api/v1/status",
            "/swagger-ui.html",
            "/swagger-ui/",
            "/v3/api-docs"
    );

    public JwtAuthenticationFilter(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        logger.debug("Processing request: {} {}", request.getMethod(), requestPath);

        // Skip JWT validation for public endpoints
        if (isPublicEndpoint(requestPath)) {
            logger.debug("Public endpoint, skipping JWT validation");
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        logger.debug("Authorization header: {}", authHeader != null ? "Present" : "Missing");

        try {
            if (authHeader == null || authHeader.trim().isEmpty()) {
                logger.warn("Missing Authorization header for protected endpoint: {}", requestPath);
                sendUnauthorizedError(response, "Missing Authorization header");
                return;
            }

            if (!authHeader.startsWith("Bearer ")) {
                logger.warn("Invalid Authorization header format. Expected 'Bearer {token}'");
                sendUnauthorizedError(response, "Invalid Authorization header format. Expected 'Bearer {token}'");
                return;
            }

            String token = authHeader.substring(7).trim(); // Extract token after "Bearer "
            
            if (token.isEmpty()) {
                logger.warn("Token is empty after 'Bearer' prefix");
                sendUnauthorizedError(response, "Token is empty");
                return;
            }

            logger.debug("Validating token...");
            if (!jwtUtil.validateToken(token)) {
                logger.warn("Invalid or expired token for endpoint: {}", requestPath);
                sendUnauthorizedError(response, "Invalid or expired token");
                return;
            }

            logger.debug("Token is valid, setting authentication in SecurityContext");
            
            // Extract subject (userId) from token
            String userId = jwtUtil.getSubjectFromToken(token);
            logger.debug("Extracted userId from token: {}", userId);
            
            // Create authentication token and set it in SecurityContextHolder
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            logger.debug("Authentication set in SecurityContext for userId: {}", userId);
            
            // Token is valid and authenticated, continue with request
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("JWT validation error: {}", e.getMessage(), e);
            sendUnauthorizedError(response, "Authentication failed: " + e.getMessage());
        }
    }

    private boolean isPublicEndpoint(String requestPath) {
        return PUBLIC_ENDPOINTS.stream()
                .anyMatch(requestPath::startsWith);
    }

    private void sendUnauthorizedError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        ApiResponse<?> errorResponse = new ApiResponse<>(false, message, "UNAUTHORIZED");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String requestPath = request.getRequestURI();
        return isPublicEndpoint(requestPath);
    }
}
