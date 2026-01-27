package com.company.mdm.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Device Entity - Simplified version
 * 
 * Represents a desktop device in the MDM system.
 * 
 * Key Rules:
 * 1. fulluuid is UNIQUE and IMMUTABLE
 * 2. status = TRUE means device is active (can receive commands)
 * 3. Only ONE device per fulluuid can have status = TRUE
 */
@Entity
@Table(name = "desktop_devices", indexes = {
        @Index(name = "idx_devices_fulluuid", columnList = "fulluuid", unique = true),
        @Index(name = "idx_devices_uuid15", columnList = "uuid15"),
        @Index(name = "idx_devices_status", columnList = "status"),
        @Index(name = "idx_devices_token_hash", columnList = "token_hash")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Device Identity (IMMUTABLE)
    @Column(name = "fulluuid", nullable = false, unique = true, length = 36, updatable = false)
    private String fulluuid;

    @Column(name = "uuid15", nullable = false, length = 15)
    private String uuid15;

    // Device Metadata
    @Column(name = "computer_name", length = 255)
    private String computerName;

    @Column(name = "os_name", length = 100)
    private String osName;

    @Column(name = "os_version", length = 50)
    private String osVersion;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // Status (Boolean: true = active, false = inactive)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Boolean status = false;

    // PushToken (Device Authentication)
    @Column(name = "token_hash", length = 255)
    private String tokenHash;

    @Column(name = "pushtoken", length = 255)
    private String pushtoken;

    @Column(name = "token_issued_at")
    private LocalDateTime tokenIssuedAt;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_check_in")
    private LocalDateTime lastCheckIn;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Audit
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // =========================================================================
    // BUSINESS LOGIC METHODS
    // =========================================================================

    /**
     * Activate device - Issue pushToken
     */
    public void activate(String tokenHash, String pushtoken, LocalDateTime expiresAt) {
        this.status = true;
        this.tokenHash = tokenHash;
        this.pushtoken = pushtoken;
        this.tokenIssuedAt = LocalDateTime.now();
        this.tokenExpiresAt = expiresAt;
    }

    /**
     * Deactivate device
     */
    public void deactivate() {
        this.status = false;
    }

    /**
     * Update device metadata on check-in
     */
    public void updateCheckIn(String computerName, String osName, String osVersion, String ipAddress) {
        this.computerName = computerName;
        this.osName = osName;
        this.osVersion = osVersion;
        this.ipAddress = ipAddress;
        this.lastCheckIn = LocalDateTime.now();
    }

    /**
     * Check if pushToken is expired
     */
    public boolean isTokenExpired() {
        return this.tokenExpiresAt != null &&
                this.tokenExpiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Check if device is active
     */
    public boolean isActive() {
        return this.status != null && this.status;
    }
}
