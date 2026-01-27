package com.company.mdm.service;

import com.company.mdm.domain.entity.Device;
import com.company.mdm.domain.entity.DeviceAuditLog;
import com.company.mdm.repository.DeviceAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Audit Service - Simplified
 * 
 * Logs all device events with REQUIRES_NEW propagation
 * to ensure logs are saved even if main transaction rolls back
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

        private final DeviceAuditLogRepository auditLogRepository;

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void logDeviceCreated(Device device, String adminEmail) {
                DeviceAuditLog auditLog = DeviceAuditLog.builder()
                                .deviceId(device.getId())
                                .fulluuid(device.getFulluuid())
                                .eventType("DEVICE_CREATED")
                                .actorType("ADMIN")
                                .build();
                auditLogRepository.save(auditLog);
                log.info("Audit: DEVICE_CREATED - {}", device.getFulluuid());
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void logEnrollmentAttempt(String fulluuid, String ipAddress) {
                DeviceAuditLog auditLog = DeviceAuditLog.builder()
                                .fulluuid(fulluuid)
                                .eventType("ENROLLMENT_ATTEMPT")
                                .ipAddress(ipAddress)
                                .actorType("DEVICE")
                                .build();
                auditLogRepository.save(auditLog);
                log.debug("Audit: ENROLLMENT_ATTEMPT - {}", fulluuid);
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void logEnrollmentSuccess(Device device, String ipAddress) {
                DeviceAuditLog auditLog = DeviceAuditLog.builder()
                                .deviceId(device.getId())
                                .fulluuid(device.getFulluuid())
                                .eventType("ENROLLMENT_SUCCESS")
                                .ipAddress(ipAddress)
                                .actorType("DEVICE")
                                .build();
                auditLogRepository.save(auditLog);
                log.info("Audit: ENROLLMENT_SUCCESS - {}", device.getFulluuid());
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void logEnrollmentFailed(String fulluuid, String reason, String ipAddress) {
                DeviceAuditLog auditLog = DeviceAuditLog.builder()
                                .fulluuid(fulluuid)
                                .eventType("ENROLLMENT_FAILED")
                                .ipAddress(ipAddress)
                                .actorType("DEVICE")
                                .build();
                auditLogRepository.save(auditLog);
                log.warn("Audit: ENROLLMENT_FAILED - {} ({})", fulluuid, reason);
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void logTokenIssued(Device device) {
                DeviceAuditLog auditLog = DeviceAuditLog.builder()
                                .deviceId(device.getId())
                                .fulluuid(device.getFulluuid())
                                .eventType("TOKEN_ISSUED")
                                .actorType("SYSTEM")
                                .build();
                auditLogRepository.save(auditLog);
                log.info("Audit: TOKEN_ISSUED - {}", device.getFulluuid());
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void logWhitelistFetched(Device device, String ipAddress) {
                DeviceAuditLog auditLog = DeviceAuditLog.builder()
                                .deviceId(device.getId())
                                .fulluuid(device.getFulluuid())
                                .eventType("WHITELIST_FETCHED")
                                .ipAddress(ipAddress)
                                .actorType("DEVICE")
                                .build();
                auditLogRepository.save(auditLog);
                log.debug("Audit: WHITELIST_FETCHED - {}", device.getFulluuid());
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void logWhitelistCreated(Long deviceId, String fulluuid, String adminEmail) {
                DeviceAuditLog auditLog = DeviceAuditLog.builder()
                                .deviceId(deviceId)
                                .fulluuid(fulluuid)
                                .eventType("WHITELIST_CREATED")
                                .actorType("ADMIN")
                                .build();
                auditLogRepository.save(auditLog);
                log.info("Audit: WHITELIST_CREATED - {} by {}", fulluuid, adminEmail);
        }
}
