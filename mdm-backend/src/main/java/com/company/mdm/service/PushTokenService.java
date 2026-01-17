package com.company.mdm.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * PushToken Service - Device Authentication
 * 
 * CRITICAL DESIGN:
 * - PushToken IS device authentication (not "just a carrier")
 * - Generates cryptographically secure random tokens
 * - Stores only SHA-256 hash (never plaintext)
 * - Validates using constant-time comparison
 * 
 * This is separate from Admin JWT authentication.
 */
@Service
@Slf4j
public class PushTokenService {
    
    @Value("${mdm.pushtoken.expiration-days:365}")
    private int expirationDays;
    
    @Value("${mdm.pushtoken.token-length:64}")
    private int tokenLength;
    
    private final SecureRandom secureRandom;
    
    public PushTokenService() {
        try {
            this.secureRandom = SecureRandom.getInstanceStrong();
            log.info("PushTokenService initialized with strong SecureRandom");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SecureRandom", e);
        }
    }
    
    /**
     * Generate a cryptographically secure pushToken
     * 
     * @return 64-character hexadecimal string (32 bytes / 256 bits of entropy)
     */
    public String generatePushToken() {
        byte[] tokenBytes = new byte[tokenLength / 2]; // 64 hex chars = 32 bytes
        secureRandom.nextBytes(tokenBytes);
        
        String pushToken = bytesToHex(tokenBytes);
        
        log.debug("Generated pushToken: {}...", pushToken.substring(0, 8));
        return pushToken;
    }
    
    /**
     * Hash pushToken for storage
     * 
     * CRITICAL: NEVER store plaintext pushToken in database
     * 
     * @param pushToken The token to hash
     * @return SHA-256 hash as hexadecimal string
     */
    public String hashToken(String pushToken) {
        if (pushToken == null || pushToken.isEmpty()) {
            throw new IllegalArgumentException("pushToken cannot be null or empty");
        }
        
        String hash = DigestUtils.sha256Hex(pushToken);
        log.debug("Hashed pushToken: {}...", hash.substring(0, 8));
        return hash;
    }
    
    /**
     * Validate a pushToken against stored hash
     * 
     * Uses constant-time comparison to prevent timing attacks
     * 
     * @param pushToken The token from request header
     * @param storedHash The hash from database
     * @return true if valid
     */
    public boolean validateToken(String pushToken, String storedHash) {
        if (pushToken == null || storedHash == null) {
            log.warn("Validation failed: null pushToken or hash");
            return false;
        }
        
        if (pushToken.length() != tokenLength) {
            log.warn("Validation failed: invalid token length {}", pushToken.length());
            return false;
        }
        
        String computedHash = hashToken(pushToken);
        
        // Constant-time comparison to prevent timing attacks
        boolean isValid = MessageDigest.isEqual(
            computedHash.getBytes(StandardCharsets.UTF_8),
            storedHash.getBytes(StandardCharsets.UTF_8)
        );
        
        if (!isValid) {
            log.warn("Validation failed: hash mismatch");
        }
        
        return isValid;
    }
    
    /**
     * Check if token is expired
     * 
     * @param expiresAt Token expiration timestamp
     * @return true if expired
     */
    public boolean isTokenExpired(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            return true;
        }
        return expiresAt.isBefore(LocalDateTime.now());
    }
    
    /**
     * Calculate expiration timestamp
     * 
     * @return Timestamp when token should expire (configured days from now)
     */
    public LocalDateTime calculateExpirationTime() {
        return LocalDateTime.now().plusDays(expirationDays);
    }
    
    /**
     * Validate token format (basic format check)
     * 
     * @param pushToken Token to validate
     * @return true if format is valid
     */
    public boolean isValidFormat(String pushToken) {
        if (pushToken == null) {
            return false;
        }
        
        // Must be exactly 64 hexadecimal characters
        return pushToken.matches("^[0-9a-f]{64}$");
    }
    
    /**
     * Convert byte array to hexadecimal string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
