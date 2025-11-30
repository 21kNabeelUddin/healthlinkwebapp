# PowerShell script to load .env file and run Spring Boot backend
# This script reads the .env file and sets environment variables before running the backend

Write-Host "Loading environment variables from .env file..." -ForegroundColor Cyan

# Read .env file and set environment variables
if (Test-Path .env) {
    Get-Content .env | ForEach-Object {
        # Skip comments and empty lines
        if ($_ -match '^\s*#' -or $_ -match '^\s*$') {
            return
        }
        
        # Parse KEY=VALUE format
        if ($_ -match '^\s*([^#=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            
            # Remove quotes if present
            if ($value -match '^"(.*)"$' -or $value -match "^'(.*)'$") {
                $value = $matches[1]
            }
            
            # Set environment variable
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
            Write-Host "  Set $key" -ForegroundColor Gray
        }
    }
    Write-Host "Environment variables loaded successfully!" -ForegroundColor Green
} else {
    Write-Host "Warning: .env file not found!" -ForegroundColor Yellow
}

Write-Host "`nStarting Spring Boot backend..." -ForegroundColor Cyan
Write-Host "Database: Neon PostgreSQL" -ForegroundColor Yellow
Write-Host "`n"

# Run the backend
./gradlew bootRun

