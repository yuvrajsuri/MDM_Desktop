package com.company.mdm.controller;

import com.company.mdm.domain.entity.Command;
import com.company.mdm.dto.CreateCommandRequest;
import com.company.mdm.dto.RegistrationResponse;
import com.company.mdm.service.CommandService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Admin Command Controller
 * 
 * Handles command creation and management by admins
 * 
 * Note: Currently no authentication. In production,
 * add @PreAuthorize("hasRole('ADMIN')")
 */
@RestController
@RequestMapping("/admin/commands")
@RequiredArgsConstructor
@Slf4j
public class AdminCommandController {

    private final CommandService commandService;

    /**
     * Create Command
     * 
     * POST /admin/commands
     * 
     * Admin creates a command for a device
     */
    @PostMapping
    public ResponseEntity<?> createCommand(
            @Valid @RequestBody CreateCommandRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String adminEmail = "admin@company.com"; // TODO: Get from JWT token

        log.info("Create command request: type={}, device={}, from={}",
                request.getCommand_type(), request.getDevice_fulluuid(), ipAddress);

        try {
            Command command = commandService.createCommand(request, adminEmail);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Command created successfully");
            response.put("command_id", command.getId());
            response.put("command_type", command.getCommandType().name());
            response.put("status", command.getStatus().name());
            response.put("created_at", command.getCreatedAt().toString());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid command request: {}", e.getMessage());

            RegistrationResponse response = new RegistrationResponse(
                    false,
                    e.getMessage(),
                    null,
                    null);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (IllegalStateException e) {
            log.warn("Cannot create command: {}", e.getMessage());

            RegistrationResponse response = new RegistrationResponse(
                    false,
                    e.getMessage(),
                    null,
                    null);

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            log.error("Error creating command", e);

            RegistrationResponse response = new RegistrationResponse(
                    false,
                    "Internal server error",
                    null,
                    null);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get Commands for Device
     * 
     * GET /admin/commands?device_fulluuid=xxx
     * 
     * Get all commands for a specific device
     */
    @GetMapping
    public ResponseEntity<?> getCommandsForDevice(
            @RequestParam("device_fulluuid") String deviceFulluuid) {
        log.info("Get commands for device: {}", deviceFulluuid);

        try {
            List<Command> commands = commandService.getCommandsForDevice(deviceFulluuid);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("commands", commands);
            response.put("count", commands.size());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());

            RegistrationResponse response = new RegistrationResponse(
                    false,
                    e.getMessage(),
                    null,
                    null);

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Get Command by ID
     * 
     * GET /admin/commands/{id}
     * 
     * Get details of a specific command
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCommand(@PathVariable Long id) {
        log.info("Get command: {}", id);

        Optional<Command> commandOpt = commandService.getCommandById(id);

        if (commandOpt.isEmpty()) {
            RegistrationResponse response = new RegistrationResponse(
                    false,
                    "Command not found",
                    null,
                    null);

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("command", commandOpt.get());

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel Command
     * 
     * DELETE /admin/commands/{id}
     * 
     * Cancel a pending command
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelCommand(@PathVariable Long id) {
        log.info("Cancel command: {}", id);

        try {
            commandService.cancelCommand(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Command cancelled");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Command not found: {}", e.getMessage());

            RegistrationResponse response = new RegistrationResponse(
                    false,
                    e.getMessage(),
                    null,
                    null);

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.warn("Cannot cancel command: {}", e.getMessage());

            RegistrationResponse response = new RegistrationResponse(
                    false,
                    e.getMessage(),
                    null,
                    null);

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
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
