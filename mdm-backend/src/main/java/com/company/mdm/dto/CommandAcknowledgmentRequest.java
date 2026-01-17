package com.company.mdm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Command Acknowledgment Request DTO
 * 
 * Sent by device to acknowledge command execution
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandAcknowledgmentRequest {

    @NotNull(message = "command_id is required")
    private Long command_id;

    @NotBlank(message = "status is required")
    private String status; // EXECUTED or FAILED

    private Map<String, Object> result; // Execution result

    private String error_message; // Only if status = FAILED
}
