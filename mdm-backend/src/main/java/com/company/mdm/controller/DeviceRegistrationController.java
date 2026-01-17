package com.company.mdm.controller;

import com.company.mdm.dto.RegistrationRequest;
import com.company.mdm.dto.RegistrationResponse;
import com.company.mdm.service.DeviceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Device Registration Controller
 * 
 * Handles device enrollment (registration) endpoint
 */
@RestController
@RequestMapping("/desktopmdm")
@RequiredArgsConstructor
@Slf4j
public class DeviceRegistrationController {

    private final DeviceService deviceService;

    /**
     * Device Registration Endpoint
     * 
     * POST /desktopmdm/register
     * 
     * Desktop agent calls this to enroll and receive pushToken
     * 
     * IDEMPOTENT: Can be called multiple times
     */
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegistrationRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);

        log.info("Registration request from {} for device {}",
                ipAddress, request.getFulluuid());

        RegistrationResponse response = deviceService.registerDevice(request, ipAddress);

        // Determine HTTP status based on response
        if (!response.isSuccess()) {
            if (response.getMessage().contains("not registered")) {
                // Device not found (not pre-provisioned)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            } else if (response.getMessage().contains("blocked")) {
                // Device is blocked
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            } else {
                // Other errors
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        }

        // Success (including idempotent re-enrollment)
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
