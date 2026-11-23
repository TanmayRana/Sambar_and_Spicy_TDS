package com.sambarspicy;

import com.sambarspicy.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {
    @TempDir Path directory;

    @Test
    void storesImagesOnlyBelowTheLimit() throws Exception {
        FileStorageService service = new FileStorageService(directory.toString());
        byte[] data = new byte[99_999];
        byte[] header = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        System.arraycopy(header, 0, data, 0, header.length);
        String stored = service.storeFile(new MockMultipartFile("file", "image.png", "image/png", data), "dish");
        assertEquals(data.length, Files.size(directory.resolve(stored.substring("/uploads/".length()))));

        byte[] tooLarge = new byte[100_000];
        System.arraycopy(header, 0, tooLarge, 0, header.length);
        assertThrows(IllegalArgumentException.class, () -> service.storeFile(
                new MockMultipartFile("file", "image.png", "image/png", tooLarge), "dish"));
    }
}
