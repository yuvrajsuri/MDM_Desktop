package com.company.mdm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Create Command Request DTO
 * 
 * Used by admin to create commands for devices
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommandRequest {

    @NotBlank(message = "device_fulluuid is required")
    private String device_fulluuid; // Target device UUID

    @NotBlank(message = "command_type is required")
    private String command_type; // GET_WHITELIST, UPDATE_POLICY, etc.

    @NotNull(message = "payload is required")
    private Map<String, Object> payload; // Command-specific data

    private Integer priority; // Optional, default 0

    private String expires_at; // Optional, ISO datetime
}
