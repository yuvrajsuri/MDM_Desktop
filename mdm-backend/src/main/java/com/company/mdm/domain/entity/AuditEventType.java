package com.company.mdm.domain.entity;

/**
 * Audit Event Types
 * 
 * All possible events that can be logged in the audit trail.
 */
public enum AuditEventType {
    
    // Device lifecycle events
    DEVICE_CREATED,              // Admin pre-provisioned device
    DEVICE_DELETED,              // Admin deleted device
    
    // Enrollment events
    ENROLLMENT_ATTEMPT,          // Device attempted to register
    ENROLLMENT_SUCCESS,          // Device successfully enrolled (pushToken issued)
    ENROLLMENT_FAILED,           // Device not found or error
    ENROLLMENT_BLOCKED,          // Blocked device tried to enroll
    RE_ENROLLMENT,               // Already-enrolled device called /register (idempotent)
    
    // Status check events
    CHECK_IN,                    // Device polled /status
    FIRST_CHECK_IN,              // Device transitioned ENROLLED → ACTIVE
    
    // Token events
    TOKEN_ISSUED,                // PushToken generated
    TOKEN_EXPIRED,               // PushToken expired
    TOKEN_REVOKED,               // Admin revoked pushToken
    
    // Status change events
    STATUS_CHANGED,              // Any status transition
    DEVICE_ACTIVATED,            // ENROLLED → ACTIVE
    DEVICE_SUSPENDED,            // ACTIVE → SUSPENDED
    DEVICE_REACTIVATED,          // SUSPENDED → ACTIVE
    DEVICE_BLOCKED,              // * → BLOCKED
    DEVICE_WIPED,                // * → WIPED
    
    // Device metadata updates
    DEVICE_UPDATED,              // Metadata changed (computer_name, os_version, etc.)
    
    // Admin actions
    ADMIN_ACTION                 // Generic admin action
}
