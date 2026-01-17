package com.company.mdm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Device Status Response DTO
 * 
 * Response sent to desktop agent during status checks
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatusResponse {

    private boolean success;

    private String message; // Only present on errors

    private String status; // Device status: ENROLLED, ACTIVE, etc.

    private Boolean isActive; // TRUE when device is enrolled and actively checking in

    private String last_check_in;

    private String token_expires_at;

    private List<CommandDTO> commands; // Pending commands
}
