package com.votum.votum_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@Service
public class FileStorageService {

    @Value("${file.storage.path}")
    private String storagePath;

    /**
     * Saves a MultipartFile to the storage root at the path formed by joining
     * the given sub-path segments, then the given filename.
     *
     * Example:
     *   saveFile(file, "users", "photos", "abc-uuid.jpg")
     *   → {storagePath}/users/photos/abc-uuid.jpg
     *
     * @param file     the uploaded file
     * @param pathParts path segments relative to storagePath; the last element is the filename
     * @return the absolute path string where the file was saved
     * @throws IOException if the directory cannot be created or the file cannot be written
     */
    public String saveFile(MultipartFile file, String... pathParts) throws IOException {
        if (pathParts == null || pathParts.length == 0) {
            throw new IllegalArgumentException("At least one path segment must be provided");
        }

        // Build full path: storagePath / pathParts[0] / pathParts[1] / ... / filename
        Path targetPath = Paths.get(storagePath, pathParts);

        // Ensure parent directories exist
        Files.createDirectories(targetPath.getParent());

        // Copy file, replacing if exists
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return targetPath.toAbsolutePath().toString();
    }

    /**
     * Ensures a directory exists at the given sub-path relative to storagePath.
     *
     * @param subPaths path segments relative to storagePath
     * @throws IOException if the directory cannot be created
     */
    public void createDirectory(String... subPaths) throws IOException {
        Path dir = Paths.get(storagePath, subPaths);
        Files.createDirectories(dir);
    }
}
