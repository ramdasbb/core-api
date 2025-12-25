package com.smartvillage.authservice.service;

import com.smartvillage.authservice.entity.AuditLog;
import com.smartvillage.authservice.entity.User;
import com.smartvillage.authservice.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logAction(User user, String action, String resourceType, String resourceId, String changes) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        
        // Convert changes to valid JSON format
        if (changes != null) {
            try {
                // Check if it's already valid JSON
                objectMapper.readTree(changes);
                log.setChanges(changes);
            } catch (Exception e) {
                // If not valid JSON, wrap it as a JSON string
                try {
                    log.setChanges(objectMapper.writeValueAsString(changes));
                } catch (Exception ex) {
                    // Fallback: set as null if serialization fails
                    log.setChanges(null);
                }
            }
        }
        
        log.setStatus("success");
        
        // Try to get IP and User-Agent from request context
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                log.setIpAddress(getClientIpAddress(request));
                log.setUserAgent(request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            // Ignore if no request context available
        }
        
        auditLogRepository.save(log);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle IPv6 with port
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
