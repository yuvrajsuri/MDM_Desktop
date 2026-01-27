package com.company.mdm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Whitelist Request DTO
 * 
 * Used by admin to create whitelist for a device
 * 
 * Example:
 * {
 * "commands": [
 * {
 * "user": "13667k",
 * "apps": ["notepad.exe", "chrome.exe"],
 * "urls": ["example.com", "github.com"]
 * },
 * {
 * "user": "27891a",
 * "apps": ["python.exe"],
 * "urls": ["stackoverflow.com"]
 * }
 * ]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WhitelistRequest {

    @NotNull(message = "commands is required")
    @NotEmpty(message = "commands cannot be empty")
    private List<Map<String, Object>> commands;
}
