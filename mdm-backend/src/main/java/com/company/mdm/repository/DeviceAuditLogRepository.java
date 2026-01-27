package com.company.mdm.repository;

import com.company.mdm.domain.entity.DeviceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Device Audit Log Repository - Simplified
 */
@Repository
public interface DeviceAuditLogRepository extends JpaRepository<DeviceAuditLog, Long> {

    /**
     * Find audit logs for a specific device
     */
    List<DeviceAuditLog> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);

    /**
     * Find audit logs by fulluuid
     */
    List<DeviceAuditLog> findByFulluuidOrderByCreatedAtDesc(String fulluuid);

    /**
     * Find audit logs by event type
     */
    List<DeviceAuditLog> findByEventType(String eventType);
}
