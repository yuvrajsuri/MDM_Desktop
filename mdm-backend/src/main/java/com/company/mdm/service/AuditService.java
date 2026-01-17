package com.company.mdm.service;

import com.company.mdm.domain.entity.*;
import com.company.mdm.repository.DeviceAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Audit Service - Automatic audit trail logging
 * 
 * CRITICAL: All device events MUST be logged
 * Uses REQUIRES_NEW propagation to ensure logs are saved even if main
 * transaction rolls back
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final DeviceAuditLogRepository auditLogRepository;

    /**
     * Log device created event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDeviceCreated(Device device, String adminEmail) {
        DeviceAuditLog auditLog = DeviceAuditLog.builder()
                .deviceId(device.getId())
                .fulluuid(device.getFulluuid())
                .eventType(AuditEventType.DEVICE_CREATED)
                .eventData(Map.of(
                        "computer_name", device.getComputerName() != null ? device.getComputerName() : "",
                        "created_by", adminEmail))
                .actorType(ActorType.ADMIN)
                .actorId(adminEmail)
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit: DEVICE_CREATED - {}", device.getFulluuid());
    }

    /**
     * Log enrollment attempt (device calls /register)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEnrollmentAttempt(String fulluuid, String computerName, String ipAddress) {
        DeviceAuditLog auditLog = DeviceAuditLog.builder()
                .deviceId(null) // May not exist yet
                .fulluuid(fulluuid)
                .eventType(AuditEventType.ENROLLMENT_ATTEMPT)
                .eventData(Map.of(
                        "computer_name", computerName != null ? computerName : "",
                        "ip_address", ipAddress != null ? ipAddress : ""))
                .ipAddress(ipAddress)
                .actorType(ActorType.DEVICE)
                .actorId(fulluuid)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit: ENROLLMENT_ATTEMPT - {}", fulluuid);
    }

    /**
     * Log successful enrollment
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEnrollmentSuccess(Device device, String ipAddress) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("computer_name", device.getComputerName());
        eventData.put("os_name", device.getOsName());
        eventData.put("os_version", device.getOsVersion());
        eventData.put("status_transition", "PENDING_ENROLLMENT -> ENROLLED");

        DeviceAuditLog auditLog = DeviceAuditLog.builder()
                .deviceId(device.getId())
                .fulluuid(device.getFulluuid())
                .eventType(AuditEventType.ENROLLMENT_SUCCESS)
                .eventData(eventData)
                .ipAddress(ipAddress)
                .actorType(ActorType.DEVICE)
                .actorId(device.getFulluuid())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit: ENROLLMENT_SUCCESS - {}", device.getFulluuid());
    }

    /**
     * Log failed enrollment (device not found)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEnrollmentFailed(String fulluuid, String reason, String ipAddress) {
        DeviceAuditLog auditLog = DeviceAuditLog.builder()
                .deviceId(null)
                .fulluuid(fulluuid)
                .eventType(AuditEventType.ENROLLMENT_FAILED)
                .eventData(Map.of("reason", reason))
                .ipAddress(ipAddress)
                .actorType(ActorType.DEVICE)
                .actorId(fulluuid)
                .build();

        auditLogRepository.save(auditLog);
        log.warn("Audit: ENROLLMENT_FAILED - {} ({})", fulluuid, reason);
    }

    /**
     * Log blocked device enrollment attempt
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEnrollmentBlocked(Device device, String ipAddress) {
        DeviceAuditLog auditLog = DeviceAuditLog.builder()
                .deviceId(device.getId())
                .fulluuid(device.getFulluuid())
                .eventType(AuditEventType.ENROLLMENT_BLOCKED)
                .eventData(Map.of(
                        "status", device.getStatus().name(),
                        "reason", "Device is blocked"))
                .ipAddress(ipAddress)
                .actorType(ActorType.DEVICE)
                .actorId(device.getFulluuid())
                .build();

        auditLogRepository.save(auditLog);
        log.warn("Audit: ENROLLMENT_BLOCKED - {}", device.getFulluuid());
    }

    /**
     * Log re-enrollment (idempotent call)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logReEnrollment(Device device, String ipAddress) {
        DeviceAuditLog auditLog = DeviceAuditLog.builder()
                .deviceId(device.getId())
                .fulluuid(device.getFulluuid())
                .eventType(AuditEventType.RE_ENROLLMENT)
                .eventData(Map.of(
                        "status", device.getStatus().name(),
                        "note", "Device called /register again (idempotent)"))
                .ipAddress(ipAddress)
                .actorType(ActorType.DEVICE)
                .actorId(device.getFulluuid())
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit: RE_ENROLLMENT - {}", device.getFulluuid());
    }

    /**
     * Log pushToken issued
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTokenIssued(Device device) {
        DeviceAuditLog auditLog = DeviceAuditLog.builder()
                .deviceId(device.getId())
                .fulluuid(device.getFulluuid())
                .eventType(AuditEventType.TOKEN_ISSUED)
                .eventData(Map.of(
                        "expires_at", device.getTokenExpiresAt().toString()))
                .actorType(ActorType.SYSTEM)
                .actorId("mdm-backend")
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit: TOKEN_ISSUED - {}", device.getFulluuid());
    }

    /**
     * Log device check-in
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCheckIn(Device device, String ipAddress) {
        DeviceAuditLog auditLog = DeviceAuditLog.builder()
                .deviceId(device.getId())
                .fulluuid(device.getFulluuid())
                .eventType(AuditEventType.CHECK_IN)
                .eventData(Map.of(
                        "status", device.getStatus().name()))
                .ipAddress(ipAddress)
                .actorType(ActorType.DEVICE)
                .actorId(device.getFulluuid())
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit: CHECK_IN - {}", device.getFulluuid());
    }

    /**
     * Log first check-in (ENROLLED → ACTIVE transition)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFirstCheckIn(Device device, String ipAddress) {
        DeviceAuditLog auditLog = DeviceAuditLog.builder()
                .deviceId(device.getId())
                .fulluuid(device.getFulluuid())
                .eventType(AuditEventType.FIRST_CHECK_IN)
                .eventData(Map.of(
                        "status_transition", "ENROLLED -> ACTIVE"))
                .ipAddress(ipAddress)
                .actorType(ActorType.DEVICE)
                .actorId(device.getFulluuid())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit: FIRST_CHECK_IN - {} (ENROLLED -> ACTIVE)", device.getFulluuid());
    }

    /**
     * Log status change
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logStatusChange(Device device, DeviceStatus oldStatus, DeviceStatus newStatus, String adminEmail) {
        DeviceAuditLog auditLog = DeviceAuditLog.builder()
                .deviceId(device.getId())
                .fulluuid(device.getFulluuid())
                .eventType(AuditEventType.STATUS_CHANGED)
                .eventData(Map.of(
                        "old_status", oldStatus.name(),
                        "new_status", newStatus.name(),
                        "changed_by", adminEmail != null ? adminEmail : "SYSTEM"))
                .actorType(adminEmail != null ? ActorType.ADMIN : ActorType.SYSTEM)
                .actorId(adminEmail != null ? adminEmail : "mdm-backend")
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit: STATUS_CHANGED - {} ({} -> {})",
                device.getFulluuid(), oldStatus, newStatus);
    }

    /**
     * Log device blocked
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDeviceBlocked(Device device, String adminEmail) {
        DeviceAuditLog auditLog = DeviceAuditLog.builder()
                .deviceId(device.getId())
                .fulluuid(device.getFulluuid())
                .eventType(AuditEventType.DEVICE_BLOCKED)
                .eventData(Map.of(
                        "blocked_by", adminEmail))
                .actorType(ActorType.ADMIN)
                .actorId(adminEmail)
                .build();

        auditLogRepository.save(auditLog);
        log.warn("Audit: DEVICE_BLOCKED - {} by {}", device.getFulluuid(), adminEmail);
    }
}
