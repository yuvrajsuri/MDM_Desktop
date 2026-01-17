package com.company.mdm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Device Registration Response DTO
 * 
 * Response sent back to desktop agent after /register call
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegistrationResponse {

    private boolean success;

    private String message;

    private String pushToken; // Only present on success

    private String expires_at; // Only present on success
}
