package com.company.mdm.controller;

import com.company.mdm.dto.CommandAcknowledgmentRequest;
import com.company.mdm.dto.RegistrationResponse;
import com.company.mdm.service.CommandService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Device Command Controller
 * 
 * Handles command acknowledgment from devices
 */
@RestController
@RequestMapping("/desktopmdm")
@RequiredArgsConstructor
@Slf4j
public class DeviceCommandController {

    private final CommandService commandService;

    /**
     * Command Acknowledgment Endpoint
     * 
     * POST /desktopmdm/acknowledge
     * 
     * Device calls this after executing a command
     * Requires X-Push-Token header (device authentication)
     */
    @PostMapping("/acknowledge")
    public ResponseEntity<RegistrationResponse> acknowledgeCommand(
            @RequestHeader(value = "X-Push-Token", required = false) String pushToken,
            @Valid @RequestBody CommandAcknowledgmentRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);

        // Validate pushToken present
        if (pushToken == null || pushToken.isEmpty()) {
            log.warn("Command acknowledgment without pushToken from {}", ipAddress);

            RegistrationResponse response = new RegistrationResponse(
                    false,
                    "X-Push-Token header is required",
                    null,
                    null);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        log.info("Command acknowledgment: id={}, status={}, from={}",
                request.getCommand_id(), request.getStatus(), ipAddress);

        try {
            // Acknowledge command
            commandService.acknowledgeCommand(
                    request.getCommand_id(),
                    request.getStatus(),
                    request.getResult(),
                    request.getError_message());

            RegistrationResponse response = new RegistrationResponse(
                    true,
                    "Command acknowledged",
                    null,
                    null);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid acknowledgment: {}", e.getMessage());

            RegistrationResponse response = new RegistrationResponse(
                    false,
                    e.getMessage(),
                    null,
                    null);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            log.error("Error acknowledging command", e);

            RegistrationResponse response = new RegistrationResponse(
                    false,
                    "Internal server error",
                    null,
                    null);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
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
