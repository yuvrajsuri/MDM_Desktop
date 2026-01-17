package com.company.mdm.controller;

import com.company.mdm.dto.StatusResponse;
import com.company.mdm.service.DeviceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Device Status Controller
 * 
 * Handles device status check endpoint (polling)
 */
@RestController
@RequestMapping("/desktopmdm")
@RequiredArgsConstructor
@Slf4j
public class DeviceStatusController {

    private final DeviceService deviceService;

    /**
     * Device Status Check Endpoint
     * 
     * GET /desktopmdm/status
     * 
     * Desktop agent polls this every 5 minutes
     * Requires X-Push-Token header
     */
    @GetMapping("/status")
    public ResponseEntity<StatusResponse> getStatus(
            @RequestHeader(value = "X-Push-Token", required = false) String pushToken,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);

        // Validate pushToken header present
        if (pushToken == null || pushToken.isEmpty()) {
            log.warn("Status check without pushToken from {}", ipAddress);

            StatusResponse response = new StatusResponse(
                    false,
                    "X-Push-Token header is required",
                    null, null, null, null, null);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        log.debug("Status check from {} with token: {}...",
                ipAddress, pushToken.substring(0, Math.min(8, pushToken.length())));

        StatusResponse response = deviceService.checkStatus(pushToken, ipAddress);

        // Determine HTTP status
        if (!response.isSuccess()) {
            if (response.getMessage().contains("blocked")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            } else {
                // Invalid/expired token
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        }

        // Success
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
