package com.civicpulse.storage;

import com.civicpulse.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/pjpeg"
    );

    private final Path storageLocation;

    public FileStorageService(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create upload directory at " + this.storageLocation, ex);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File size exceeds the maximum allowed limit of 10MB");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "file"));
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < originalFilename.length() - 1) {
            extension = originalFilename.substring(dotIndex + 1).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Invalid file format. Only JPG, JPEG, PNG, and WEBP images are supported.");
        }

        validateFile(file, extension);

        String storedFilename = UUID.randomUUID() + "." + extension;
        Path targetLocation = this.storageLocation.resolve(storedFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {} -> {}", originalFilename, storedFilename);
            return "/uploads/" + storedFilename;
        } catch (IOException ex) {
            log.error("Failed to store file: {}", originalFilename, ex);
            throw new RuntimeException("Failed to store file on disk", ex);
        }
    }

    private void validateFile(MultipartFile file, String extension) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String normalizedMime = contentType.toLowerCase().trim();
            if (!ALLOWED_MIME_TYPES.contains(normalizedMime)) {
                throw new BadRequestException("Unsupported media type: " + contentType + ". Allowed types: JPEG, PNG, WEBP");
            }

            if (("jpg".equals(extension) || "jpeg".equals(extension))
                    && !normalizedMime.contains("jpeg") && !normalizedMime.contains("jpg")) {
                throw new BadRequestException("MIME type (" + contentType + ") does not match file extension (." + extension + ")");
            }
            if ("png".equals(extension) && !normalizedMime.contains("png")) {
                throw new BadRequestException("MIME type (" + contentType + ") does not match file extension (." + extension + ")");
            }
            if ("webp".equals(extension) && !normalizedMime.contains("webp")) {
                throw new BadRequestException("MIME type (" + contentType + ") does not match file extension (." + extension + ")");
            }
        }

        byte[] header = new byte[12];
        try (InputStream is = file.getInputStream()) {
            int bytesRead = is.readNBytes(header, 0, 12);
            if (bytesRead < 12) {
                throw new BadRequestException("Uploaded file is corrupted or too small to be a valid image");
            }
        } catch (IOException ex) {
            log.error("Failed to read header bytes for file validation", ex);
            throw new BadRequestException("Unable to read file for validation");
        }

        boolean matchesExtension = false;
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            matchesExtension = isJpeg(header);
        } else if ("png".equals(extension)) {
            matchesExtension = isPng(header);
        } else if ("webp".equals(extension)) {
            matchesExtension = isWebp(header);
        }

        if (!matchesExtension) {
            throw new BadRequestException("File signature (magic bytes) does not match the declared extension: ." + extension);
        }
    }

    private boolean isJpeg(byte[] h) {
        return (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] h) {
        return (h[0] & 0xFF) == 0x89 && h[1] == 0x50 && h[2] == 0x4E && h[3] == 0x47
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A;
    }

    private boolean isWebp(byte[] h) {
        return h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
    }
}
