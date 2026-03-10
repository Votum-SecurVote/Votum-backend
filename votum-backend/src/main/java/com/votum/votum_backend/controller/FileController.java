package com.votum.votum_backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * FileController.
 * Serves stored files (user photos, Aadhaar PDFs) from the secure storage path.
 * The path parameter is relative to the configured storage root and is sanitized
 * to prevent directory traversal attacks.
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${file.storage.path}")
    private String storagePath;

    /**
     * Serves a file by its relative path within the storage root.
     *
     * Example: GET /api/files/users/photos/abc-uuid.jpg
     *          → serves {storagePath}/users/photos/abc-uuid.jpg
     */
    @GetMapping("/**")
    public ResponseEntity<Resource> serveFile(
            @RequestParam(required = false) String dummy,
            jakarta.servlet.http.HttpServletRequest request) throws MalformedURLException {

        // Extract the relative sub-path from the URL after /api/files/
        String requestPath = request.getRequestURI();
        String prefix = "/api/files/";
        String relativePath = requestPath.substring(requestPath.indexOf(prefix) + prefix.length());

        // Resolve and normalize to prevent directory traversal
        Path storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
        Path filePath = storageRoot.resolve(relativePath).normalize();

        // Security check: ensure the resolved path is within the storage root
        if (!filePath.startsWith(storageRoot)) {
            return ResponseEntity.badRequest().build();
        }

        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        // Determine content type from file extension
        String filename = filePath.getFileName().toString().toLowerCase();
        MediaType contentType = filename.endsWith(".pdf")
                ? MediaType.APPLICATION_PDF
                : MediaType.IMAGE_JPEG;

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
