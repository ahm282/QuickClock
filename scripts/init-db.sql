-- =============================================================================
-- QuickClock Database Initialization
-- =============================================================================
-- This file is executed by PostgreSQL on first startup.
-- Primarily used for database-level configuration.
--
-- NOTE: Application users are created by Spring Boot's bootstrap mechanism.
--       This file is for PostgreSQL-specific initialization only.
-- =============================================================================

-- Set timezone to Cairo (modify as needed)
SET timezone = 'Africa/Cairo';

-- Create extensions if needed (uncomment if required)
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Add a database comment for documentation
COMMENT ON DATABASE quickclock IS 'QuickClock Time Tracking Application - Production Database';

-- Log successful initialization
DO $$
BEGIN
    RAISE NOTICE 'QuickClock database initialized successfully';
END $$;
