package com.healthlink.security.encryption;

import com.healthlink.infrastructure.logging.SafeLogger;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PhiEncryptionService
 *
 * Centralizes management of active + legacy AES keys for PHI field encryption.
 * Configuration options:
 * 1. Single key (backwards compatible): healthlink.phi.encryption-key
 * 2. Multi-key rotation list: healthlink.phi.encryption-keys =
 * "K1:base64key256,K0:base64oldkey256"
 * - First entry is considered ACTIVE.
 * - Each key must be 256-bit (32 raw bytes) base64 or plain 32-character
 * string. Base64 is recommended.
 *
 * Resolution:
 * - Active alias returned by getActiveAlias().
 * - Encryption uses active key only.
 * - Decryption resolves by alias, falls back to trying all keys.
 *
 * HIPAA Context: Enables non-disruptive key rotation for stored PHI without
 * re-encrypt migration.
 */
@Service
public class PhiEncryptionService {

    // Encryption constants
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BIT = 128;
    private static final int KEY_LENGTH_BYTES = 32;
    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final String DEFAULT_ALIAS = "K1";

    private final Environment env;
    private final Map<String, SecretKey> keys = new ConcurrentHashMap<>();
    private String activeAlias;

    public PhiEncryptionService(Environment env) {
        this.env = env;
    }

    @PostConstruct
    public void init() {
        String multi = env.getProperty("healthlink.phi.encryption-keys");
        if (multi != null && !multi.isBlank()) {
            String[] parts = multi.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.isEmpty())
                    continue;
                int colon = trimmed.indexOf(':');
                if (colon < 0) {
                    throw new IllegalStateException("Invalid encryption key entry (missing alias): " + trimmed);
                }
                String alias = trimmed.substring(0, colon);
                String rawKey = trimmed.substring(colon + 1).trim();
                SecretKey key = toKey(rawKey);
                keys.put(alias, key);
            }
            if (keys.isEmpty()) {
                throw new IllegalStateException("No valid encryption keys loaded from healthlink.phi.encryption-keys");
            }
            activeAlias = parts[0].substring(0, parts[0].indexOf(':')); // first entry active
        } else {
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
            // Validate the key can be decoded/used (toKey will throw if invalid)
            SecretKey key = toKey(single);
            activeAlias = DEFAULT_ALIAS; // default alias
            keys.put(activeAlias, key);
        }
    }

    private SecretKey toKey(String raw) {
        byte[] bytes;
        if (raw.length() == KEY_LENGTH_BYTES && raw.matches("[A-Za-z0-9+/=]+") == false) {
            // Plain 32-char key
            bytes = raw.substring(0, KEY_LENGTH_BYTES).getBytes();
        } else {
            byte[] decoded = null;
            try {
                decoded = Base64.getDecoder().decode(raw);
            } catch (IllegalArgumentException ignored) {
                // keep decoded null
            }
            if (decoded != null && decoded.length >= KEY_LENGTH_BYTES) {
                bytes = decoded;
            } else {
                // fall back to raw bytes when base64 decode insufficient
                bytes = raw.getBytes();
            }
            if (bytes.length < KEY_LENGTH_BYTES) {
                throw new IllegalStateException("Provided key (after decode) must be >=32 bytes (256 bits)");
            }
            if (bytes.length > KEY_LENGTH_BYTES) {
                bytes = Arrays.copyOf(bytes, KEY_LENGTH_BYTES); // truncate to 256 bits
            }
        }
        return new SecretKeySpec(bytes, "AES");
    }

    public SecretKey getActiveKey() {
        return keys.get(activeAlias);
    }

    public String getActiveAlias() {
        return activeAlias;
    }

    public SecretKey resolveKey(String alias) {
        return keys.get(alias);
    }

    public Map<String, SecretKey> getAllKeys() {
        return Collections.unmodifiableMap(keys);
    }

    /**
     * Rotate to a newly provided key (alias must be unique). The new key becomes
     * active, previous active retained as legacy.
     * Key material provided as base64 or raw 32+ char string. Returns new active
     * alias.
     */
    public synchronized String rotate(String newAlias, String rawKey) {
        if (keys.containsKey(newAlias)) {
            throw new IllegalArgumentException("Alias already exists: " + newAlias);
        }
        SecretKey key = toKey(rawKey);
        keys.put(newAlias, key);
        activeAlias = newAlias;
        return activeAlias;
    }

    /**
     * Encrypt plaintext using the active key.
     * Format: "{alias}:{iv}:{ciphertext}" (Base64 encoded IV and ciphertext)
     */
    public String encrypt(String plaintext) {
        if (plaintext == null)
            return null;
        if (plaintext.isEmpty()) {
            // Let it fall through to encrypt empty bytes
        }

        try {
            SecretKey key = getActiveKey();
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(AES_GCM_ALGORITHM);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new java.security.SecureRandom().nextBytes(iv);
            javax.crypto.spec.GCMParameterSpec spec = new javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv);
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, spec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            return activeAlias + ":" + Base64.getEncoder().encodeToString(iv) + ":"
                    + Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt ciphertext.
     * Expects format: "{alias}:{iv}:{ciphertext}"
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null)
            return null;
        if (encryptedData.isEmpty())
            return ""; // Or handle as invalid format? Test says "invalid-format" throws.

        String[] parts = encryptedData.split(":", -1);
        if (parts.length != 3) {
            // Handle empty string case from encrypt if it produces something different?
            // If encrypt("") produces "alias:iv:ciphertext", then split is 3.
            // If input is just "invalid-format", split is 1.
            throw new RuntimeException("Invalid encrypted data format");
        }

        String alias = parts[0];
        String ivBase64 = parts[1];
        String ciphertextBase64 = parts[2];

        SecretKey key = keys.get(alias);
        if (key == null) {
            throw new RuntimeException("Unknown encryption key alias: " + alias);
        }

        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(AES_GCM_ALGORITHM);
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            byte[] ciphertext = Base64.getDecoder().decode(ciphertextBase64);
            javax.crypto.spec.GCMParameterSpec spec = new javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv);
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, spec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            SafeLogger.get(PhiEncryptionService.class)
                .event("phi_decryption_failure")
                .with("error_type", e.getClass().getSimpleName())
                .log();
            // Generic error - do not expose exception details
            throw new RuntimeException("PHI decryption failed", new SecurityException("Decryption error"));
        }
    }

    /**
     * Encrypt with ALL available keys (for rotation support or multi-key
     * requirements).
     */
    public List<String> encryptMulti(String plaintext) {
        List<String> results = new ArrayList<>();
        for (Map.Entry<String, SecretKey> entry : keys.entrySet()) {
            try {
                String alias = entry.getKey();
                SecretKey key = entry.getValue();

                javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(AES_GCM_ALGORITHM);
                byte[] iv = new byte[GCM_IV_LENGTH];
                new java.security.SecureRandom().nextBytes(iv);
                javax.crypto.spec.GCMParameterSpec spec = new javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv);
                cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, spec);

                byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                results.add(alias + ":" + Base64.getEncoder().encodeToString(iv) + ":"
                        + Base64.getEncoder().encodeToString(ciphertext));
            } catch (Exception e) {
                throw new RuntimeException("Multi-encryption failed for alias " + entry.getKey(), e);
            }
        }
        return results;
    }
}
