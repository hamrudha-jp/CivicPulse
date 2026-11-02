-- ==============================================================================
-- CivicPulse Migration: Support CANCELLED Status in Check Constraints
-- ==============================================================================
-- Note: Fresh databases will automatically inherit all enum constants from
-- ComplaintStatus.java. If upgrading an existing database, run this script.
-- ==============================================================================

DO $$
BEGIN
    -- 1. Update complaints status check constraint
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'complaints_status_check'
    ) THEN
        ALTER TABLE complaints DROP CONSTRAINT complaints_status_check;
    END IF;

    ALTER TABLE complaints ADD CONSTRAINT complaints_status_check
        CHECK (status IN (
            'SUBMITTED', 'UNDER_REVIEW', 'ASSIGNED',
            'IN_PROGRESS', 'RESOLVED', 'REJECTED', 'DUPLICATE', 'CANCELLED'
        ));

    -- 2. Update complaint_status_history to_status constraint
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'complaint_status_history_to_status_check'
    ) THEN
        ALTER TABLE complaint_status_history DROP CONSTRAINT complaint_status_history_to_status_check;
    END IF;

    ALTER TABLE complaint_status_history ADD CONSTRAINT complaint_status_history_to_status_check
        CHECK (to_status IN (
            'SUBMITTED', 'UNDER_REVIEW', 'ASSIGNED',
            'IN_PROGRESS', 'RESOLVED', 'REJECTED', 'DUPLICATE', 'CANCELLED'
        ));

    -- 3. Update complaint_status_history from_status constraint
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'complaint_status_history_from_status_check'
    ) THEN
        ALTER TABLE complaint_status_history DROP CONSTRAINT complaint_status_history_from_status_check;
    END IF;

    ALTER TABLE complaint_status_history ADD CONSTRAINT complaint_status_history_from_status_check
        CHECK (from_status IN (
            'SUBMITTED', 'UNDER_REVIEW', 'ASSIGNED',
            'IN_PROGRESS', 'RESOLVED', 'REJECTED', 'DUPLICATE', 'CANCELLED'
        ));

    RAISE NOTICE 'CivicPulse status check constraints updated successfully.';
END $$;
