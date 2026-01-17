package com.company.mdm.domain.entity;

/**
 * Actor Type - Who performed the action
 */
public enum ActorType {
    /**
     * Desktop device (using pushToken)
     */
    DEVICE,
    
    /**
     * Admin user (using JWT - future)
     */
    ADMIN,
    
    /**
     * System (automated action, scheduled job, etc.)
     */
    SYSTEM
}
