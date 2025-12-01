package com.healthlink.infrastructure.storage;

import com.healthlink.security.encryption.PhiFileEncryptionService;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * MinioStorageService
 * <p>
 * Handles secure file uploads/downloads to MinIO object storage with PHI encryption.
 * All uploaded files are encrypted using AES-256-GCM before storage and decrypted on retrieval.
 * <p>
 * HIPAA Context:
 * - Payment receipts are encrypted at rest
 * - Lab result uploads are encrypted at rest
 * - Medical document attachments are encrypted at rest
 * <p>
 * Security:
 * - Files encrypted using PhiFileEncryptionService before upload
 * - Presigned URLs provide time-limited access
 * - Content type and size validation before upload
 */
@Service
@ConditionalOnProperty(prefix = "healthlink.storage", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;
    private final PhiFileEncryptionService phiFileEncryptionService;

    @Value("${healthlink.storage.bucket:healthlink-records}")
    private String bucketName;

    @Value("${healthlink.storage.encryption-enabled:true}")
    private boolean encryptionEnabled;

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final int READ_BUFFER_SIZE = 8192;
    private static final java.util.Set<String> ALLOWED_CONTENT_TYPES = java.util.Set.of(
            "image/png", "image/jpeg", "application/pdf", "image/gif"
    );

    /**
     * Upload a file with PHI encryption.
     * 
     * @param file The file to upload
     * @return The stored object name (UUID-prefixed)
     * @throws IllegalArgumentException if file validation fails
     * @throws RuntimeException if upload fails
     */
    public String uploadFile(MultipartFile file) {
        try {
            validateFile(file);
            
            String fileName = UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());
            byte[] fileData = file.getBytes();
            
            byte[] dataToStore;
            long contentLength;
            
            if (encryptionEnabled) {
                // Encrypt file data before upload (PHI protection)
                dataToStore = phiFileEncryptionService.encryptFile(fileData);
                contentLength = dataToStore.length;
                log.debug("PHI file encrypted for upload: {} -> {} bytes", fileData.length, contentLength);
            } else {
                dataToStore = fileData;
                contentLength = file.getSize();
            }
            
            try (InputStream inputStream = new ByteArrayInputStream(dataToStore)) {
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(inputStream, contentLength, -1)
                        .contentType(encryptionEnabled ? "application/octet-stream" : file.getContentType())
                        .build()
                );
            }
            
            log.info("PHI file uploaded successfully: {}", fileName);
            return fileName;
            
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    /**
     * Download and decrypt a file from MinIO.
     * 
     * @param objectName The object name to retrieve
     * @return Decrypted file data
     * @throws RuntimeException if download or decryption fails
     */
    public byte[] downloadFile(String objectName) {
        try {
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build())) {
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[READ_BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = stream.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                
                byte[] encryptedData = baos.toByteArray();
                
                if (encryptionEnabled) {
                    // Decrypt file data (PHI protection)
                    byte[] decrypted = phiFileEncryptionService.decryptFile(encryptedData);
                    log.debug("PHI file decrypted: {} -> {} bytes", encryptedData.length, decrypted.length);
                    return decrypted;
                }
                
                return encryptedData;
            }
        } catch (Exception e) {
            log.error("Failed to download file from MinIO: {}", objectName, e);
            throw new RuntimeException("Failed to download file from MinIO", e);
        }
    }

    /**
     * Generate a presigned URL for direct file access.
     * Note: If encryption is enabled, the URL will return encrypted data.
     * Use downloadFile() for decrypted access.
     * 
     * @param objectName The object name
     * @return Presigned URL valid for 1 hour
     * @throws RuntimeException if URL generation fails
     */
    public String generatePresignedUrl(String objectName) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(60 * 60) // 1 hour
                    .build()
            );
            
            if (encryptionEnabled) {
                log.warn("Presigned URL generated for encrypted file. " +
                         "Client must handle decryption or use downloadFile() API.");
            }
            
            return url;
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for: {}", objectName, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    /**
     * Check if a file exists in the bucket.
     * 
     * @param objectName The object name to check
     * @return true if file exists
     */
    public boolean fileExists(String objectName) {
        try {
            minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .length(1L) // Only fetch first byte to check existence
                    .build()
            ).close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file not allowed");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds max size 5MB");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported content type: " + contentType);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unnamed";
        }
        // Remove path separators and suspicious characters
        return filename.replaceAll("[/\\\\:*?\"<>|]", "_");
    }
}

