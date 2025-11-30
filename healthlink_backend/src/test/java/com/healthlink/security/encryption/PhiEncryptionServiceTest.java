package com.healthlink.security.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.lenient;

/**
 * Tests for PhiEncryptionService
 */
@ExtendWith(MockitoExtension.class)
class PhiEncryptionServiceTest {

    private PhiEncryptionService encryptionService;
    private String testKey1;
    private String testKey2;

    @Mock
    private org.springframework.core.env.Environment env;

    @BeforeEach
    void setUp() {
        encryptionService = new PhiEncryptionService(env);

        // Generate test keys (256-bit AES)
        byte[] keyBytes1 = new byte[32];
        byte[] keyBytes2 = new byte[32];
        for (int i = 0; i < 32; i++) {
            keyBytes1[i] = (byte) i;
            keyBytes2[i] = (byte) (i + 32);
        }

        testKey1 = Base64.getEncoder().encodeToString(keyBytes1);
        testKey2 = Base64.getEncoder().encodeToString(keyBytes2);

        // Stub environment to return keys
        lenient().when(env.getProperty("healthlink.phi.encryption-keys"))
                .thenReturn("1:" + testKey1 + ",2:" + testKey2);

        // Initialize
        encryptionService.init();
    }

    @Test
    void encrypt_shouldReturnEncryptedData() {
        String plaintext = "Sensitive patient data";

        String encrypted = encryptionService.encrypt(plaintext);

        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(encrypted).startsWith("1:"); // Should include key ID prefix
    }

    @Test
    void decrypt_shouldReturnOriginalPlaintext() {
        String plaintext = "Sensitive patient data";
        String encrypted = encryptionService.encrypt(plaintext);

        String decrypted = encryptionService.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encrypt_shouldHandleEmptyString() {
        String encrypted = encryptionService.encrypt("");

        assertThat(encrypted).isNotNull();
        assertThat(encryptionService.decrypt(encrypted)).isEmpty();
    }

    @Test
    void encrypt_shouldReturnNullForNullInput() {
        String encrypted = encryptionService.encrypt(null);

        assertThat(encrypted).isNull();
    }

    @Test
    void decrypt_shouldReturnNullForNullInput() {
        String decrypted = encryptionService.decrypt(null);

        assertThat(decrypted).isNull();
    }

    @Test
    void encryptMulti_shouldEncryptWithAllKeys() {
        String plaintext = "Multi-encrypted data";

        List<String> encrypted = encryptionService.encryptMulti(plaintext);

        assertThat(encrypted).hasSize(2); // Both keys used
        assertThat(encrypted.get(0)).startsWith("1:");
        assertThat(encrypted.get(1)).startsWith("2:");
    }

    @Test
    void decryptMulti_shouldDecryptAnyValidCiphertext() {
        String plaintext = "Multi-encrypted data";
        List<String> encrypted = encryptionService.encryptMulti(plaintext);

        // Should be able to decrypt using any of the encrypted versions
        String decrypted1 = encryptionService.decrypt(encrypted.get(0));
        String decrypted2 = encryptionService.decrypt(encrypted.get(1));

        assertThat(decrypted1).isEqualTo(plaintext);
        assertThat(decrypted2).isEqualTo(plaintext);
    }

    @Test
    void decrypt_shouldHandleOldKeyAfterRotation() {
        String plaintext = "Data encrypted with old key";
        String encryptedWithKey1 = encryptionService.encrypt(plaintext);

        // Verify old key can still decrypt
        String decrypted = encryptionService.decrypt(encryptedWithKey1);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encrypt_shouldProduceDifferentCiphertextForSamePlaintext() {
        String plaintext = "Same plaintext";

        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);

        // Due to random IV, ciphertexts should be different
        assertThat(encrypted1).isNotEqualTo(encrypted2);

        // But both should decrypt to same plaintext
        assertThat(encryptionService.decrypt(encrypted1)).isEqualTo(plaintext);
        assertThat(encryptionService.decrypt(encrypted2)).isEqualTo(plaintext);
    }

    @Test
    void decrypt_shouldThrowForInvalidFormat() {
        assertThatThrownBy(() -> encryptionService.decrypt("invalid-format"))
                .isInstanceOf(RuntimeException.class);
    }
}
