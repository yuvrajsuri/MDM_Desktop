package com.company.mdm.repository;

import com.company.mdm.domain.entity.AuditEventType;
import com.company.mdm.domain.entity.DeviceAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Device Audit Log Repository
 * 
 * Data access for audit trail
 */
@Repository
public interface DeviceAuditLogRepository extends JpaRepository<DeviceAuditLog, Long> {
    
    /**
     * Find audit logs for a specific device
     */
    Page<DeviceAuditLog> findByDeviceIdOrderByCreatedAtDesc(Long deviceId, Pageable pageable);
    
    /**
     * Find audit logs by fulluuid (includes failed enrollment attempts)
     */
    List<DeviceAuditLog> findByFulluuidOrderByCreatedAtDesc(String fulluuid);
    
    /**
     * Find audit logs by event type
     */
    List<DeviceAuditLog> findByEventType(AuditEventType eventType);
    
    /**
     * Find recent audit logs within time range
     */
    List<DeviceAuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
        LocalDateTime start, 
        LocalDateTime end
    );
    
    /**
     * Count failed enrollment attempts for a fulluuid
     */
    Long countByFulluuidAndEventType(String fulluuid, AuditEventType eventType);
}
