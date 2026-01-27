package com.company.mdm.service;

import com.company.mdm.domain.entity.Command;
import com.company.mdm.domain.entity.Device;
import com.company.mdm.dto.WhitelistRequest;
import com.company.mdm.repository.CommandRepository;
import com.company.mdm.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Command Service - Simplified
 * 
 * Manages whitelist and other commands for devices
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommandService {

    private final CommandRepository commandRepository;
    private final DeviceRepository deviceRepository;
    private final AuditService auditService;

    /**
     * Create whitelist command for a device
     * Replaces any existing whitelist for that device
     */
    @Transactional
    public Command createWhitelist(String fulluuid, WhitelistRequest request, String createdBy) {
        // Find active device
        Optional<Device> deviceOpt = deviceRepository.findByFulluuidAndStatusTrue(fulluuid);

        if (deviceOpt.isEmpty()) {
            throw new IllegalArgumentException("Active device not found: " + fulluuid);
        }

        Device device = deviceOpt.get();

        // Create command
        Command command = Command.builder()
                .deviceId(device.getId())
                .commandType("GET_WHITELIST")
                .payload(request.getCommands())
                .createdBy(createdBy)
                .build();

        command = commandRepository.save(command);

        // Audit
        auditService.logWhitelistCreated(device.getId(), fulluuid, createdBy);

        log.info("Whitelist created for device: {}, command_id: {}", fulluuid, command.getId());

        return command;
    }

    /**
     * Get latest whitelist for a device
     * Returns device-specific whitelist or system default
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWhitelist(Long deviceId) {
        // Try to get device-specific whitelist
        Optional<Command> commandOpt = commandRepository.findLatestByDeviceIdAndCommandType(
                deviceId,
                "GET_WHITELIST");

        if (commandOpt.isPresent()) {
            return commandOpt.get().getPayload();
        }

        // Return system default whitelist
        Optional<Command> defaultCommandOpt = commandRepository.findLatestByDeviceIdAndCommandType(
                0L, // System default device ID
                "GET_WHITELIST");

        if (defaultCommandOpt.isPresent()) {
            return defaultCommandOpt.get().getPayload();
        }

        // Fallback: hardcoded default
        log.warn("No whitelist found, returning hardcoded default");
        return getHardcodedDefault();
    }

    /**
     * Get all commands for a device
     */
    public List<Command> getCommandsForDevice(String fulluuid) {
        Optional<Device> deviceOpt = deviceRepository.findByFulluuidAndStatusTrue(fulluuid);

        if (deviceOpt.isEmpty()) {
            throw new IllegalArgumentException("Active device not found: " + fulluuid);
        }

        return commandRepository.findByDeviceIdOrderByCreatedAtDesc(deviceOpt.get().getId());
    }

    /**
     * Hardcoded default whitelist
     */
    private List<Map<String, Object>> getHardcodedDefault() {
        List<Map<String, Object>> defaultList = new ArrayList<>();
        defaultList.add(Map.of(
                "user", "default",
                "apps",
                List.of("cmd.exe", "code.exe", "python.exe", "powershell.exe", "chrome.exe", "msedge.exe", "brave.exe",
                        "dmwmdmapp.exe", "_unins.tmp"),
                "urls",
                List.of("docs.python.org", "cdnjs.cloudflare.com", "cdn.jsdelivr.net", "code.jquery.com",
                        "ajax.googleapis.com", "cdn.tailwindcss.com", "bootstrapcdn.com", "fonts.googleapis.com",
                        "fonts.gstatic.com", "gstatic.com", "googleusercontent.com", "githubassets.com",
                        "githubusercontent.com", "gravatar.com", "twimg.com", "fbcdn.net", "googletagmanager.com",
                        "google-analytics.com", "facebook.net", "hotjar.com", "mixpanel.com", "recaptcha.net",
                        "hcaptcha.com", "octocaptcha.com", "api.github.com", "stripe.com", "example.com",
                        "172.17.42.164",
                        "localhost", "127.0.0.1", "devicemax.com", "microsoft.com")));
        return defaultList;
    }
}
