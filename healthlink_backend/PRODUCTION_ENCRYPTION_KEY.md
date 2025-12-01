# Generating Production PHI Encryption Key

## Quick Generation Methods

### Method 1: Using OpenSSL (Recommended)
```bash
# Generate a secure 32-byte (256-bit) key, base64-encoded
openssl rand -base64 32
```

**Output example:**
```
K8j3mN9pQ2vR7tY5wX1zA4bC6dE8fG0hI2jK4lM6nO8pQ0rS2tU4vW6xY8zA=
```

### Method 2: Using PowerShell (Windows)
```powershell
# Generate secure random bytes and encode to base64
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

### Method 3: Using Online Tools (Less Secure)
- Visit: https://generate-random.org/api-key-generator?count=1&length=32&type=base64
- Or use: https://www.random.org/strings/?num=1&len=32&digits=on&upperalpha=on&loweralpha=on&unique=on&format=base64

⚠️ **Warning**: Online tools are less secure. Use only if you trust the service.

### Method 4: Using Python
```python
import secrets
import base64

# Generate 32 random bytes
key_bytes = secrets.token_bytes(32)
# Encode to base64
key_base64 = base64.b64encode(key_bytes).decode('utf-8')
print(key_base64)
```

## Setting the Key in Production

### Railway
1. Go to your project → **Variables** tab
2. Click **+ New Variable**
3. Name: `PHI_ENCRYPTION_KEY`
4. Value: Paste your generated key
5. Click **Add**

### Render
1. Go to your service → **Environment** tab
2. Click **Add Environment Variable**
3. Key: `PHI_ENCRYPTION_KEY`
4. Value: Paste your generated key
5. Click **Save Changes**

### Fly.io
```bash
# Using fly CLI
fly secrets set PHI_ENCRYPTION_KEY="your-generated-key-here"
```

Or in Fly.io dashboard:
1. Go to your app → **Secrets** tab
2. Click **Add Secret**
3. Key: `PHI_ENCRYPTION_KEY`
4. Value: Paste your generated key

### AWS/GCP/Azure
Set as environment variable in your deployment configuration:
- **AWS Elastic Beanstalk**: Environment → Configuration → Software → Environment Properties
- **GCP Cloud Run**: Container → Environment Variables
- **Azure App Service**: Configuration → Application Settings
- **Render**: Environment → Environment Variables
- **Railway**: Variables tab in your service settings

### Using `.env` file (don't commit to git!):
```env
PHI_ENCRYPTION_KEY=your-generated-key-here
```

## Security Best Practices

1. ✅ **Never commit the key to Git**
   - Add to `.gitignore`
   - Use environment variables or secrets management

2. ✅ **Use different keys for each environment**
   - Development: Default key (okay for local)
   - Staging: Separate key
   - Production: Unique, strong key

3. ✅ **Rotate keys periodically**
   - Generate new key
   - Update environment variable
   - Re-encrypt existing data (requires migration)

4. ✅ **Store securely**
   - Use platform secrets management (Railway, Render, etc.)
   - Consider AWS Secrets Manager, HashiCorp Vault for enterprise

5. ✅ **Backup the key securely**
   - Store in password manager (1Password, LastPass)
   - Encrypt backup files
   - Never store in plain text files

## Verification

After setting the key, verify it's loaded:
1. Check application logs for: "Using default development PHI encryption key..."
   - ✅ If you DON'T see this warning → Key is set correctly
   - ⚠️ If you DO see this warning → Key not set, using default

2. Test encryption/decryption in your application

## Key Format Requirements

- **Length**: Must be >= 32 characters (after base64 decode = 32 bytes)
- **Format**: Base64-encoded string (recommended) OR plain 32-character string
- **Example valid keys**:
  - Base64: `K8j3mN9pQ2vR7tY5wX1zA4bC6dE8fG0hI2jK4lM6nO8pQ0rS2tU4vW6xY8zA=`
  - Plain: `abcdefghijklmnopqrstuvwxyz123456` (exactly 32 chars)

## Troubleshooting

**Error: "encryption-key must be provided and >=32 chars"**
- Check environment variable is set correctly
- Verify no extra spaces or quotes
- Ensure key is at least 32 characters

**Error: "Provided key (after decode) must be >=32 bytes"**
- Base64 key decodes to less than 32 bytes
- Generate a new key using Method 1 (OpenSSL)

## Example: Complete Setup for Railway

```bash
# 1. Generate key
openssl rand -base64 32
# Output: xY9kP2mN7qR4tW8vZ1bC3dE5fG7hI9jK1lM3nO5pQ7rS9tU1vW3xY5zA7bC9dE=

# 2. In Railway dashboard:
#    - Go to Variables
#    - Add: PHI_ENCRYPTION_KEY = xY9kP2mN7qR4tW8vZ1bC3dE5fG7hI9jK1lM3nO5pQ7rS9tU1vW3xY5zA7bC9dE=
#    - Deploy

# 3. Verify in logs (should NOT see default key warning)
```

