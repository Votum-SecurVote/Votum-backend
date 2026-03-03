package com.votum.votum_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.votum.votum_backend.dto.LoginRequest;
import com.votum.votum_backend.model.Admin;
import com.votum.votum_backend.model.User;
import com.votum.votum_backend.repository.AdminRepository;
import com.votum.votum_backend.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Authentication Flow
 * Tests the full HTTP flow from Controller → Service → Repository → Database
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Authentication Integration Tests")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Admin testAdmin;

    @BeforeEach
    void setUp() {
        // Clean database
        userRepository.deleteAll();
        adminRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setFullName("John Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setPhone("9876543210");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setAadhaarHash("KjM0nn5gaorS4w48hFIfk3dFDPCQg+Fi4KmxSAzg+XI=");
        testUser.setRole("USER");
        testUser.setDob(LocalDate.of(1990, 1, 1));
        testUser.setGender("Male");
        testUser.setAddress("123 Main Street");
        testUser.setStatus("APPROVED");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        // Create test admin
        testAdmin = new Admin();
        testAdmin.setFullName("Admin User");
        testAdmin.setEmail("admin@votum.com");
        testAdmin.setPasswordHash(passwordEncoder.encode("admin123"));
        testAdmin.setCreatedAt(LocalDateTime.now());
        testAdmin = adminRepository.save(testAdmin);
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
    @DisplayName("Should successfully login user and return JWT token")
    void testUserLogin_Success() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john.doe@example.com");
        loginRequest.setPassword("password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string(notNullValue()))
                .andExpect(jsonPath("$").isString()); // JWT token is returned as string
    }

    @Test
    @DisplayName("Should return 401 when user login with invalid credentials")
    void testUserLogin_InvalidCredentials() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john.doe@example.com");
        loginRequest.setPassword("wrongpassword");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Should return 401 when login with non-existent user")
    void testUserLogin_UserNotFound() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent@example.com");
        loginRequest.setPassword("password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Should successfully login admin and return JWT token")
    void testAdminLogin_Success() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@votum.com");
        loginRequest.setPassword("admin123");

        // Act & Assert
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string(notNullValue()))
                .andExpect(jsonPath("$").isString()); // JWT token
    }

    @Test
    @DisplayName("Should return 401 when admin login with invalid credentials")
    void testAdminLogin_InvalidCredentials() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@votum.com");
        loginRequest.setPassword("wrongpassword");

        // Act & Assert
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Should return 403 when accessing protected endpoint without token")
    void testProtectedEndpoint_NoToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 when accessing protected endpoint with invalid token")
    void testProtectedEndpoint_InvalidToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer invalid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 200 when accessing protected endpoint with valid token")
    void testProtectedEndpoint_ValidToken() throws Exception {
        // Arrange - First login to get token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john.doe@example.com");
        loginRequest.setPassword("password123");

        String token = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Act & Assert - Access protected endpoint with token
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    @Test
    @DisplayName("Should return 403 when USER tries to access ADMIN endpoint")
    void testRoleBasedAccess_UserAccessingAdminEndpoint() throws Exception {
        // Arrange - Login as user
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john.doe@example.com");
        loginRequest.setPassword("password123");

        String token = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Act & Assert - Try to access admin endpoint
        mockMvc.perform(get("/api/admin/elections")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 200 when ADMIN accesses admin endpoint with valid token")
    void testRoleBasedAccess_AdminAccessingAdminEndpoint() throws Exception {
        // Arrange - Login as admin
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@votum.com");
        loginRequest.setPassword("admin123");

        String token = mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Act & Assert - Access admin endpoint
        mockMvc.perform(get("/api/admin/elections")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
