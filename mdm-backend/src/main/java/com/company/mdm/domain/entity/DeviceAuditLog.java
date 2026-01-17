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
import java.util.Map;

/**
 * Device Audit Log Entity
 * 
 * Automatic audit trail for all device events.
 * Every state change, enrollment attempt, and check-in is logged.
 */
@Entity
@Table(name = "device_audit_log", indexes = {
    @Index(name = "idx_audit_device_id", columnList = "device_id"),
    @Index(name = "idx_audit_event_type", columnList = "event_type"),
    @Index(name = "idx_audit_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceAuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "device_id")
    private Long deviceId;  // Nullable for failed enrollment attempts
    
    @Column(name = "fulluuid", length = 36)
    private String fulluuid;  // Always present (even for non-existent devices)
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuditEventType eventType;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", columnDefinition = "jsonb")
    private Map<String, Object> eventData;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", length = 50)
    private ActorType actorType;
    
    @Column(name = "actor_id", length = 100)
    private String actorId;  // Device fulluuid or admin email
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
