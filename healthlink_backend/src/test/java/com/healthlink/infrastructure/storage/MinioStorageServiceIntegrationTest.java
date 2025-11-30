package com.healthlink.infrastructure.storage;

import com.healthlink.config.TestSearchConfig;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.*;

/**
 * MinIO Integration Tests
 * 
 * Requirements:
 * - MinIO server running (Docker: docker-compose up -d minio)
 * - Bucket configured in application-test.yml
 * 
 * Tests cover:
 * - File upload with various content types
 * - Presigned URL generation
 * - File size validation
 * - Content type validation
 * - Error handling
 */
@SpringBootTest(properties = {
    // Use H2 in-memory database for tests
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    // Disable Liquibase migrations for tests
    "spring.liquibase.enabled=false"
})
@ActiveProfiles("test")
@Import(TestSearchConfig.class)
@DisplayName("MinIO Storage Integration Tests")
class MinioStorageServiceIntegrationTest {

    @Autowired
    private MinioStorageService minioStorageService;

    @Autowired
    private MinioClient minioClient;

    @Value("${healthlink.storage.bucket}")
    private String bucketName;

    @BeforeEach
    void setUp() throws Exception {
        // Ensure bucket exists before tests
        boolean bucketExists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(bucketName).build()
        );
        
        if (!bucketExists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder().bucket(bucketName).build()
            );
        }
    }

    @Test
    @DisplayName("Should upload PNG image successfully")
    void shouldUploadPngImage() {
        // Given
        byte[] imageContent = createTestImageBytes();
        MultipartFile file = new MockMultipartFile(
            "test-image.png",
            "test-image.png",
            "image/png",
            imageContent
        );

        // When
        String fileName = minioStorageService.uploadFile(file);

        // Then
        assertThat(fileName).isNotNull();
        assertThat(fileName).contains("test-image.png");
        assertThat(fileName).matches("^[0-9a-f-]+-test-image\\.png$");

        // Cleanup
        cleanupFile(fileName);
    }

    @Test
    @DisplayName("Should upload JPEG image successfully")
    void shouldUploadJpegImage() {
        // Given
        byte[] imageContent = createTestImageBytes();
        MultipartFile file = new MockMultipartFile(
            "test-photo.jpg",
            "test-photo.jpg",
            "image/jpeg",
            imageContent
        );

        // When
        String fileName = minioStorageService.uploadFile(file);

        // Then
        assertThat(fileName).isNotNull();
        assertThat(fileName).endsWith(".jpg");

        // Cleanup
        cleanupFile(fileName);
    }

    @Test
    @DisplayName("Should upload PDF document successfully")
    void shouldUploadPdfDocument() {
        // Given
        byte[] pdfContent = createTestPdfBytes();
        MultipartFile file = new MockMultipartFile(
            "medical-report.pdf",
            "medical-report.pdf",
            "application/pdf",
            pdfContent
        );

        // When
        String fileName = minioStorageService.uploadFile(file);

        // Then
        assertThat(fileName).isNotNull();
        assertThat(fileName).endsWith(".pdf");

        // Cleanup
        cleanupFile(fileName);
    }

    @Test
    @DisplayName("Should generate presigned URL for uploaded file")
    void shouldGeneratePresignedUrl() {
        // Given: Upload a file first
        MultipartFile file = new MockMultipartFile(
            "test.png",
            "test.png",
            "image/png",
            createTestImageBytes()
        );
        String fileName = minioStorageService.uploadFile(file);

        // When
        String presignedUrl = minioStorageService.generatePresignedUrl(fileName);

        // Then
        assertThat(presignedUrl).isNotNull();
        assertThat(presignedUrl).startsWith("http");
        assertThat(presignedUrl).contains(bucketName);
        assertThat(presignedUrl).contains(fileName);
        assertThat(presignedUrl).contains("X-Amz-Algorithm=");
        assertThat(presignedUrl).contains("X-Amz-Credential=");
        assertThat(presignedUrl).contains("X-Amz-Signature=");

        // Cleanup
        cleanupFile(fileName);
    }

    @Test
    @DisplayName("Should reject empty file")
    void shouldRejectEmptyFile() {
        // Given
        MultipartFile emptyFile = new MockMultipartFile(
            "empty.png",
            "empty.png",
            "image/png",
            new byte[0]
        );

        // When/Then
        assertThatThrownBy(() -> minioStorageService.uploadFile(emptyFile))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Empty file not allowed");
    }

    @Test
    @DisplayName("Should reject null file")
    void shouldRejectNullFile() {
        // When/Then
        assertThatThrownBy(() -> minioStorageService.uploadFile(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Empty file not allowed");
    }

    @Test
    @DisplayName("Should reject file exceeding 5MB size limit")
    void shouldRejectOversizedFile() {
        // Given: Create 6MB file
        byte[] largeContent = new byte[6 * 1024 * 1024];
        MultipartFile largeFile = new MockMultipartFile(
            "large.png",
            "large.png",
            "image/png",
            largeContent
        );

        // When/Then
        assertThatThrownBy(() -> minioStorageService.uploadFile(largeFile))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File exceeds max size 5MB");
    }

    @Test
    @DisplayName("Should reject unsupported content type")
    void shouldRejectUnsupportedContentType() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "document.docx",
            "document.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            new byte[100]
        );

        // When/Then
        assertThatThrownBy(() -> minioStorageService.uploadFile(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported content type");
    }

    @Test
    @DisplayName("Should reject file with null content type")
    void shouldRejectNullContentType() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "unknown",
            "unknown",
            null,
            new byte[100]
        );

        // When/Then
        assertThatThrownBy(() -> minioStorageService.uploadFile(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported content type");
    }

    @Test
    @DisplayName("Should accept file at exactly 5MB limit")
    void shouldAcceptFileAtSizeLimit() {
        // Given: Create exactly 5MB file
        byte[] content = new byte[5 * 1024 * 1024];
        MultipartFile file = new MockMultipartFile(
            "large-but-valid.png",
            "large-but-valid.png",
            "image/png",
            content
        );

        // When
        String fileName = minioStorageService.uploadFile(file);

        // Then
        assertThat(fileName).isNotNull();

        // Cleanup
        cleanupFile(fileName);
    }

    @Test
    @DisplayName("Should handle special characters in filename")
    void shouldHandleSpecialCharactersInFilename() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "test file with spaces & special_chars-123.png",
            "test file with spaces & special_chars-123.png",
            "image/png",
            createTestImageBytes()
        );

        // When
        String fileName = minioStorageService.uploadFile(file);

        // Then
        assertThat(fileName).isNotNull();
        assertThat(fileName).contains("test file with spaces & special_chars-123.png");

        // Cleanup
        cleanupFile(fileName);
    }

    @Test
    @DisplayName("Should upload multiple files sequentially")
    void shouldUploadMultipleFiles() {
        // Given
        String[] fileNames = new String[3];

        // When: Upload 3 files
        for (int i = 0; i < 3; i++) {
            MultipartFile file = new MockMultipartFile(
                "file-" + i + ".png",
                "file-" + i + ".png",
                "image/png",
                createTestImageBytes()
            );
            fileNames[i] = minioStorageService.uploadFile(file);
        }

        // Then
        for (String fileName : fileNames) {
            assertThat(fileName).isNotNull();
        }

        // Verify all files have unique names (UUID prefix)
        assertThat(fileNames[0]).isNotEqualTo(fileNames[1]);
        assertThat(fileNames[1]).isNotEqualTo(fileNames[2]);

        // Cleanup
        for (String fileName : fileNames) {
            cleanupFile(fileName);
        }
    }

    @Test
    @DisplayName("Should upload GIF image successfully")
    void shouldUploadGifImage() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "animation.gif",
            "animation.gif",
            "image/gif",
            createTestImageBytes()
        );

        // When
        String fileName = minioStorageService.uploadFile(file);

        // Then
        assertThat(fileName).isNotNull();
        assertThat(fileName).endsWith(".gif");

        // Cleanup
        cleanupFile(fileName);
    }

    // ==================== Helper Methods ====================

    /**
     * Create test image bytes (minimal valid PNG header)
     */
    private byte[] createTestImageBytes() {
        // PNG signature + minimal valid PNG data
        byte[] pngHeader = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x0D, // IHDR chunk length
            0x49, 0x48, 0x44, 0x52, // IHDR chunk type
            0x00, 0x00, 0x00, 0x01, // Width: 1
            0x00, 0x00, 0x00, 0x01, // Height: 1
            0x08, 0x02, 0x00, 0x00, 0x00, // Bit depth, color type, compression, filter, interlace
        };
        byte[] fullImage = new byte[pngHeader.length + 100]; // Add some padding
        System.arraycopy(pngHeader, 0, fullImage, 0, pngHeader.length);
        return fullImage;
    }

    /**
     * Create test PDF bytes (minimal valid PDF)
     */
    private byte[] createTestPdfBytes() {
        String pdfContent = "%PDF-1.4\n" +
            "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n" +
            "2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj\n" +
            "3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj\n" +
            "xref\n0 4\n0000000000 65535 f\n0000000009 00000 n\n" +
            "0000000056 00000 n\n0000000114 00000 n\n" +
            "trailer<</Size 4/Root 1 0 R>>\nstartxref\n199\n%%EOF";
        return pdfContent.getBytes();
    }

    /**
     * Cleanup uploaded file from MinIO
     */
    private void cleanupFile(String fileName) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build()
            );
        } catch (Exception e) {
            // Log but don't fail test
            System.err.println("Failed to cleanup file: " + fileName);
        }
    }
}
