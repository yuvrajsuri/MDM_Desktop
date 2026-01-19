package com.company.mdm.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Command Entity - Represents commands sent to devices
 * 
 * Commands are created by admins and delivered to devices via /status endpoint
 */
@Entity
@Table(name = "desktop_commands", indexes = {
        @Index(name = "idx_commands_device_id", columnList = "device_id"),
        @Index(name = "idx_commands_status", columnList = "status"),
        @Index(name = "idx_commands_type", columnList = "command_type"),
        @Index(name = "idx_commands_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Command {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // DEVICE REFERENCE
    // =========================================================================

    @Column(name = "device_id", nullable = false)
    private Long deviceId; // Reference to devices table

    // =========================================================================
    // COMMAND DETAILS
    // =========================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", nullable = false, length = 50)
    private CommandType commandType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload; // Command-specific data

    // =========================================================================
    // COMMAND STATUS
    // =========================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private CommandStatus status = CommandStatus.PENDING;

    // =========================================================================
    // EXECUTION TRACKING
    // =========================================================================

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", columnDefinition = "jsonb")
    private Map<String, Object> result; // Execution result from device

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // Error details if failed

    // =========================================================================
    // TIMESTAMPS
    // =========================================================================

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt; // When device received command

    @Column(name = "executed_at")
    private LocalDateTime executedAt; // When device executed command

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // =========================================================================
    // METADATA
    // =========================================================================

    @Column(name = "created_by", length = 100)
    private String createdBy; // Admin who created command

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0; // Higher = more urgent (0 = normal)

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Optional expiration

    // =========================================================================
    // BUSINESS LOGIC
    // =========================================================================

    /**
     * Mark command as delivered to device
     */
    public void markDelivered() {
        if (this.status == CommandStatus.PENDING) {
            this.status = CommandStatus.DELIVERED;
            this.deliveredAt = LocalDateTime.now();
        }
    }

    /**
     * Mark command as successfully executed
     */
    public void markExecuted(Map<String, Object> result) {
        this.status = CommandStatus.EXECUTED;
        this.executedAt = LocalDateTime.now();
        this.result = result;
    }

    /**
     * Mark command as failed
     */
    public void markFailed(String errorMessage) {
        this.status = CommandStatus.FAILED;
        this.executedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    /**
     * Check if command is expired
     */
    public boolean isExpired() {
        return this.expiresAt != null && this.expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Check if command is pending delivery
     */
    public boolean isPending() {
        return this.status == CommandStatus.PENDING && !isExpired();
    }
}
