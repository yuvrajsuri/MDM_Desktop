-- =============================================================================
-- Desktop MDM - Initial Database Schema
-- Version: V1__Initial_schema.sql
-- =============================================================================

-- =============================================================================
-- DEVICES TABLE
-- =============================================================================
CREATE TABLE devices (
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
    CONSTRAINT devices_fulluuid_format CHECK (
        fulluuid ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
    ),
    CONSTRAINT devices_uuid15_length CHECK (length(uuid15) = 15),
    CONSTRAINT devices_status_valid CHECK (status IN (
        'PENDING_ENROLLMENT',
        'ENROLLED',
        'ACTIVE',
        'SUSPENDED',
        'BLOCKED',
        'WIPED'
    ))
);

-- =============================================================================
-- DEVICE AUDIT LOG TABLE
-- =============================================================================
CREATE TABLE device_audit_log (
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
    CONSTRAINT fk_device_audit_log_device
        FOREIGN KEY (device_id)
        REFERENCES devices(id)
        ON DELETE CASCADE
);

-- =============================================================================
-- INDEXES
-- =============================================================================

-- Devices table indexes
CREATE UNIQUE INDEX idx_devices_fulluuid ON devices(fulluuid);
CREATE INDEX idx_devices_uuid15 ON devices(uuid15);
CREATE INDEX idx_devices_status ON devices(status);
CREATE INDEX idx_devices_token_hash ON devices(token_hash) WHERE token_hash IS NOT NULL;
CREATE INDEX idx_devices_last_check_in ON devices(last_check_in) WHERE last_check_in IS NOT NULL;
CREATE INDEX idx_devices_created_at ON devices(created_at);

-- Audit log indexes
CREATE INDEX idx_audit_device_id ON device_audit_log(device_id);
CREATE INDEX idx_audit_event_type ON device_audit_log(event_type);
CREATE INDEX idx_audit_created_at ON device_audit_log(created_at DESC);
CREATE INDEX idx_audit_fulluuid ON device_audit_log(fulluuid);

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

CREATE TRIGGER devices_updated_at_trigger
    BEFORE UPDATE ON devices
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- SAMPLE DATA (Optional - for development/testing)
-- =============================================================================

-- Pre-provision some test devices
INSERT INTO devices (fulluuid, uuid15, status, created_by, notes) VALUES
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
INSERT INTO device_audit_log (device_id, fulluuid, event_type, actor_type, actor_id) VALUES
    (1, '03D0CBD0-D924-2DEC-BD74-DC375680ECE4', 'DEVICE_CREATED', 'ADMIN', 'admin@company.com'),
    (2, '550e8400-e29b-41d4-a716-446655440000', 'DEVICE_CREATED', 'ADMIN', 'admin@company.com');


