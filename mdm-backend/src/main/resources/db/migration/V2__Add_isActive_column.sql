-- =============================================================================
-- Desktop MDM - Add isActive Column
-- Version: V2__Add_isActive_column.sql
-- =============================================================================

-- Add isActive column to devices table
ALTER TABLE devices
ADD COLUMN is_active BOOLEAN DEFAULT FALSE;

-- Add index for querying active devices
CREATE INDEX idx_devices_is_active ON devices(is_active) WHERE is_active = TRUE;

-- Set isActive to TRUE for devices that are already ACTIVE status
UPDATE devices
SET is_active = TRUE
WHERE status = 'ACTIVE';

-- Add comment
COMMENT ON COLUMN devices.is_active IS 'TRUE when device is enrolled and actively checking in';
