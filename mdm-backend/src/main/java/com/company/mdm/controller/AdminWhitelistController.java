package com.company.mdm.controller;

import com.company.mdm.domain.entity.Command;
import com.company.mdm.dto.WhitelistRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.company.mdm.service.CommandService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Whitelist Controller
 * 
 * Handles admin whitelist creation and management
 */
@RestController
@RequestMapping("/admin/whitelist")
@RequiredArgsConstructor
@Slf4j
public class AdminWhitelistController {

    private final CommandService commandService;

    /**
     * Create Whitelist for Device
     * 
     * POST /admin/whitelist/{fulluuid}
     * 
     * Admin creates/updates whitelist for a specific device
     * 
     * Request Body:
     * {
     * "commands": [
     * {
     * "user": "13667k",
     * "apps": ["notepad.exe", "chrome.exe"],
     * "urls": ["example.com", "github.com"]
     * }
     * ]
     * }
     */
    @PostMapping("/{fulluuid}")
    public ResponseEntity<?> createWhitelist(
            @PathVariable String fulluuid,
            @Valid @RequestBody WhitelistRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);
        String adminEmail = "admin@company.com"; // TODO: Get from JWT token

        log.info("Create whitelist request for device: {} from {}", fulluuid, ipAddress);

        try {
            Command command = commandService.createWhitelist(fulluuid, request, adminEmail);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Whitelist created successfully");
            response.put("command_id", command.getId());
            response.put("device_fulluuid", fulluuid);
            response.put("created_at", command.getCreatedAt().toString());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid whitelist request: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            log.error("Error creating whitelist", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Internal server error");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get Commands for Device
     * 
     * GET /admin/whitelist/{fulluuid}
     * 
     * Get all commands for a specific device
     */
    @GetMapping("/{fulluuid}")
    public ResponseEntity<?> getCommandsForDevice(@PathVariable String fulluuid) {
        log.info("Get commands for device: {}", fulluuid);

        try {
            List<Command> commands = commandService.getCommandsForDevice(fulluuid);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("device_fulluuid", fulluuid);
            response.put("commands", commands);
            response.put("count", commands.size());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
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
