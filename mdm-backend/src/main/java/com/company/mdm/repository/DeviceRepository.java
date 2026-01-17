package com.company.mdm.repository;

import com.company.mdm.domain.entity.Device;
import com.company.mdm.domain.entity.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Device Repository
 * 
 * Data access layer for Device entities
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    
    /**
     * Find device by fulluuid (immutable device identity)
     * 
     * CRITICAL: This is the primary lookup method for device operations
     */
    Optional<Device> findByFulluuid(String fulluuid);
    
    /**
     * Find device by pushToken hash
     * 
     * Used for device authentication in /status endpoint
     */
    Optional<Device> findByTokenHash(String tokenHash);
    
    /**
     * Find device by uuid15 (secondary identifier)
     */
    Optional<Device> findByUuid15(String uuid15);
    
    /**
     * Find all devices with specific status
     */
    List<Device> findByStatus(DeviceStatus status);
    
    /**
     * Find devices that haven't checked in since given timestamp
     * (Used to identify stale/offline devices)
     */
    @Query("SELECT d FROM Device d WHERE d.status = 'ACTIVE' " +
           "AND d.lastCheckIn < :threshold")
    List<Device> findStaleDevices(@Param("threshold") LocalDateTime threshold);
    
    /**
     * Find devices with expiring tokens
     */
    @Query("SELECT d FROM Device d WHERE d.status IN ('ENROLLED', 'ACTIVE') " +
           "AND d.tokenExpiresAt < :threshold " +
           "AND d.tokenExpiresAt > CURRENT_TIMESTAMP")
    List<Device> findDevicesWithExpiringTokens(@Param("threshold") LocalDateTime threshold);
    
    /**
     * Count devices by status
     */
    Long countByStatus(DeviceStatus status);
    
    /**
     * Check if fulluuid already exists
     */
    boolean existsByFulluuid(String fulluuid);
}
