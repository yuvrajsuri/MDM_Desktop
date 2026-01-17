package com.company.mdm.domain.entity;

/**
 * Command Type Enumeration
 * 
 * Defines all possible command types that can be sent to devices
 */
public enum CommandType {

    /**
     * Get/Update whitelist configuration
     * Payload: {whitelist: [{user, apps, urls}]}
     * Response: {success, applied_at}
     */
    GET_WHITELIST,

    /**
     * Update device policy
     * Payload: {policy_name, settings}
     * Response: {success, applied_at}
     */
    UPDATE_POLICY,

    /**
     * Install software
     * Payload: {package_name, version, download_url}
     * Response: {success, installed_version}
     */
    INSTALL_SOFTWARE,

    /**
     * Uninstall software
     * Payload: {package_name}
     * Response: {success}
     */
    UNINSTALL_SOFTWARE,

    /**
     * Remote wipe device
     * Payload: {wipe_type: "full" | "selective", preserve_data: []}
     * Response: {success, wiped_at}
     */
    REMOTE_WIPE,

    /**
     * Restart device
     * Payload: {delay_seconds}
     * Response: {success, restart_scheduled_at}
     */
    RESTART_DEVICE,

    /**
     * Lock device
     * Payload: {message, unlock_code}
     * Response: {success, locked_at}
     */
    LOCK_DEVICE,

    /**
     * Unlock device
     * Payload: {unlock_code}
     * Response: {success, unlocked_at}
     */
    UNLOCK_DEVICE,

    /**
     * Collect device info
     * Payload: {info_types: ["hardware", "software", "network"]}
     * Response: {success, data}
     */
    COLLECT_INFO,

    /**
     * Run custom script
     * Payload: {script_type, script_content, args}
     * Response: {success, output, exit_code}
     */
    RUN_SCRIPT,

    /**
     * Update configuration
     * Payload: {config_key, config_value}
     * Response: {success, updated_at}
     */
    UPDATE_CONFIG
}
