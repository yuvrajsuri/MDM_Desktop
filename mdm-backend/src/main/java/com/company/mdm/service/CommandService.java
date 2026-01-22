package com.company.mdm.service;

import com.company.mdm.domain.entity.Command;
import com.company.mdm.domain.entity.CommandStatus;
import com.company.mdm.domain.entity.CommandType;
import com.company.mdm.domain.entity.Device;
import com.company.mdm.dto.CommandDTO;
import com.company.mdm.dto.CreateCommandRequest;
import com.company.mdm.repository.CommandRepository;
import com.company.mdm.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Command Service - Business logic for command management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommandService {

    private final CommandRepository commandRepository;
    private final DeviceRepository deviceRepository;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Create a new command for a device
     */
    @Transactional
    public Command createCommand(CreateCommandRequest request, String createdBy) {
        // Find device by fulluuid
        Optional<Device> deviceOpt = deviceRepository.findByFulluuid(request.getDevice_fulluuid());

        if (deviceOpt.isEmpty()) {
            throw new IllegalArgumentException("Device not found: " + request.getDevice_fulluuid());
        }

        Device device = deviceOpt.get();

        // Validate device is operational
        if (!device.isOperational()) {
            throw new IllegalStateException(
                    "Cannot send command to device in status: " + device.getStatus());
        }

        // Parse command type
        CommandType commandType;
        try {
            commandType = CommandType.valueOf(request.getCommand_type().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid command_type: " + request.getCommand_type());
        }

        // Parse expiration if provided
        LocalDateTime expiresAt = null;
        if (request.getExpires_at() != null && !request.getExpires_at().isEmpty()) {
            try {
                expiresAt = LocalDateTime.parse(request.getExpires_at(), ISO_FORMATTER);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid expires_at format. Use ISO 8601: " + e.getMessage());
            }
        }

        // Create command
        Command command = Command.builder()
                .deviceId(device.getId())
                .commandType(commandType)
                .payload(request.getPayload())
                .status(CommandStatus.PENDING)
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .expiresAt(expiresAt)
                .createdBy(createdBy)
                .build();

        command = commandRepository.save(command);

        log.info("Command created: id={}, type={}, device={}",
                command.getId(), command.getCommandType(), device.getFulluuid());

        return command;
    }

    /**
     * Get pending commands for a device
     * Called by /status endpoint
     */
    @Transactional
    public List<CommandDTO> getPendingCommandsForDevice(Long deviceId) {
        List<Command> commands = commandRepository.findPendingCommandsForDevice(
                deviceId,
                LocalDateTime.now());

        // Mark commands as delivered
        commands.forEach(Command::markDelivered);
        commandRepository.saveAll(commands);

        log.debug("Delivered {} commands to device {}", commands.size(), deviceId);

        // Convert to DTOs
        return commands.stream()
                .map(this::toCommandDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all commands for a device
     */
    public List<Command> getCommandsForDevice(String fulluuid) {
        Optional<Device> deviceOpt = deviceRepository.findByFulluuid(fulluuid);

        if (deviceOpt.isEmpty()) {
            throw new IllegalArgumentException("Device not found: " + fulluuid);
        }

        return commandRepository.findByDeviceIdOrderByCreatedAtDesc(deviceOpt.get().getId());
    }

    /**
     * Get command by ID
     */
    public Optional<Command> getCommandById(Long commandId) {
        return commandRepository.findById(commandId);
    }

    /**
     * Cancel a pending command
     */
    @Transactional
    public void cancelCommand(Long commandId) {
        Optional<Command> commandOpt = commandRepository.findById(commandId);

        if (commandOpt.isEmpty()) {
            throw new IllegalArgumentException("Command not found: " + commandId);
        }

        Command command = commandOpt.get();

        if (command.getStatus() != CommandStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot cancel command in status: " + command.getStatus());
        }

        command.setStatus(CommandStatus.CANCELLED);
        commandRepository.save(command);

        log.info("Command cancelled: id={}", commandId);
    }

    /**
     * Clean up expired commands (scheduled job)
     */
    @Transactional
    public int cleanupExpiredCommands() {
        List<Command> expiredCommands = commandRepository.findExpiredCommands(LocalDateTime.now());

        expiredCommands.forEach(cmd -> {
            cmd.setStatus(CommandStatus.EXPIRED);
            log.debug("Command expired: id={}", cmd.getId());
        });

        commandRepository.saveAll(expiredCommands);

        return expiredCommands.size();
    }

    /**
     * Convert Command entity to CommandDTO
     */
    private CommandDTO toCommandDTO(Command command) {
        return CommandDTO.builder()
                .id(command.getId())
                .type(command.getCommandType().name())
                .payload(command.getPayload())
                .priority(command.getPriority())
                .expires_at(command.getExpiresAt() != null ? command.getExpiresAt().format(ISO_FORMATTER) : null)
                .build();
    }
}
