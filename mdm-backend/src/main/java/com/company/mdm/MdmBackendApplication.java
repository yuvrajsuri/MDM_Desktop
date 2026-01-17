package com.company.mdm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Desktop MDM Backend Application
 * 
 * Enterprise-grade Mobile Device Management system for desktop devices.
 * 
 * Key Features:
 * - Pre-provisioned enrollment model
 * - PushToken-based device authentication
 * - Automatic audit logging
 * - Idempotent operations
 * - Explicit status transitions
 * 
 * Authentication Model:
 * - Devices: PushToken (64-char hex, SHA-256 stored)
 * - Admins: JWT (future implementation)
 */
@SpringBootApplication
public class MdmBackendApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MdmBackendApplication.class, args);
    }
}
