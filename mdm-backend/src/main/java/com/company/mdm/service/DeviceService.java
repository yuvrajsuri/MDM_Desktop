package com.company.mdm.service;

import com.company.mdm.domain.entity.Device;
import com.company.mdm.dto.RegistrationRequest;
import com.company.mdm.dto.RegistrationResponse;
import com.company.mdm.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Device Service - Simplified
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

        private final DeviceRepository deviceRepository;
        private final PushTokenService pushTokenService;
        private final AuditService auditService;

        private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

        /**
         * Register a device (enrollment)
         * 
         * ONLY works with devices that have status = TRUE
         * Does NOT change status, only finds existing active devices
         * IDEMPOTENT: Can be called multiple times with same fulluuid
         */
        @Transactional
        public RegistrationResponse registerDevice(RegistrationRequest request, String ipAddress) {
                String fulluuid = request.getFulluuid();

                log.info("Registration attempt for device: {}", fulluuid);

                // Log enrollment attempt
                auditService.logEnrollmentAttempt(fulluuid, ipAddress);

                // Find device by fulluuid with status = TRUE ONLY
                Optional<Device> deviceOpt = deviceRepository.findByFulluuidAndStatusTrue(fulluuid);

                if (deviceOpt.isEmpty()) {
                        // Device not found with status = true
                        log.warn("Device not found: {}", fulluuid);
                        auditService.logEnrollmentFailed(fulluuid, "Device not found", ipAddress);

                        return new RegistrationResponse(
                                        false,
                                        "Device not found.",
                                        null,
                                        null);
                }

                Device device = deviceOpt.get();

                // Check if device already has token (idempotent behavior)
                if (device.getPushtoken() != null && !device.getPushtoken().isEmpty()) {
                        // Device already has token - return existing token (idempotent)
                        log.info("Device already has token (idempotent): {}", fulluuid);

                        // Update metadata
                        device.updateCheckIn(
                                        request.getComputer_name(),
                                        request.getOs_name(),
                                        request.getOs_version(),
                                        ipAddress);
                        deviceRepository.save(device);

                        return new RegistrationResponse(
                                        true,
                                        "Device already registered",
                                        device.getPushtoken(),
                                        device.getTokenExpiresAt() != null
                                                        ? device.getTokenExpiresAt().format(ISO_FORMATTER)
                                                        : null);
                }

                // Generate NEW pushToken (first time registration)
                String pushToken = pushTokenService.generatePushToken();
                String tokenHash = pushTokenService.hashToken(pushToken);
                LocalDateTime expiresAt = pushTokenService.calculateExpirationTime();

                // Update device metadata and token
                device.setUuid15(request.getUuid15());
                device.setComputerName(request.getComputer_name());
                device.setOsName(request.getOs_name());
                device.setOsVersion(request.getOs_version());
                device.setIpAddress(ipAddress);
                device.setTokenHash(tokenHash);
                device.setPushtoken(pushToken);
                device.setTokenIssuedAt(LocalDateTime.now());
                device.setTokenExpiresAt(expiresAt);

                deviceRepository.save(device);

                // Audit logging
                auditService.logEnrollmentSuccess(device, ipAddress);
                auditService.logTokenIssued(device);

                log.info("Device registered successfully: {}", fulluuid);

                return new RegistrationResponse(
                                true,
                                "Device registered successfully",
                                pushToken,
                                expiresAt.format(ISO_FORMATTER));
        }

        /**
         * Create a new device (Admin function)
         * Device is created with status = TRUE (active) by default
         */
        @Transactional
        public Device createDevice(String fulluuid, String uuid15, String createdBy, String notes) {
                // Check if active device already exists
                if (deviceRepository.existsByFulluuidAndStatusTrue(fulluuid)) {
                        throw new IllegalArgumentException("Device already exists with fulluuid: " + fulluuid);
                }

                Device device = Device.builder()
                                .fulluuid(fulluuid)
                                .uuid15(uuid15)
                                .status(true) // Active by default - ready for registration
                                .createdBy(createdBy)
                                .notes(notes)
                                .build();

                device = deviceRepository.save(device);
                deviceRepository.flush();
                auditService.logDeviceCreated(device, createdBy);

                log.info("Device created with status=true: {}", fulluuid);

                return device;
        }

        /**
         * Get device by fulluuid (active only)
         */
        public Optional<Device> getActiveDevice(String fulluuid) {
                return deviceRepository.findByFulluuidAndStatusTrue(fulluuid);
        }
}
