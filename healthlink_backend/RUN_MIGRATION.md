# How to Run Database Migration

## Option 1: Let Hibernate Auto-Update (Easiest - Development)

Since your application uses `ddl-auto: update`, Hibernate will automatically add the new columns when you restart the backend:

```bash
cd healthlink_backend
./gradlew bootRun
```

Hibernate will detect the new `town` and `servicesOffered` fields in the `Facility` entity and create the columns automatically.

**Note:** This is fine for development, but for production you should use Option 2.

---

## Option 2: Run SQL Script Manually (Recommended for Production)

### Method A: Using psql (PostgreSQL Command Line)

1. **Get your database connection details from `.env` file:**
   - Look for `SPRING_DATASOURCE_URL` or extract:
     - Host
     - Port
     - Database name
     - Username
     - Password

2. **Run the migration script:**

   **If using connection string format:**
   ```bash
   # Extract connection details from SPRING_DATASOURCE_URL
   # Format: postgresql://user:password@host:port/database
   
   psql "postgresql://user:password@host:port/database" -f DATABASE_MIGRATION_CLINIC_UPDATES.sql
   ```

   **If using separate connection details:**
   ```bash
   psql -h localhost -p 5432 -U your_username -d your_database -f DATABASE_MIGRATION_CLINIC_UPDATES.sql
   ```

   **Windows PowerShell:**
   ```powershell
   $env:PGPASSWORD="your_password"
   psql -h localhost -p 5432 -U your_username -d your_database -f DATABASE_MIGRATION_CLINIC_UPDATES.sql
   ```

### Method B: Using pgAdmin (GUI Tool)

1. Open pgAdmin
2. Connect to your database
3. Right-click on your database → **Query Tool**
4. Open the file `DATABASE_MIGRATION_CLINIC_UPDATES.sql`
5. Copy and paste the SQL into the query editor
6. Click **Execute** (F5)

### Method C: Using DBeaver or Other Database Clients

1. Connect to your PostgreSQL database
2. Open a new SQL script
3. Copy the contents of `DATABASE_MIGRATION_CLINIC_UPDATES.sql`
4. Paste and execute

### Method D: Using Docker (if your database is in Docker)

```bash
# Copy the SQL file into the container
docker cp DATABASE_MIGRATION_CLINIC_UPDATES.sql container_name:/tmp/

# Execute it
docker exec -i container_name psql -U username -d database_name < /tmp/DATABASE_MIGRATION_CLINIC_UPDATES.sql
```

---

## Option 3: Verify Migration Success

After running the migration (either Option 1 or 2), verify the columns were added:

```sql
-- Check if columns exist
SELECT column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_name = 'facilities' 
AND column_name IN ('town', 'services_offered');

-- Check existing data
SELECT id, name, town, services_offered FROM facilities LIMIT 5;
```

---

## Troubleshooting

### Error: "column already exists"
- This means the column was already added (possibly by Hibernate auto-update)
- The script uses `IF NOT EXISTS` so it's safe to run again

### Error: "permission denied"
- Make sure you're using a database user with ALTER TABLE permissions
- You may need to use a superuser or the database owner

### Error: "relation does not exist"
- Make sure you're connected to the correct database
- Check that the `facilities` table exists

---

## For Production

In production, it's recommended to:
1. Run the migration script manually (Option 2) during a maintenance window
2. Verify the migration succeeded
3. Then restart the application

This gives you more control and allows you to verify the changes before the application uses them.

