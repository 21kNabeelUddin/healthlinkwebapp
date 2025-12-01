# Setting Up .env File for Local Development

## Quick Setup

1. **Get your Neon database connection string:**
   - Go to your Neon dashboard
   - Select your project
   - Click on "Connection Details" or "Connection String"
   - Copy the connection string (format: `postgresql://user:password@host:port/database?sslmode=require`)

2. **Create `.env` file:**
   ```powershell
   # In healthlink_backend directory
   Copy-Item .env.example .env
   ```

3. **Edit `.env` file** with your Neon database details:
   ```env
   SPRING_DATASOURCE_URL=jdbc:postgresql://your-neon-host:5432/your-database?sslmode=require
   SPRING_DATASOURCE_USERNAME=your-username
   SPRING_DATASOURCE_PASSWORD=your-password
   PHI_ENCRYPTION_KEY=your-generated-key
   ```

## Converting Neon Connection String

If Neon gives you a connection string like:
```
postgresql://user:password@ep-cool-darkness-123456.us-east-2.aws.neon.tech/healthlink?sslmode=require
```

Convert it to JDBC format:
```
jdbc:postgresql://ep-cool-darkness-123456.us-east-2.aws.neon.tech/healthlink?sslmode=require&user=user&password=password
```

Or split it:
```
SPRING_DATASOURCE_URL=jdbc:postgresql://ep-cool-darkness-123456.us-east-2.aws.neon.tech/healthlink?sslmode=require
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=password
```

## Generate Encryption Key

Run this PowerShell command:
```powershell
.\generate-key.ps1
```

Or manually:
```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

Copy the output and paste it as `PHI_ENCRYPTION_KEY` in your `.env` file.

## Verify Setup

After creating `.env`, restart the backend:
```powershell
./gradlew bootRun
```

You should see:
- ✅ "✓ Loaded environment variables from .env file"
- ✅ Database connection successful
- ✅ "Started HealthLinkApplication"

## Troubleshooting

**Error: "UnknownHostException: postgres"**
- Your `.env` file is not being loaded
- Check that `.env` is in `healthlink_backend/` directory (same folder as `build.gradle`)
- Verify `SPRING_DATASOURCE_URL` is set correctly

**Error: "Connection refused"**
- Check your Neon database is running
- Verify the host, port, username, and password are correct
- Ensure `sslmode=require` is included for Neon

**Error: "encryption-key must be provided"**
- Set `PHI_ENCRYPTION_KEY` in your `.env` file
- Generate a key using `.\generate-key.ps1`

