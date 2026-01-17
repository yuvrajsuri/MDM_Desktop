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
 * Device Entity - Represents a desktop device in the MDM system.
 * 
 * CRITICAL DESIGN RULES:
 * 1. Device identity (fulluuid) is IMMUTABLE after creation
 * 2. Status transitions are EXPLICIT and enforced
 * 3. PushToken never stored in plaintext (only hash)
 * 4. ENROLLED means token issued, ACTIVE means first check-in received
 */
@Entity
@Table(name = "devices", indexes = {
        @Index(name = "idx_devices_fulluuid", columnList = "fulluuid", unique = true),
        @Index(name = "idx_devices_uuid15", columnList = "uuid15"),
        @Index(name = "idx_devices_status", columnList = "status"),
        @Index(name = "idx_devices_token_hash", columnList = "token_hash"),
        @Index(name = "idx_devices_last_check_in", columnList = "last_check_in")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // IMMUTABLE DEVICE IDENTITY
    // =========================================================================

    @Column(name = "fulluuid", nullable = false, unique = true, length = 36, updatable = false)
    private String fulluuid; // IMMUTABLE - Set once, never changed

    @Column(name = "uuid15", nullable = false, length = 15, updatable = false)
    private String uuid15; // IMMUTABLE - Set once, never changed

    // =========================================================================
    // DEVICE METADATA (Mutable - updates on check-in)
    // =========================================================================

    @Column(name = "computer_name", length = 255)
    private String computerName;

    @Column(name = "os_name", length = 100)
    private String osName;

    @Column(name = "os_version", length = 50)
    private String osVersion;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // =========================================================================
    // STATUS MANAGEMENT
    // Status transitions:
    // PENDING_ENROLLMENT → ENROLLED (token issued)
    // ENROLLED → ACTIVE (first check-in)
    // ACTIVE → SUSPENDED (admin action)
    // ACTIVE → BLOCKED (admin action)
    // * → WIPED (admin action)
    // =========================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private DeviceStatus status = DeviceStatus.PENDING_ENROLLMENT;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = false;

    // =========================================================================
    // PUSHTOKEN MANAGEMENT (Device Authentication)
    // CRITICAL: PushToken is device authentication, NOT "just a carrier"
    // =========================================================================

    @Column(name = "token_hash", length = 255)
    private String tokenHash; // SHA-256 hash of pushToken (NEVER store plaintext)

    @Column(name = "token_issued_at")
    private LocalDateTime tokenIssuedAt;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    // =========================================================================
    // TIMESTAMPS (Audit trail)
    // =========================================================================

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt; // When token was first issued

    @Column(name = "last_check_in")
    private LocalDateTime lastCheckIn; // Last /status poll

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // =========================================================================
    // AUDIT FIELDS
    // =========================================================================

    @Column(name = "created_by", length = 100)
    private String createdBy; // Admin who pre-provisioned

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // =========================================================================
    // BUSINESS LOGIC METHODS
    // =========================================================================

    /**
     * Enroll device - Issue pushToken (transition to ENROLLED)
     * CRITICAL: This is the ONLY way to move from PENDING_ENROLLMENT to ENROLLED
     */
    public void enroll(String tokenHash, LocalDateTime expiresAt) {
        if (this.status != DeviceStatus.PENDING_ENROLLMENT) {
            throw new IllegalStateException(
                    "Cannot enroll device in status: " + this.status +
                            ". Must be PENDING_ENROLLMENT.");
        }

        this.status = DeviceStatus.ENROLLED;
        this.tokenHash = tokenHash;
        this.tokenIssuedAt = LocalDateTime.now();
        this.tokenExpiresAt = expiresAt;
        this.enrolledAt = LocalDateTime.now();
    }

    /**
     * Activate device - First check-in received (transition to ACTIVE)
     * CRITICAL: This is the ONLY way to move from ENROLLED to ACTIVE
     */
    public void activate() {
        if (this.status != DeviceStatus.ENROLLED) {
            throw new IllegalStateException(
                    "Cannot activate device in status: " + this.status +
                            ". Must be ENROLLED.");
        }

        this.isActive = true;
        this.lastCheckIn = LocalDateTime.now();
    }

    /**
     * Update device metadata and check-in timestamp
     * Called on every /status poll
     */
    public void updateCheckIn(String computerName, String osName, String osVersion, String ipAddress) {
        this.computerName = computerName;
        this.osName = osName;
        this.osVersion = osVersion;
        this.ipAddress = ipAddress;
        this.lastCheckIn = LocalDateTime.now();

        // Transition ENROLLED → ACTIVE on first check-in
        if (this.status == DeviceStatus.ENROLLED) {
            this.activate();
        }
    }

    /**
     * Block device (admin action)
     */
    public void block() {
        if (this.status == DeviceStatus.BLOCKED) {
            return; // Already blocked
        }
        this.status = DeviceStatus.BLOCKED;
        this.isActive = false;
    }

    /**
     * Suspend device (admin action)
     */
    public void suspend() {
        if (this.status != DeviceStatus.ENROLLED) {
            throw new IllegalStateException(
                    "Cannot suspend device in status: " + this.status +
                            ". Must be ENROLLED.");
        }
        this.status = DeviceStatus.SUSPENDED;
        this.isActive = false;
    }

    /**
     * Reactivate suspended device (admin action)
     */
    public void reactivate() {
        if (this.status != DeviceStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Cannot reactivate device in status: " + this.status +
                            ". Must be SUSPENDED.");
        }
        this.status = DeviceStatus.ENROLLED;
        this.isActive = true;
    }

    /**
     * Wipe device (admin action)
     */
    public void wipe() {
        this.status = DeviceStatus.WIPED;
        this.tokenHash = null; // Revoke pushToken
        this.tokenExpiresAt = null;
        this.isActive = false;
    }

    /**
     * Check if device can register/enroll
     */
    public boolean canEnroll() {
        return this.status == DeviceStatus.PENDING_ENROLLMENT;
    }

    /**
     * Check if device is blocked
     */
    public boolean isBlocked() {
        return this.status == DeviceStatus.BLOCKED;
    }

    /**
     * Check if device is operational (can check in)
     */
    public boolean isOperational() {
        return this.status == DeviceStatus.ENROLLED ||
                this.isActive == true;
    }

    /**
     * Check if pushToken is expired
     */
    public boolean isTokenExpired() {
        return this.tokenExpiresAt != null &&
                this.tokenExpiresAt.isBefore(LocalDateTime.now());
    }
}
