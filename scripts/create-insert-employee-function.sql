-- =============================================================================
-- Create insert_employee function in existing database
-- =============================================================================
-- Run this ONCE before importing employees:
-- docker compose -f docker-compose.prod.yml exec -T database psql -U postgres -d quickclock < scripts/create-insert-employee-function.sql
-- =============================================================================

-- Create extensions if not already present
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Drop function if it exists (for updates)
DROP FUNCTION IF EXISTS insert_employee(VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR, TEXT[]);

-- Create the insert_employee function
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
    -- Check if user already exists
    IF EXISTS (SELECT 1 FROM users WHERE username = p_username) THEN
        RAISE NOTICE 'User % already exists, skipping', p_username;
        RETURN;
    END IF;

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

    RAISE NOTICE 'Created employee: % (%) with roles: %', p_display_name, p_username, p_roles;
END;
$$ LANGUAGE plpgsql;

-- Verify function was created
SELECT 'insert_employee function created successfully' AS status;
