package com.healthlink.security.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests round-trip encryption/decryption and legacy compatibility for FieldEncryptionConverter.
 * Ensures HIPAA PHI text cannot be persisted plaintext and supports key rotation.
 */
class FieldEncryptionConverterTest {

    private FieldEncryptionConverter converter;
    private PhiEncryptionService service;

    @BeforeEach
    void setUp() {
        MockEnvironment env = new MockEnvironment();
        // Provide two keys (active + legacy) for rotation test
        // 32-char plain keys for simplicity here (would be base64 normally)
        env.setProperty("healthlink.phi.encryption-keys", "K2:ABCDEFGHIJKLMNOPQRSTUVWXYZ123456,K1:ZYXWVUTSRQPONMLKJIHGFEDCBA654321");
        service = new PhiEncryptionService(env);
        service.init();
        
        // Mock SpringContext to return our service
        ApplicationContext mockContext = mock(ApplicationContext.class);
        when(mockContext.getBean(PhiEncryptionService.class)).thenReturn(service);
        ReflectionTestUtils.setField(SpringContext.class, "applicationContext", mockContext);
        
        converter = new FieldEncryptionConverter();
    }

    @Test
    void encryptAndDecryptRoundTrip() {
        String plain = "Sensitive PHI notes: hypertension and diabetes";
        String encrypted = converter.convertToDatabaseColumn(plain);
        assertNotEquals(plain, encrypted);
        assertTrue(encrypted.startsWith("ENC:"));
        String decrypted = converter.convertToEntityAttribute(encrypted);
        assertEquals(plain, decrypted);
    }

    @Test
    void legacyPlaintextPassthrough() {
        String legacy = "Legacy non-encrypted text";
        String out = converter.convertToEntityAttribute(legacy);
        assertEquals(legacy, out);
    }

    @Test
    void decryptWithUnknownAliasFallsBack() {
        String plain = "Rotation test PHI";
        String encrypted = converter.convertToDatabaseColumn(plain);
        // Tamper alias to simulate old unknown alias
        String tampered = encrypted.replace("ENC:" + service.getActiveAlias(), "ENC:UNKNOWN");
        String decrypted = converter.convertToEntityAttribute(tampered);
        assertEquals(plain, decrypted); // should succeed via fallback legacy key attempts
    }
}
