-- =============================================================================
-- QuickClock Database Initialization
-- =============================================================================
-- This file is executed by PostgreSQL on first startup.
-- Use this to insert production employee accounts.
--
-- IMPORTANT: 
--   - Super Admin and Kiosk accounts are created by Spring Boot bootstrap
--   - This script is for additional employee accounts only
--   - Generate password hashes and secrets using: ./scripts/generate-user-credentials.sh
-- =============================================================================

-- Set timezone to Cairo (modify as needed)
SET timezone = 'Africa/Cairo';

-- Create pgcrypto extension for password hashing support
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Add a database comment for documentation
COMMENT ON DATABASE quickclock IS 'QuickClock Time Tracking Application - Production Database';

-- =============================================================================
-- EMPLOYEE ACCOUNTS
-- =============================================================================
-- Uncomment and modify the section below to add production employees.
-- Use ./scripts/generate-user-credentials.sh to generate password hashes and secrets.
--
-- WAIT for Spring Boot to create the tables first, then you can manually insert
-- users or use the provided SQL below after initial startup.
-- =============================================================================

-- NOTE: This will be executed BEFORE Spring Boot creates tables, so we can't
-- insert users here directly. Instead, we'll create a stored procedure that
-- can be called AFTER the application starts.

-- Create a helper function to insert employees (call this AFTER app startup)
CREATE OR REPLACE FUNCTION insert_employee(
    p_username VARCHAR(32),
    p_display_name VARCHAR(64),
    p_display_name_arabic VARCHAR(64),
    p_password_hash VARCHAR(100),
    p_secret VARCHAR(100),
    p_roles TEXT[]
) RETURNS void AS $$
DECLARE
    v_user_id BIGINT;
    v_role TEXT;
BEGIN
    -- Insert user
    INSERT INTO users (
        public_id,
        username,
        display_name,
        display_name_arabic,
        password_hash,
        secret,
        account_type,
        active,
        last_password_change,
        failed_login_attempts,
        created_at,
        updated_at
    ) VALUES (
        uuid_generate_v4(),
        p_username,
        p_display_name,
        p_display_name_arabic,
        p_password_hash,
        p_secret,
        'EMPLOYEE',
        true,
        NOW(),
        0,
        NOW(),
        NOW()
    ) RETURNING id INTO v_user_id;

    -- Insert roles
    FOREACH v_role IN ARRAY p_roles
    LOOP
        INSERT INTO user_roles (user_id, role)
        VALUES (v_user_id, v_role);
    END LOOP;

    RAISE NOTICE 'Created employee: % (%)', p_display_name, p_username;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- EXAMPLE USAGE (Run this AFTER Spring Boot has created the tables)
-- =============================================================================
-- Connect to the database and run:
--
-- SELECT insert_employee(
--     'john.doe',                    -- username
--     'John Doe',                    -- display_name
--     'جون دو',                      -- display_name_arabic
--     '$2a$10$...',                  -- password_hash (use generate-user-credentials.sh)
--     'base64secret...',             -- secret (use generate-user-credentials.sh)
--     ARRAY['EMPLOYEE']              -- roles: EMPLOYEE, ADMIN, or both
-- );
--
-- For admin rights: ARRAY['EMPLOYEE', 'ADMIN']
-- For employee only: ARRAY['EMPLOYEE']
-- =============================================================================

-- Log successful initialization
DO $$
BEGIN
    RAISE NOTICE 'QuickClock database initialized successfully';
    RAISE NOTICE 'Use insert_employee() function to add employees after application startup';
END $$;
