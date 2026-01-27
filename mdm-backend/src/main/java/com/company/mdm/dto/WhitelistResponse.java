package com.company.mdm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Whitelist Response DTO
 * 
 * Response sent to device when calling /getwhitelist
 * 
 * Example:
 * {
 * "success": true,
 * "commands": [
 * {
 * "user": "13667k",
 * "apps": ["notepad.exe", "chrome.exe"],
 * "urls": ["example.com", "github.com"]
 * }
 * ]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WhitelistResponse {

    private boolean success;

    private String message; // Only present on errors

    private List<Map<String, Object>> commands;
}
