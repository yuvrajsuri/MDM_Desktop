package com.company.mdm.domain.entity;

/**
 * Command Status Enumeration
 * 
 * Tracks the lifecycle of a command from creation to execution
 */
public enum CommandStatus {

    /**
     * Command created, waiting to be delivered to device
     */
    PENDING,

    /**
     * Command delivered to device (device received it in /status response)
     */
    DELIVERED,

    /**
     * Device is currently executing the command
     */
    EXECUTING,

    /**
     * Command executed successfully by device
     */
    EXECUTED,

    /**
     * Command execution failed
     */
    FAILED,

    /**
     * Command cancelled by admin before delivery
     */
    CANCELLED,

    /**
     * Command expired before delivery
     */
    EXPIRED
}
