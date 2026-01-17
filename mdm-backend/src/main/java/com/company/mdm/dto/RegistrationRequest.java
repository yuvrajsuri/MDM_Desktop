package com.company.mdm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Device Registration Request DTO
 * 
 * Payload sent by desktop agent when calling /register
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {

    @NotBlank(message = "fulluuid is required")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "fulluuid must be a valid UUID format")
    private String fulluuid;

    @NotBlank(message = "uuid15 is required")
    @Size(min = 15, max = 15, message = "uuid15 must be exactly 15 characters")
    private String uuid15;

    @NotBlank(message = "computer_name is required")
    @Size(max = 255, message = "computer_name must not exceed 255 characters")
    private String computer_name;

    @NotBlank(message = "os_name is required")
    @Size(max = 100, message = "os_name must not exceed 100 characters")
    private String os_name;

    @NotBlank(message = "os_version is required")
    @Size(max = 50, message = "os_version must not exceed 50 characters")
    private String os_version;
}
