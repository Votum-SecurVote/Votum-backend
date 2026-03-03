package com.votum.votum_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.votum.votum_backend.dto.LoginRequest;
import com.votum.votum_backend.model.User;
import com.votum.votum_backend.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for User Flow
 * Tests user registration, duplicate prevention, and profile fetching
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("User Integration Tests")
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean database
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setFullName("Jane Smith");
        testUser.setEmail("jane.smith@example.com");
        testUser.setPhone("9876543210");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setAadhaarHash("KjM0nn5gaorS4w48hFIfk3dFDPCQg+Fi4KmxSAzg+XI=");
        testUser.setRole("USER");
        testUser.setDob(LocalDate.of(1995, 5, 15));
        testUser.setGender("Female");
        testUser.setAddress("456 Oak Avenue");
        testUser.setStatus("APPROVED");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);
    }

    @AfterAll
    static void cleanUpTestStorage() {
        // Clean up test_storage folder after all tests
        try {
            Path testStoragePath = Paths.get("test_storage");
            if (Files.exists(testStoragePath)) {
                FileSystemUtils.deleteRecursively(testStoragePath);
                System.out.println("✓ Cleaned up test_storage directory");
            }
        } catch (IOException e) {
            System.err.println("Failed to clean up test_storage: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void testRegisterUser_Success() throws Exception {
        // Arrange
        String registerData = """
                {
                    "fullName": "Alice Johnson",
                    "email": "alice.johnson@example.com",
                    "phone": "9123456789",
                    "password": "securepass123",
                    "aadhaar": "987654321012",
                    "dob": "1992-03-20",
                    "gender": "Female",
                    "address": "789 Pine Street"
                }
                """;

        MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                "application/json",
                registerData.getBytes()
        );

        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "photo.jpg",
                "image/jpeg",
                "fake photo content".getBytes()
        );

        MockMultipartFile aadhaarPdf = new MockMultipartFile(
                "aadhaarPdf",
                "aadhaar.pdf",
                "application/pdf",
                "fake pdf content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/auth/register")
                        .file(data)
                        .file(photo)
                        .file(aadhaarPdf))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully. Await admin approval."));

        // Verify user is saved in database
        User savedUser = userRepository.findByEmail("alice.johnson@example.com").orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getFullName()).isEqualTo("Alice Johnson");
        assertThat(savedUser.getPhone()).isEqualTo("9123456789");
        assertThat(savedUser.getStatus()).isEqualTo("PENDING");
        assertThat(savedUser.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Should fail when registering user with duplicate email")
    void testRegisterUser_DuplicateEmail() throws Exception {
        // Arrange - Use existing user's email
        String registerData = String.format("""
                {
                    "fullName": "Duplicate User",
                    "email": "%s",
                    "phone": "9999999999",
                    "password": "password123",
                    "aadhaar": "111111111111",
                    "dob": "1990-01-01",
                    "gender": "Male",
                    "address": "Test Address"
                }
                """, testUser.getEmail());

        MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                "application/json",
                registerData.getBytes()
        );

        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "photo.jpg",
                "image/jpeg",
                "fake photo content".getBytes()
        );

        MockMultipartFile aadhaarPdf = new MockMultipartFile(
                "aadhaarPdf",
                "aadhaar.pdf",
                "application/pdf",
                "fake pdf content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/auth/register")
                        .file(data)
                        .file(photo)
                        .file(aadhaarPdf))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should successfully fetch user profile with valid token")
    void testGetUserProfile_Success() throws Exception {
        // Arrange - Login to get token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("jane.smith@example.com");
        loginRequest.setPassword("password123");

        String token = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Act & Assert
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane.smith@example.com"))
                .andExpect(jsonPath("$.fullName").value("Jane Smith"))
                .andExpect(jsonPath("$.phone").value("9876543210"))
                .andExpect(jsonPath("$.gender").value("Female"))
                .andExpect(jsonPath("$.address").value("456 Oak Avenue"));
    }

    @Test
    @DisplayName("Should return 403 when fetching profile without token")
    void testGetUserProfile_NoToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should successfully fetch published elections")
    void testGetPublishedElections_Success() throws Exception {
        // Arrange - Login to get token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("jane.smith@example.com");
        loginRequest.setPassword("password123");

        String token = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Act & Assert
        mockMvc.perform(get("/api/user/elections")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should verify user exists in database after registration")
    void testUserPersistence() throws Exception {
        // Arrange
        String registerData = """
                {
                    "fullName": "Bob Wilson",
                    "email": "bob.wilson@example.com",
                    "phone": "9111111111",
                    "password": "bobpass123",
                    "aadhaar": "222222222222",
                    "dob": "1988-07-10",
                    "gender": "Male",
                    "address": "321 Elm Street"
                }
                """;

        MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                "application/json",
                registerData.getBytes()
        );

        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "photo.jpg",
                "image/jpeg",
                "fake photo content".getBytes()
        );

        MockMultipartFile aadhaarPdf = new MockMultipartFile(
                "aadhaarPdf",
                "aadhaar.pdf",
                "application/pdf",
                "fake pdf content".getBytes()
        );

        // Act
        mockMvc.perform(multipart("/api/auth/register")
                        .file(data)
                        .file(photo)
                        .file(aadhaarPdf))
                .andExpect(status().isOk());

        // Assert - Check database directly
        User savedUser = userRepository.findByEmail("bob.wilson@example.com").orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getStatus()).isEqualTo("PENDING");

        // Verify password is hashed
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("bobpass123");
        assertThat(passwordEncoder.matches("bobpass123", savedUser.getPasswordHash())).isTrue();
    }
}
