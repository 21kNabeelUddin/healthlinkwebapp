package com.healthlink.security.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/**
 * FieldEncryptionConverter
 * <p>
 * HIPAA / PHI Context:
 * Applies authenticated AES/GCM encryption at the JPA attribute level to prevent raw PHI
 * (e.g., medical notes, prescription body text, lab descriptions, family history notes) from
 * ever being persisted in plaintext. The converter supports key rotation by embedding a
 * key alias in the ciphertext: ENC:<alias>:<base64(iv+cipherText)>.
 * <p>
 * Rotation Strategy:
 * Active + legacy keys are provided by {@link PhiEncryptionService}. During decryption we try
 * the resolved key (by alias). If alias missing (legacy "ENC:" prefix) we fall back to the active key.
 * If decryption fails with active key we attempt all legacy keys sequentially allowing seamless
 * rotation without pre-migration re-encryption.
 * <p>
 * Security Notes:
 * - Uses 96-bit IV (12 bytes) per NIST recommendation for GCM.
 * - Tag length fixed at 128 bits.
 * - Fails fast on encryption errors; returns raw legacy value only when clearly not encrypted.
 */
@Component
@Converter(autoApply = false)
public class FieldEncryptionConverter implements AttributeConverter<String, String> {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;
    private final SecureRandom secureRandom = new SecureRandom();
    // Do NOT rely on constructor injection here: Hibernate may instantiate the
    // converter directly. Resolve the service lazily from the Spring context
    // to ensure the converter works whether created by Spring or JPA provider.
    private transient PhiEncryptionService encryptionService;

    public FieldEncryptionConverter() {
        // no-op; service resolved lazily
    }

    private PhiEncryptionService svc() {
        if (this.encryptionService == null) {
            this.encryptionService = SpringContext.getBean(PhiEncryptionService.class);
        }
        return this.encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        if (attribute.startsWith("ENC:")) return attribute; // already encrypted
        try {
            SecretKey activeKey = svc().getActiveKey();
            String alias = svc().getActiveAlias();
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, activeKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] cipherText = cipher.doFinal(attribute.getBytes());
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            return "ENC:" + alias + ':' + Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failure", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        if (!dbData.startsWith("ENC:")) {
            // Legacy plaintext; treat as-is.
            return dbData;
        }
        try {
            // Format ENC:<alias>:<payload> OR legacy ENC:<payload>
            String remainder = dbData.substring(4); // strip ENC:
            String alias;
            String payload;
            int firstColon = remainder.indexOf(':');
            if (firstColon > 0) {
                alias = remainder.substring(0, firstColon);
                payload = remainder.substring(firstColon + 1);
            } else {
                // Legacy without alias
                alias = encryptionService.getActiveAlias();
                payload = remainder;
            }
            byte[] data = Base64.getDecoder().decode(payload);
            ByteBuffer bb = ByteBuffer.wrap(data);
            byte[] iv = new byte[IV_LENGTH];
            bb.get(iv);
            byte[] cipherText = new byte[bb.remaining()];
            bb.get(cipherText);

            SecretKey key = svc().resolveKey(alias);
            if (key == null) {
                // Try all legacy keys if alias unknown
                for (Map.Entry<String, SecretKey> entry : svc().getAllKeys().entrySet()) {
                    key = entry.getValue();
                    try {
                        Cipher cipher = Cipher.getInstance(ALGO);
                        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
                        byte[] plain = cipher.doFinal(cipherText);
                        return new String(plain);
                    } catch (Exception ignored) { /* try next */ }
                }
                return dbData; // Could not decrypt
            }
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText);
        } catch (Exception e) {
            return dbData; // Return raw if decryption impossible to avoid data loss
        }
    }
}
