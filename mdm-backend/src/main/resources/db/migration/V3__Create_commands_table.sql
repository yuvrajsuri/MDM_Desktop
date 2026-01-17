-- =============================================================================
-- Desktop MDM - Commands Table
-- Version: V3__Create_commands_table.sql
-- =============================================================================

-- =============================================================================
-- COMMANDS TABLE
-- =============================================================================
CREATE TABLE commands (
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
    
    -- Foreign key to devices
    CONSTRAINT fk_commands_device
        FOREIGN KEY (device_id)
        REFERENCES devices(id)
        ON DELETE CASCADE,
    
    -- Check constraints
    CONSTRAINT commands_status_valid CHECK (status IN (
        'PENDING',
        'DELIVERED',
        'EXECUTING',
        'EXECUTED',
        'FAILED',
        'CANCELLED',
        'EXPIRED'
    )),
    
    CONSTRAINT commands_type_valid CHECK (command_type IN (
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
    
    CONSTRAINT commands_priority_valid CHECK (priority >= 0 AND priority <= 10)
);

-- =============================================================================
-- INDEXES
-- =============================================================================

CREATE INDEX idx_commands_device_id ON commands(device_id);
CREATE INDEX idx_commands_status ON commands(status);
CREATE INDEX idx_commands_type ON commands(command_type);
CREATE INDEX idx_commands_created_at ON commands(created_at DESC);

-- Composite index for pending commands query (most common)
CREATE INDEX idx_commands_device_pending ON commands(device_id, status) 
WHERE status = 'PENDING';

-- Index for expired commands cleanup job
CREATE INDEX idx_commands_expires ON commands(expires_at) 
WHERE expires_at IS NOT NULL AND status = 'PENDING';

-- =============================================================================
-- TRIGGER FOR updated_at
-- =============================================================================

CREATE TRIGGER commands_updated_at_trigger
    BEFORE UPDATE ON commands
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- COMMENTS
-- =============================================================================

COMMENT ON TABLE commands IS 'Commands sent from admin to devices';
COMMENT ON COLUMN commands.device_id IS 'Reference to devices table';
COMMENT ON COLUMN commands.command_type IS 'Type of command (GET_WHITELIST, UPDATE_POLICY, etc.)';
COMMENT ON COLUMN commands.payload IS 'Command-specific data (JSON)';
COMMENT ON COLUMN commands.status IS 'Command lifecycle status';
COMMENT ON COLUMN commands.result IS 'Execution result from device (JSON)';
COMMENT ON COLUMN commands.priority IS 'Higher number = higher priority (0-10)';
COMMENT ON COLUMN commands.expires_at IS 'Command expires if not delivered by this time';
