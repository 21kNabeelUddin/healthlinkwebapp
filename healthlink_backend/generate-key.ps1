# Generate Production PHI Encryption Key
# Run this script to generate a secure encryption key for production

Write-Host "Generating secure PHI encryption key..." -ForegroundColor Green

# Generate 32 random bytes and encode to base64
$keyBytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($keyBytes)
$keyBase64 = [Convert]::ToBase64String($keyBytes)

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Your Production PHI Encryption Key:" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host $keyBase64 -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Copy this key and set it as PHI_ENCRYPTION_KEY in your deployment platform." -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Railway: Project → Variables → Add PHI_ENCRYPTION_KEY" -ForegroundColor White
Write-Host "2. Render: Service → Environment → Add PHI_ENCRYPTION_KEY" -ForegroundColor White
Write-Host "3. Fly.io: fly secrets set PHI_ENCRYPTION_KEY=`"$keyBase64`"" -ForegroundColor White
Write-Host ""
Write-Host "⚠️  IMPORTANT: Keep this key secure! Never commit it to Git." -ForegroundColor Red

