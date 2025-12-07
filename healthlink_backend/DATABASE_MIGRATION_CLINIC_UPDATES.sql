-- Database Migration: Add town and services_offered fields to facilities table
-- Date: 2025-01-27
-- Description: Adds town field for better location filtering and services_offered field to track which services (ONLINE/ONSITE) each clinic offers

-- Add town column
ALTER TABLE facilities ADD COLUMN IF NOT EXISTS town VARCHAR(120);

-- Add services_offered column
-- Format: comma-separated values like "ONLINE,ONSITE" or just "ONSITE"
ALTER TABLE facilities ADD COLUMN IF NOT EXISTS services_offered VARCHAR(50);

-- Set default services_offered for existing clinics (assume they offer both for backward compatibility)
UPDATE facilities SET services_offered = 'ONLINE,ONSITE' WHERE services_offered IS NULL;

-- Add index for town if needed for filtering
CREATE INDEX IF NOT EXISTS idx_facility_town ON facilities(town);

-- Add index for services_offered if needed for filtering
CREATE INDEX IF NOT EXISTS idx_facility_services ON facilities(services_offered);

