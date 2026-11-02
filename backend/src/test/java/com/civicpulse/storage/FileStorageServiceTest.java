package com.civicpulse.storage;

import com.civicpulse.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    void testValidJpegUploadSucceeds() {
        byte[] validJpeg = new byte[] {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x01, 0x00, 0x60
        };

        MockMultipartFile file = new MockMultipartFile(
                "photo", "road_pothole.jpg", "image/jpeg", validJpeg
        );

        String fileUrl = fileStorageService.storeFile(file);
        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/uploads/"));
        assertTrue(fileUrl.endsWith(".jpg"));
    }

    @Test
    void testValidPngUploadSucceeds() {
        byte[] validPng = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        };

        MockMultipartFile file = new MockMultipartFile(
                "photo", "screenshot.png", "image/png", validPng
        );

        String fileUrl = fileStorageService.storeFile(file);
        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/uploads/"));
        assertTrue(fileUrl.endsWith(".png"));
    }

    @Test
    void testValidWebpUploadSucceeds() {
        byte[] validWebp = new byte[] {
                'R', 'I', 'F', 'F',
                0x24, 0x00, 0x00, 0x00,
                'W', 'E', 'B', 'P',
                'V', 'P', '8', ' '
        };

        MockMultipartFile file = new MockMultipartFile(
                "photo", "damage.webp", "image/webp", validWebp
        );

        String fileUrl = fileStorageService.storeFile(file);
        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/uploads/"));
        assertTrue(fileUrl.endsWith(".webp"));
    }

    @Test
    void testSpoofedExtensionRejectedByMagicByteCheck() {
        byte[] fakeContent = "This is a plain text file pretending to be a JPG".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile spoofedFile = new MockMultipartFile(
                "photo", "malicious.jpg", "image/jpeg", fakeContent
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                fileStorageService.storeFile(spoofedFile)
        );
        assertTrue(ex.getMessage().contains("magic bytes") || ex.getMessage().contains("signature"));
    }

    @Test
    void testDisallowedExtensionRejected() {
        byte[] data = "some script".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile badFile = new MockMultipartFile(
                "file", "script.sh", "application/x-sh", data
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                fileStorageService.storeFile(badFile)
        );
        assertTrue(ex.getMessage().contains("Invalid file format"));
    }

    @Test
    void testMimeExtensionMismatchRejected() {
        byte[] validPng = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        };

        // PNG payload with .jpg extension
        MockMultipartFile mismatched = new MockMultipartFile(
                "photo", "sneaky.jpg", "image/png", validPng
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                fileStorageService.storeFile(mismatched)
        );
        assertTrue(ex.getMessage().contains("does not match file extension"));
    }

    @Test
    void testEmptyFileRejected() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "photo", "empty.jpg", "image/jpeg", new byte[0]
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                fileStorageService.storeFile(emptyFile)
        );
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void testCorruptedTruncatedFileRejected() {
        byte[] shortBytes = new byte[] { (byte) 0xFF, (byte) 0xD8 }; // only 2 bytes

        MockMultipartFile shortFile = new MockMultipartFile(
                "photo", "truncated.jpg", "image/jpeg", shortBytes
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                fileStorageService.storeFile(shortFile)
        );
        assertTrue(ex.getMessage().contains("corrupted") || ex.getMessage().contains("too small"));
    }
}
