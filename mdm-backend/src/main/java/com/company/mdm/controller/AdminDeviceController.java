package com.company.mdm.controller;

import com.company.mdm.domain.entity.Device;
import com.company.mdm.service.DeviceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin Device Controller
 * 
 * Handles admin device management
 */
@RestController
@RequestMapping("/admin/devices")
@RequiredArgsConstructor
@Slf4j
public class AdminDeviceController {

    private final DeviceService deviceService;

    /**
     * Create Device
     * 
     * POST /admin/devices
     * 
     * Admin pre-provisions a new device
     * 
     * Request Body:
     * {
     * "fulluuid": "550e8400-e29b-41d4-a716-446655440000",
     * "uuid15": "550e8400e29b41d",
     * "notes": "Marketing Laptop"
     * }
     */
    @PostMapping
    public ResponseEntity<?> createDevice(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);
        String adminEmail = "admin@company.com"; // TODO: Get from JWT token

        String fulluuid = request.get("fulluuid");
        String uuid15 = request.get("uuid15");
        String notes = request.get("notes");

        log.info("Create device request: {} from {}", fulluuid, ipAddress);

        // Validate required fields
        if (fulluuid == null || fulluuid.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "fulluuid is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (uuid15 == null || uuid15.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "uuid15 is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            Device device = deviceService.createDevice(fulluuid, uuid15, adminEmail, notes);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Device created successfully");
            response.put("device_id", device.getId());
            response.put("fulluuid", device.getFulluuid());
            response.put("status", device.getStatus());
            response.put("created_at", device.getCreatedAt().toString());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Device creation failed: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            log.error("Error creating device", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Internal server error");

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
