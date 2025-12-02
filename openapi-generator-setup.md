# OpenAPI Generator Setup for HealthLink Mobile App

This guide explains how to use OpenAPI Generator to generate Flutter/Dart DTOs and API clients from the `healthlink-api.yml` specification.

## Prerequisites

1. **Install OpenAPI Generator CLI**
   ```bash
   # Using npm
   npm install -g @openapitools/openapi-generator-cli
   
   # Or using Homebrew (macOS)
   brew install openapi-generator
   
   # Or using Docker
   docker pull openapitools/openapi-generator-cli
   ```

2. **Verify Installation**
   ```bash
   openapi-generator-cli version
   ```

## Generate Flutter/Dart Code

### Option 1: Using the Configuration File

```bash
openapi-generator-cli generate \
  -i healthlink-api.yml \
  -g dart \
  -o mobile_app/lib/generated/api \
  -c openapi-generator-config.yaml
```

### Option 2: Direct Command (without config file)

```bash
openapi-generator-cli generate \
  -i healthlink-api.yml \
  -g dart \
  -o mobile_app/lib/generated/api \
  --additional-properties=pubName=healthlink_api,pubVersion=1.0.0,useEnumExtension=true,enumUnknownDefaultCase=true,serializationLibrary=json_serializable,dateLibrary=core,nullableFields=true
```

### Option 3: Using Docker

```bash
docker run --rm \
  -v ${PWD}:/local \
  openapitools/openapi-generator-cli generate \
  -i /local/healthlink-api.yml \
  -g dart \
  -o /local/mobile_app/lib/generated/api \
  -c /local/openapi-generator-config.yaml
```

## Generated Code Structure

After generation, you'll have:

```
mobile_app/lib/generated/api/
├── lib/
│   ├── api/              # API client classes
│   ├── models/           # DTO models
│   └── client.dart       # Main API client
├── pubspec.yaml          # Dart package dependencies
└── README.md             # Generated documentation
```

## Integration with Flutter App

1. **Add Generated Package to `pubspec.yaml`**
   ```yaml
   dependencies:
     healthlink_api:
       path: lib/generated/api
   ```

2. **Import and Use**
   ```dart
   import 'package:healthlink_api/api.dart';
   
   final api = HealthLinkApi();
   final authResponse = await api.auth.login(LoginRequest(
     email: 'user@example.com',
     password: 'password',
   ));
   ```

## Regenerating Code

When the API spec changes:

1. Update `healthlink-api.yml`
2. Run the generator command again
3. Review generated code for breaking changes
4. Update your Flutter app code accordingly

## Troubleshooting

### Issue: Missing Dependencies
If generated code has missing dependencies, add them to `pubspec.yaml`:
```yaml
dependencies:
  dio: ^5.0.0
  json_annotation: ^4.8.0
  built_value: ^8.0.0  # If using built_value serialization
```

### Issue: Type Mismatches
- Check that `healthlink-api.yml` matches the backend implementation
- Verify date/time formats match between spec and backend
- Ensure enum values match exactly

### Issue: Authentication Headers
The generated client should handle JWT tokens. Configure it:
```dart
final api = HealthLinkApi();
api.setBearerAuth('your-access-token');
```

## Next Steps

1. Review generated models for accuracy
2. Create wrapper services in your Flutter app
3. Implement error handling
4. Add retry logic for network failures
5. Set up token refresh mechanism

## References

- [OpenAPI Generator Documentation](https://openapi-generator.tech/)
- [Dart Generator Options](https://openapi-generator.tech/docs/generators/dart)
- [Flutter HTTP Best Practices](https://docs.flutter.dev/cookbook/networking)

