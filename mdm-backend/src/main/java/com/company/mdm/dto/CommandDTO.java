package com.company.mdm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Command DTO - Sent to device in /status response
 * 
 * Lightweight representation of command for device
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommandDTO {

    private Long id;

    private String type; // Command type (GET_WHITELIST, UPDATE_POLICY, etc.)

    private Map<String, Object> payload; // Command-specific data

    private Integer priority; // Higher = more urgent

    private String expires_at; // ISO datetime
}
