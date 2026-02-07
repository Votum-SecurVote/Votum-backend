package com.votum.votum_backend.service;

import com.votum.votum_backend.dto.RegisterRequest;
import com.votum.votum_backend.model.User;
import com.votum.votum_backend.model.UserBiometrics;
import com.votum.votum_backend.repository.UserRepository;
import com.votum.votum_backend.repository.UserBiometricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserBiometricsRepository biometricsRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${file.storage.path}")
    private String storagePath;

    public void register(RegisterRequest request,
                         MultipartFile photo,
                         MultipartFile aadhaarPdf) throws IOException {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone already exists");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAadhaarHash(hashAadhaar(request.getAadhaar()));
        user.setDob(request.getDob());
        user.setGender(request.getGender());
        user.setAddress(request.getAddress());
        user.setStatus("PENDING");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        // Save files
        String photoPath = saveFile(photo, "photos");
        String aadhaarPath = saveFile(aadhaarPdf, "aadhaar");

        UserBiometrics biometrics = new UserBiometrics();
        biometrics.setUser(user);
        biometrics.setPhotoPath(photoPath);
        biometrics.setAadhaarPdfPath(aadhaarPath);
        biometrics.setFaceEmbedding(new byte[0]);

        biometricsRepository.save(biometrics);
    }

    private String saveFile(MultipartFile file, String folder) throws IOException {
        Path dir = Paths.get(storagePath, folder);
        Files.createDirectories(dir);

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = dir.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString();
    }

    private String hashAadhaar(String aadhaar) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(aadhaar.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Aadhaar hashing failed");
        }
    }
}
