-- =============================================================================
-- Desktop MDM - Complete Database Schema with desktop_ prefix
-- Combined: V1 + V2 + V3 + V4 (All in one file)
-- Use this for fresh database setup
-- =============================================================================

-- =============================================================================
-- DESKTOP_DEVICES TABLE
-- =============================================================================
CREATE TABLE desktop_devices (
    id                  BIGSERIAL PRIMARY KEY,
    
    -- Device Identity (IMMUTABLE)
    fulluuid            VARCHAR(36) NOT NULL UNIQUE,
    uuid15              VARCHAR(15) NOT NULL,
    
    -- Device Metadata (updated on check-in)
    computer_name       VARCHAR(255),
    os_name             VARCHAR(100),
    os_version          VARCHAR(50),
    ip_address          VARCHAR(45),
    
    -- Status Management
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING_ENROLLMENT',
    is_active           BOOLEAN DEFAULT FALSE,
    
    -- PushToken (Device Authentication)
    token_hash          VARCHAR(255),
    token_issued_at     TIMESTAMP,
    token_expires_at    TIMESTAMP,
    
    -- Timestamps
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enrolled_at         TIMESTAMP,
    last_check_in       TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Audit
    created_by          VARCHAR(100),
    notes               TEXT,
    
    -- Constraints
    CONSTRAINT desktop_devices_fulluuid_format CHECK (
        fulluuid ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
    ),
    CONSTRAINT desktop_devices_uuid15_length CHECK (length(uuid15) = 15),
    CONSTRAINT desktop_devices_status_valid CHECK (status IN (
        'PENDING_ENROLLMENT',
        'ENROLLED',
        'ACTIVE',
        'SUSPENDED',
        'BLOCKED',
        'WIPED'
    ))
);

-- =============================================================================
-- DESKTOP_COMMANDS TABLE
-- =============================================================================
CREATE TABLE desktop_commands (
    id                  BIGSERIAL PRIMARY KEY,
    device_id           BIGINT NOT NULL,
    command_type        VARCHAR(50) NOT NULL,
    payload             JSONB,
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    result              JSONB,
    error_message       TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at        TIMESTAMP,
    executed_at         TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(100),
    priority            INTEGER DEFAULT 0,
    expires_at          TIMESTAMP,
    
    -- Foreign key to desktop_devices
    CONSTRAINT fk_desktop_commands_device
        FOREIGN KEY (device_id)
        REFERENCES desktop_devices(id)
        ON DELETE CASCADE,
    
    -- Check constraints
    CONSTRAINT desktop_commands_status_valid CHECK (status IN (
        'PENDING',
        'DELIVERED',
        'EXECUTING',
        'EXECUTED',
        'FAILED',
        'CANCELLED',
        'EXPIRED'
    )),
    
    CONSTRAINT desktop_commands_type_valid CHECK (command_type IN (
        'GET_WHITELIST',
        'UPDATE_POLICY',
        'INSTALL_SOFTWARE',
        'UNINSTALL_SOFTWARE',
        'REMOTE_WIPE',
        'RESTART_DEVICE',
        'LOCK_DEVICE',
        'UNLOCK_DEVICE',
        'COLLECT_INFO',
        'RUN_SCRIPT',
        'UPDATE_CONFIG'
    )),
    
    CONSTRAINT desktop_commands_priority_valid CHECK (priority >= 0 AND priority <= 10)
);

-- =============================================================================
-- DESKTOP_DEVICE_AUDIT_LOG TABLE
-- =============================================================================
CREATE TABLE desktop_device_audit_log (
    id                  BIGSERIAL PRIMARY KEY,
    device_id           BIGINT,
    fulluuid            VARCHAR(36),
    event_type          VARCHAR(50) NOT NULL,
    event_data          JSONB,
    ip_address          VARCHAR(45),
    user_agent          TEXT,
    actor_type          VARCHAR(50),
    actor_id            VARCHAR(100),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key (nullable for failed enrollment attempts)
    CONSTRAINT fk_desktop_device_audit_log_device
        FOREIGN KEY (device_id)
        REFERENCES desktop_devices(id)
        ON DELETE CASCADE
);

-- =============================================================================
-- INDEXES - DESKTOP_DEVICES
-- =============================================================================

CREATE UNIQUE INDEX idx_desktop_devices_fulluuid ON desktop_devices(fulluuid);
CREATE INDEX idx_desktop_devices_uuid15 ON desktop_devices(uuid15);
CREATE INDEX idx_desktop_devices_status ON desktop_devices(status);
CREATE INDEX idx_desktop_devices_is_active ON desktop_devices(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_desktop_devices_token_hash ON desktop_devices(token_hash) WHERE token_hash IS NOT NULL;
CREATE INDEX idx_desktop_devices_last_check_in ON desktop_devices(last_check_in) WHERE last_check_in IS NOT NULL;
CREATE INDEX idx_desktop_devices_created_at ON desktop_devices(created_at);

-- =============================================================================
-- INDEXES - DESKTOP_COMMANDS
-- =============================================================================

CREATE INDEX idx_desktop_commands_device_id ON desktop_commands(device_id);
CREATE INDEX idx_desktop_commands_status ON desktop_commands(status);
CREATE INDEX idx_desktop_commands_type ON desktop_commands(command_type);
CREATE INDEX idx_desktop_commands_created_at ON desktop_commands(created_at DESC);

-- Composite index for pending commands query (most common)
CREATE INDEX idx_desktop_commands_device_pending ON desktop_commands(device_id, status) 
WHERE status = 'PENDING';

-- Index for expired commands cleanup job
CREATE INDEX idx_desktop_commands_expires ON desktop_commands(expires_at) 
WHERE expires_at IS NOT NULL AND status = 'PENDING';

-- =============================================================================
-- INDEXES - DESKTOP_DEVICE_AUDIT_LOG
-- =============================================================================

CREATE INDEX idx_desktop_audit_device_id ON desktop_device_audit_log(device_id);
CREATE INDEX idx_desktop_audit_event_type ON desktop_device_audit_log(event_type);
CREATE INDEX idx_desktop_audit_created_at ON desktop_device_audit_log(created_at DESC);
CREATE INDEX idx_desktop_audit_fulluuid ON desktop_device_audit_log(fulluuid);

-- =============================================================================
-- TRIGGERS
-- =============================================================================

-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER desktop_devices_updated_at_trigger
    BEFORE UPDATE ON desktop_devices
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER desktop_commands_updated_at_trigger
    BEFORE UPDATE ON desktop_commands
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- COMMENTS
-- =============================================================================

-- desktop_devices comments
COMMENT ON TABLE desktop_devices IS 'Desktop devices managed by MDM system';
COMMENT ON COLUMN desktop_devices.fulluuid IS 'Full device UUID (36 chars)';
COMMENT ON COLUMN desktop_devices.uuid15 IS 'Short device UUID (15 chars)';
COMMENT ON COLUMN desktop_devices.is_active IS 'TRUE when device is enrolled and actively checking in';
COMMENT ON COLUMN desktop_devices.token_hash IS 'Hashed pushToken for authentication';

-- desktop_commands comments
COMMENT ON TABLE desktop_commands IS 'Commands sent from admin to devices';
COMMENT ON COLUMN desktop_commands.device_id IS 'Reference to desktop_devices table';
COMMENT ON COLUMN desktop_commands.command_type IS 'Type of command (GET_WHITELIST, UPDATE_POLICY, etc.)';
COMMENT ON COLUMN desktop_commands.payload IS 'Command-specific data (JSON)';
COMMENT ON COLUMN desktop_commands.status IS 'Command lifecycle status';
COMMENT ON COLUMN desktop_commands.result IS 'Execution result from device (JSON)';
COMMENT ON COLUMN desktop_commands.priority IS 'Higher number = higher priority (0-10)';
COMMENT ON COLUMN desktop_commands.expires_at IS 'Command expires if not delivered by this time';

-- desktop_device_audit_log comments
COMMENT ON TABLE desktop_device_audit_log IS 'Audit trail for all device events';

-- =============================================================================
-- SAMPLE DATA (Optional - for development/testing)
-- =============================================================================

-- Pre-provision some test devices
INSERT INTO desktop_devices (fulluuid, uuid15, status, created_by, notes) VALUES
    (
        '03D0CBD0-D924-2DEC-BD74-DC375680ECE4',
        '03ddc375680ece4',
        'PENDING_ENROLLMENT',
        'admin@company.com',
        'Test Device - KBP-DEV-207'
    ),
    (
        '550e8400-e29b-41d4-a716-446655440000',
        '550e8400e29b41d',
        'PENDING_ENROLLMENT',
        'admin@company.com',
        'Test Device - Marketing Laptop'
    );

-- Log device creation events
INSERT INTO desktop_device_audit_log (device_id, fulluuid, event_type, actor_type, actor_id) VALUES
    (1, '03D0CBD0-D924-2DEC-BD74-DC375680ECE4', 'DEVICE_CREATED', 'ADMIN', 'admin@company.com'),
    (2, '550e8400-e29b-41d4-a716-446655440000', 'DEVICE_CREATED', 'ADMIN', 'admin@company.com');