package com.votum.votum_backend.service;

import com.votum.votum_backend.dto.RegisterRequest;
import com.votum.votum_backend.dto.UserProfileResponse;
import com.votum.votum_backend.model.User;
import com.votum.votum_backend.model.UserBiometrics;
import com.votum.votum_backend.repository.UserRepository;
import com.votum.votum_backend.repository.UserBiometricsRepository;
import com.votum.votum_backend.security.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserBiometricsRepository biometricsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final FileStorageService fileStorageService;

    // ================= REGISTER =================

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
        user.setStatus("PENDING");   // IMPORTANT
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        // Save files with UUID-based filenames inside structured folders
        String photoUuid = UUID.randomUUID().toString();
        String aadhaarUuid = UUID.randomUUID().toString();

        String photoPath = fileStorageService.saveFile(
                photo, "users", "photos", photoUuid + ".jpg");

        String aadhaarPath = fileStorageService.saveFile(
                aadhaarPdf, "users", "aadhaar", aadhaarUuid + ".pdf");

        UserBiometrics biometrics = new UserBiometrics();
        biometrics.setUser(user);
        biometrics.setPhotoPath(photoPath);
        biometrics.setAadhaarPdfPath(aadhaarPath);
        biometrics.setFaceEmbedding(new byte[0]);

        biometricsRepository.save(biometrics);
    }

    // ================= HASH AADHAAR =================

    private String hashAadhaar(String aadhaar) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder()
                    .encodeToString(md.digest(aadhaar.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Aadhaar hashing failed");
        }
    }

    // ================= LOGIN =================

    public String login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        return jwtUtil.generateToken(email, user.getRole());
    }

    // ================= GET PROFILE =================

    public UserProfileResponse getProfile(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserBiometrics biometrics =
                biometricsRepository.findById(user.getId()).orElse(null);

        return UserProfileResponse.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .dob(user.getDob())
                .gender(user.getGender())
                .address(user.getAddress())
                .status(user.getStatus())
                .photoPath(biometrics != null ? biometrics.getPhotoPath() : null)
                .aadhaarPdfPath(biometrics != null ? biometrics.getAadhaarPdfPath() : null)
                .build();
    }

    // ================= ADMIN - GET PENDING USERS =================

    public List<UserProfileResponse> getPendingUsers() {

        List<User> users = userRepository.findByStatus("PENDING");

        return users.stream()
                .map(user -> UserProfileResponse.builder()
                        .fullName(user.getFullName())
                        .userId(user.getId().toString())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .dob(user.getDob())
                        .gender(user.getGender())
                        .address(user.getAddress())
                        .status(user.getStatus())
                        .build()
                )
                .toList();
    }

    // ================= ADMIN - APPROVE =================

    public void approveUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus("APPROVED");
        userRepository.save(user);
    }

    // ================= ADMIN - REJECT =================

    public void rejectUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus("REJECTED");
        userRepository.save(user);
    }

    public List<UserProfileResponse> getAllUsers() {

        return userRepository.findAll().stream()
                .map(user -> UserProfileResponse.builder()
                        .userId(user.getId().toString())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .dob(user.getDob())
                        .gender(user.getGender())
                        .address(user.getAddress())
                        .status(user.getStatus())
                        .build()
                )
                .toList();
    }

}
