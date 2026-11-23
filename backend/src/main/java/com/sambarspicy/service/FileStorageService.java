package com.sambarspicy.service;

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
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Pattern MANAGED_IMAGE = Pattern.compile("(?:cat|dish)-[0-9]+-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(?:jpg|jpeg|png|webp)");

    private final Path uploadLocation;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadLocation);
        } catch (IOException e) {
            log.warn("Could not create upload directory: {}", this.uploadLocation, e);
        }
    }

    public String storeFile(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg");
        String extension = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx >= 0) {
            extension = originalFilename.substring(dotIdx).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file extension. Allowed: " + ALLOWED_EXTENSIONS);
        }

        if (file.getSize() >= 100_000) {
            throw new IllegalArgumentException("Stored image must be less than 100 KB");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            boolean jpeg = header.length >= 3 && (header[0] & 255) == 0xff && (header[1] & 255) == 0xd8 && (header[2] & 255) == 0xff;
            boolean png = header.length >= 8 && java.util.Arrays.equals(java.util.Arrays.copyOf(header, 8), new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
            boolean webp = header.length >= 12 && new String(header, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF") && new String(header, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
            if (!((jpeg && (extension.equals(".jpg") || extension.equals(".jpeg"))) ||
                    (png && extension.equals(".png")) || (webp && extension.equals(".webp")))) {
                throw new IllegalArgumentException("Image contents do not match the file type");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read image", e);
        }

        String safePrefix = (prefix != null && !prefix.isBlank()) ? prefix + "-" : "";
        String storedFilename = safePrefix + UUID.randomUUID() + extension;

        Path targetPath = this.uploadLocation.resolve(storedFilename);
        try {
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + storedFilename;
        } catch (IOException e) {
            try { Files.deleteIfExists(targetPath); } catch (IOException cleanup) { e.addSuppressed(cleanup); }
            throw new RuntimeException("Failed to store file " + originalFilename, e);
        }
    }

    // Only delete names created by this application, never paths supplied by clients.
    public void deleteManagedImage(String imagePath) {
        if (imagePath == null || !imagePath.startsWith("/uploads/")) return;
        String name = imagePath.substring("/uploads/".length());
        if (!MANAGED_IMAGE.matcher(name).matches()) return;
        try {
            Files.deleteIfExists(uploadLocation.resolve(name));
        } catch (IOException e) {
            log.warn("Could not remove obsolete uploaded image {}", imagePath, e);
        }
    }
}
