package com.company.mdm.domain.entity;

/**
 * Device Status Enumeration
 * 
 * CRITICAL STATUS FLOW:
 * PENDING_ENROLLMENT → ENROLLED → ACTIVE → (SUSPENDED | BLOCKED | WIPED)
 * 
 * Status Definitions:
 * - PENDING_ENROLLMENT: Device pre-provisioned by admin, awaiting first registration
 * - ENROLLED: PushToken issued, device registered, but no check-in yet
 * - ACTIVE: Device actively checking in (first check-in received)
 * - SUSPENDED: Temporarily disabled by admin (can be reactivated)
 * - BLOCKED: Permanently banned by admin (cannot be reactivated)
 * - WIPED: Remote wipe executed (terminal state)
 */
public enum DeviceStatus {
    
    /**
     * Device record exists, waiting for device to register
     * Admin has pre-provisioned, but desktop hasn't called /register yet
     */
    PENDING_ENROLLMENT,
    
    /**
     * Device registered, pushToken issued
     * Device can authenticate but hasn't sent first status check-in
     * Transition: PENDING_ENROLLMENT → ENROLLED (via /register API)
     */
    ENROLLED,
    
    /**
     * Device is operational and checking in regularly
     * First /status check-in received
     * Transition: ENROLLED → ACTIVE (via first /status call)
     */
    ACTIVE,
    
    /**
     * Device temporarily suspended by admin
     * Can be reactivated
     * Transition: ACTIVE → SUSPENDED (via admin API)
     */
    SUSPENDED,
    
    /**
     * Device permanently blocked by admin
     * Cannot be reactivated (security violation, lost device, etc.)
     * Transition: * → BLOCKED (via admin API)
     */
    BLOCKED,
    
    /**
     * Remote wipe executed
     * Terminal state
     * Transition: * → WIPED (via admin API)
     */
    WIPED;
    
    /**
     * Check if status allows enrollment
     */
    public boolean canEnroll() {
        return this == PENDING_ENROLLMENT;
    }
    
    /**
     * Check if status allows status check-in
     */
    public boolean canCheckIn() {
        return this == ENROLLED || this == ACTIVE;
    }
    
    /**
     * Check if status is terminal (no more transitions allowed)
     */
    public boolean isTerminal() {
        return this == WIPED;
    }
}
