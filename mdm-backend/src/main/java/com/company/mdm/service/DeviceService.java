package com.company.mdm.service;

import com.company.mdm.domain.entity.Device;
import com.company.mdm.domain.entity.DeviceStatus;
import com.company.mdm.dto.CommandDTO;
import com.company.mdm.dto.RegistrationRequest;
import com.company.mdm.dto.RegistrationResponse;
import com.company.mdm.dto.StatusResponse;
import com.company.mdm.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Device Service - Business logic for device operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final PushTokenService pushTokenService;
    private final AuditService auditService;
    private final CommandService commandService; // NEW: Inject CommandService

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Register a device (enrollment)
     * 
     * IDEMPOTENT: Can be called multiple times with same fulluuid
     */
    @Transactional
    public RegistrationResponse registerDevice(RegistrationRequest request, String ipAddress) {
        String fulluuid = request.getFulluuid();

        log.info("Registration attempt for device: {}", fulluuid);

        // Log enrollment attempt
        auditService.logEnrollmentAttempt(
                fulluuid,
                request.getComputer_name(),
                ipAddress);

        // Find device by fulluuid
        Optional<Device> deviceOpt = deviceRepository.findByFulluuid(fulluuid);

        if (deviceOpt.isEmpty()) {
            // Device not found - not pre-provisioned
            log.warn("Device not found: {}", fulluuid);
            auditService.logEnrollmentFailed(fulluuid, "Device not pre-provisioned", ipAddress);

            return new RegistrationResponse(
                    false,
                    "Device not registered",
                    null,
                    null);
        }

        Device device = deviceOpt.get();

        // Check if device is blocked
        if (device.isBlocked()) {
            log.warn("Blocked device attempted enrollment: {}", fulluuid);
            auditService.logEnrollmentBlocked(device, ipAddress);

            return new RegistrationResponse(
                    false,
                    "Device blocked by administrator",
                    null,
                    null);
        }

        // Check if already enrolled (idempotent)
        if (device.getStatus() == DeviceStatus.ENROLLED ||
                device.getStatus() == DeviceStatus.ACTIVE) {

            // Update metadata
            device.setComputerName(request.getComputer_name());
            device.setOsName(request.getOs_name());
            device.setOsVersion(request.getOs_version());
            device.setIpAddress(ipAddress);
            device.setLastCheckIn(LocalDateTime.now());

            deviceRepository.save(device);

            log.info("Device already enrolled (idempotent): {}", fulluuid);
            auditService.logReEnrollment(device, ipAddress);

            // Return existing token info (idempotent response)
            // Note: In production, you'd need to regenerate or retrieve the actual token
            // For now, we'll indicate success but client should store their original token
            return new RegistrationResponse(
                    true,
                    "Device already registered",
                    null, // Client should use their stored token
                    device.getTokenExpiresAt() != null ? device.getTokenExpiresAt().format(ISO_FORMATTER) : null);
        }

        // Check if device can enroll
        if (!device.canEnroll()) {
            log.warn("Device cannot enroll, status: {}", device.getStatus());
            auditService.logEnrollmentFailed(
                    fulluuid,
                    "Invalid status: " + device.getStatus(),
                    ipAddress);

            return new RegistrationResponse(
                    false,
                    "Device cannot be enrolled in current status: " + device.getStatus(),
                    null,
                    null);
        }

        // Generate pushToken
        String pushToken = pushTokenService.generatePushToken();
        String tokenHash = pushTokenService.hashToken(pushToken);
        LocalDateTime expiresAt = pushTokenService.calculateExpirationTime();

        // Update device metadata
        device.setUuid15(request.getUuid15());
        device.setComputerName(request.getComputer_name());
        device.setOsName(request.getOs_name());
        device.setOsVersion(request.getOs_version());
        device.setIpAddress(ipAddress);

        // Enroll device (status transition: PENDING_ENROLLMENT → ENROLLED)
        device.enroll(tokenHash, expiresAt);

        deviceRepository.save(device);

        // Audit logging
        auditService.logEnrollmentSuccess(device, ipAddress);
        auditService.logTokenIssued(device);

        log.info("Device enrolled successfully: {}", fulluuid);

        return new RegistrationResponse(
                true,
                "Device registered successfully",
                pushToken,
                expiresAt.format(ISO_FORMATTER));
    }

    /**
     * Check device status (polling endpoint)
     */
    @Transactional
    public StatusResponse checkStatus(String pushToken, String ipAddress) {

        // Validate token format
        if (!pushTokenService.isValidFormat(pushToken)) {
            log.warn("Invalid pushToken format");
            return new StatusResponse(
                    false,
                    "Invalid pushToken format",
                    null, null, null, null, null);
        }

        // Hash token to find device
        String tokenHash = pushTokenService.hashToken(pushToken);
        Optional<Device> deviceOpt = deviceRepository.findByTokenHash(tokenHash);

        if (deviceOpt.isEmpty()) {
            log.warn("Device not found for token");
            return new StatusResponse(
                    false,
                    "Invalid or expired pushToken",
                    null, null, null, null, null);
        }

        Device device = deviceOpt.get();

        // Check token expiration
        if (device.isTokenExpired()) {
            log.warn("Expired token for device: {}", device.getFulluuid());
            return new StatusResponse(
                    false,
                    "pushToken expired",
                    null, null, null, null, null);
        }

        // Check if device is blocked
        if (device.isBlocked()) {
            log.warn("Blocked device attempted status check: {}", device.getFulluuid());
            return new StatusResponse(
                    false,
                    "Device blocked by administrator",
                    null, null, null, null, null);
        }

        // Check if device is operational
        if (!device.isOperational()) {
            log.warn("Device not operational, status: {}", device.getStatus());
            return new StatusResponse(
                    false,
                    "Device is not operational",
                    null, null, null, null, null);
        }

        // Track if this is first check-in (ENROLLED → ACTIVE transition)
        boolean isFirstCheckIn = (device.getStatus() == DeviceStatus.ENROLLED);

        // Update check-in timestamp and transition to ACTIVE if needed
        device.updateCheckIn(
                device.getComputerName(),
                device.getOsName(),
                device.getOsVersion(),
                ipAddress);

        deviceRepository.save(device);

        // Audit logging
        if (isFirstCheckIn) {
            auditService.logFirstCheckIn(device, ipAddress);
            log.info("First check-in: {} (ENROLLED → ACTIVE)", device.getFulluuid());
        } else {
            auditService.logCheckIn(device, ipAddress);
            log.debug("Check-in: {}", device.getFulluuid());
        }

        // Get pending commands for device
        List<CommandDTO> commands = commandService.getPendingCommandsForDevice(device.getId());

        log.debug("Returning {} pending commands to device {}", commands.size(), device.getFulluuid());

        // Return status
        return new StatusResponse(
                true,
                null,
                device.getStatus().name(),
                device.getIsActive(), // Include isActive flag
                device.getLastCheckIn() != null ? device.getLastCheckIn().format(ISO_FORMATTER) : null,
                device.getTokenExpiresAt() != null ? device.getTokenExpiresAt().format(ISO_FORMATTER) : null,
                commands // Return actual commands instead of empty list
        );
    }
}
