-- Fix medical_records table: Change oid columns to TEXT
-- Run this in your Neon database to fix the "Large Objects may not be used in auto-commit mode" error

-- Step 1: Check current column types
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'medical_records' 
AND column_name IN ('details', 'description');

-- Step 2: Alter columns from oid to TEXT
-- This will fix the Hibernate error when reading medical records
ALTER TABLE medical_records 
    ALTER COLUMN details TYPE TEXT,
    ALTER COLUMN description TYPE TEXT;

-- Step 3: Verify the change
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'medical_records' 
AND column_name IN ('details', 'description');

-- Note: If you have existing data stored as oid, you may need to migrate it first.
-- For new records, this change will allow them to be stored and read correctly.

