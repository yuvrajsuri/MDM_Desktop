package com.company.mdm.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Command Entity - Simplified version
 * 
 * Stores whitelist configurations and other commands for devices
 * Each user on a device should have only ONE active command at a time
 */
@Entity
@Table(name = "desktop_commands", indexes = {
        @Index(name = "idx_commands_device_id", columnList = "device_id"),
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

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "command_type", nullable = false, length = 50)
    private String commandType;

    /**
     * Payload structure for GET_WHITELIST:
     * [
     * {
     * "user": "13667k",
     * "apps": ["notepad.exe", "chrome.exe"],
     * "urls": ["example.com", "github.com"]
     * },
     * {
     * "user": "27891a",
     * "apps": ["python.exe"],
     * "urls": ["stackoverflow.com"]
     * }
     * ]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> payload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;
}
