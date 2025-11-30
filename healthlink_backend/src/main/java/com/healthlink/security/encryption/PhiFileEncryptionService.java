package com.healthlink.security.encryption;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * PhiFileEncryptionService
 * <p>
 * Provides AES-256-GCM encryption for binary PHI data (images, documents, lab results).
 * Files are encrypted before upload to MinIO and decrypted on retrieval.
 * <p>
 * Format: [12-byte IV][ciphertext with GCM tag]
 * <p>
 * HIPAA Context:
 * - Payment receipts containing patient identification
 * - Lab result images/scans
 * - Medical record attachments
 * - Any uploaded PHI documents
 * <p>
 * Security:
 * - Uses 256-bit AES with GCM authenticated encryption
 * - 96-bit IV (12 bytes) per NIST recommendation
 * - 128-bit authentication tag
 * - Keys managed by PhiEncryptionService for rotation support
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhiFileEncryptionService {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;
    private static final int BUFFER_SIZE = 8192;
    
    private final PhiEncryptionService phiEncryptionService;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Encrypt binary data (file content) using AES-256-GCM.
     * 
     * @param data The plaintext binary data to encrypt
     * @return Encrypted data with prepended IV
     * @throws IllegalStateException if encryption fails
     */
    public byte[] encryptFile(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Cannot encrypt null or empty data");
        }
        
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, phiEncryptionService.getActiveKey(), 
                       new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            
            byte[] ciphertext = cipher.doFinal(data);
            
            // Prepend IV to ciphertext
            ByteBuffer buffer = ByteBuffer.allocate(IV_LENGTH + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            
            log.debug("PHI file encrypted: {} bytes -> {} bytes", data.length, buffer.array().length);
            return buffer.array();
            
        } catch (Exception e) {
            log.error("PHI file encryption failed", e);
            throw new IllegalStateException("Failed to encrypt PHI file data", e);
        }
    }

    /**
     * Encrypt an InputStream and return encrypted InputStream.
     * Reads entire stream into memory for encryption.
     * 
     * @param inputStream The plaintext input stream
     * @param contentLength Expected content length for buffer allocation
     * @return InputStream of encrypted data
     * @throws IllegalStateException if encryption fails
     */
    public InputStream encryptStream(InputStream inputStream, long contentLength) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream((int) contentLength);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            
            byte[] encrypted = encryptFile(baos.toByteArray());
            return new ByteArrayInputStream(encrypted);
            
        } catch (Exception e) {
            log.error("PHI stream encryption failed", e);
            throw new IllegalStateException("Failed to encrypt PHI stream", e);
        }
    }

    /**
     * Decrypt binary data encrypted with encryptFile().
     * 
     * @param encryptedData The encrypted data (IV + ciphertext)
     * @return Decrypted plaintext data
     * @throws IllegalStateException if decryption fails
     */
    public byte[] decryptFile(byte[] encryptedData) {
        if (encryptedData == null || encryptedData.length <= IV_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted data: too short or null");
        }
        
        try {
            ByteBuffer buffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            
            // Try active key first
            try {
                return decryptWithKey(iv, ciphertext, phiEncryptionService.getActiveKey());
            } catch (Exception e) {
                log.debug("Decryption with active key failed, trying legacy keys");
            }
            
            // Try all legacy keys
            for (var entry : phiEncryptionService.getAllKeys().entrySet()) {
                try {
                    return decryptWithKey(iv, ciphertext, entry.getValue());
                } catch (Exception ignored) {
                    // Try next key
                }
            }
            
            throw new IllegalStateException("Failed to decrypt with any available key");
            
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("PHI file decryption failed", e);
            throw new IllegalStateException("Failed to decrypt PHI file data", e);
        }
    }

    /**
     * Decrypt an InputStream and return decrypted InputStream.
     * 
     * @param encryptedStream The encrypted input stream
     * @return InputStream of decrypted data
     * @throws IllegalStateException if decryption fails
     */
    public InputStream decryptStream(InputStream encryptedStream) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = encryptedStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            
            byte[] decrypted = decryptFile(baos.toByteArray());
            return new ByteArrayInputStream(decrypted);
            
        } catch (Exception e) {
            log.error("PHI stream decryption failed", e);
            throw new IllegalStateException("Failed to decrypt PHI stream", e);
        }
    }

    /**
     * Get the encrypted size for a given plaintext size.
     * Useful for content-length calculations.
     * 
     * @param plaintextSize Original file size
     * @return Expected encrypted size (IV + ciphertext + GCM tag)
     */
    public long getEncryptedSize(long plaintextSize) {
        // IV (12 bytes) + plaintext + GCM tag (16 bytes)
        return IV_LENGTH + plaintextSize + (TAG_LENGTH_BIT / 8);
    }

    private byte[] decryptWithKey(byte[] iv, byte[] ciphertext, javax.crypto.SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        return cipher.doFinal(ciphertext);
    }
}
