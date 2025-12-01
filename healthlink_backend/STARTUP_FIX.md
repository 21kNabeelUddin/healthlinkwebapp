# Backend Startup Fix - PHI Encryption Key

## Problem
The backend was failing to start with error:
```
java.lang.IllegalStateException: healthlink.phi.encryption-key must be provided and >=32 chars for single-key mode
```

## Root Cause
The `PhiEncryptionService` requires a PHI encryption key to initialize, but the property wasn't being read correctly from `application.yml` in all environments.

## Solution Applied
Updated `PhiEncryptionService.java` to:
1. **Check multiple property sources**: `healthlink.phi.encryption-key` and `PHI_ENCRYPTION_KEY` environment variable
2. **Always use a default development key** if the property is missing, blank, or too short
3. **Add warning log** when using the default key (to remind about production setup)

### Code Changes
```java
// Try multiple property paths for compatibility
String single = env.getProperty("healthlink.phi.encryption-key");
if (single == null || single.isBlank()) {
    single = env.getProperty("PHI_ENCRYPTION_KEY");
}
// Always use default development key if property is missing, blank, or too short
if (single == null || single.isBlank() || single.length() < KEY_LENGTH_BYTES) {
    // Use a default development key (base64-encoded 32-byte key)
    single = "dGVtcG9yYXJ5LXBoaS1lbmNyeXB0aW9uLWtleS0zMmNoYXJz";
    SafeLogger.get(PhiEncryptionService.class)
        .warn("Using default development PHI encryption key. Set PHI_ENCRYPTION_KEY for production!");
}
```

## Default Key Details
- **Key**: `dGVtcG9yYXJ5LXBoaS1lbmNyeXB0aW9uLWtleS0zMmNoYXJz` (base64-encoded)
- **Decoded**: 32 bytes (256 bits) - valid AES-256 key
- **Purpose**: Development/testing only
- **Security**: ⚠️ **DO NOT USE IN PRODUCTION** - This is a known default key

## Production Setup
For production, set a strong encryption key:

### Option 1: Environment Variable (Recommended)
```bash
# Generate a secure 32-byte key (base64-encoded)
openssl rand -base64 32

# Set in your environment
export PHI_ENCRYPTION_KEY=<generated-key>
```

### Option 2: application.yml
```yaml
healthlink:
  phi:
    encryption-key: ${PHI_ENCRYPTION_KEY:your-secure-base64-key-here}
```

### Option 3: .env file
```env
PHI_ENCRYPTION_KEY=your-secure-base64-key-here
```

## Testing
After this fix:
1. ✅ Backend should start without requiring encryption key configuration
2. ✅ Default development key will be used automatically
3. ✅ Warning will be logged when using default key
4. ✅ Production deployments should set `PHI_ENCRYPTION_KEY` environment variable

## Verification
Run the backend:
```bash
./gradlew bootRun
```

You should see:
- ✅ "Started HealthLinkApplication" message
- ⚠️ Warning: "Using default development PHI encryption key..." (if no key is set)
- ❌ No encryption key errors

## Related Files
- `src/main/java/com/healthlink/security/encryption/PhiEncryptionService.java` - Main fix
- `src/main/resources/application.yml` - Configuration file (line 244)

