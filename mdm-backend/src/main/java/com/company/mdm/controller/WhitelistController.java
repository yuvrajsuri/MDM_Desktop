package com.company.mdm.controller;

import com.company.mdm.domain.entity.Device;
import com.company.mdm.dto.WhitelistResponse;
import com.company.mdm.repository.DeviceRepository;
import com.company.mdm.service.AuditService;
import com.company.mdm.service.CommandService;
import com.company.mdm.service.PushTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Whitelist Controller
 * 
 * Handles device whitelist fetching
 */
@RestController
@RequestMapping("/desktopmdm")
@RequiredArgsConstructor
@Slf4j
public class WhitelistController {

    private final CommandService commandService;
    private final DeviceRepository deviceRepository;
    private final PushTokenService pushTokenService;
    private final AuditService auditService;

    /**
     * Get Whitelist Endpoint
     * 
     * GET /desktopmdm/getwhitelist
     * 
     * Device calls this to get whitelist configuration
     * Requires X-Push-Token header for authentication
     */
    @GetMapping("/getwhitelist")
    public ResponseEntity<WhitelistResponse> getWhitelist(
            @RequestHeader(value = "X-Push-Token", required = false) String pushToken,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);

        // Validate pushToken header present
        if (pushToken == null || pushToken.isEmpty()) {
            log.warn("Whitelist request without pushToken from {}", ipAddress);

            WhitelistResponse response = new WhitelistResponse(
                    false,
                    "X-Push-Token header is required",
                    null);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Validate token format
        if (!pushTokenService.isValidFormat(pushToken)) {
            log.warn("Invalid pushToken format from {}", ipAddress);

            WhitelistResponse response = new WhitelistResponse(
                    false,
                    "Invalid pushToken format",
                    null);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Hash token and find device
        String tokenHash = pushTokenService.hashToken(pushToken);
        Optional<Device> deviceOpt = deviceRepository.findByTokenHashAndStatusTrue(tokenHash);

        if (deviceOpt.isEmpty()) {
            log.warn("Device not found or inactive for token from {}", ipAddress);

            WhitelistResponse response = new WhitelistResponse(
                    false,
                    "Invalid or expired pushToken",
                    null);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        Device device = deviceOpt.get();

        // Check token expiration
        if (device.isTokenExpired()) {
            log.warn("Expired token for device: {} from {}", device.getFulluuid(), ipAddress);

            WhitelistResponse response = new WhitelistResponse(
                    false,
                    "pushToken expired",
                    null);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Update last check-in
        device.updateCheckIn(
                device.getComputerName(),
                device.getOsName(),
                device.getOsVersion(),
                ipAddress);
        deviceRepository.save(device);

        // Get whitelist
        List<Map<String, Object>> commands = commandService.getWhitelist(device.getId());

        // Audit
        auditService.logWhitelistFetched(device, ipAddress);

        log.debug("Whitelist fetched by device: {}", device.getFulluuid());

        // Return whitelist
        WhitelistResponse response = new WhitelistResponse(
                true,
                null,
                commands);

        return ResponseEntity.ok(response);
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
