package com.votum.votum_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.votum.votum_backend.dto.LoginRequest;
import com.votum.votum_backend.dto.RegisterRequest;
import com.votum.votum_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private MockMultipartFile photoFile;
    private MockMultipartFile aadhaarFile;
    private MockMultipartFile dataFile;

    @BeforeEach
    void setUp() throws Exception {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("John Doe");
        registerRequest.setEmail("john.doe@example.com");
        registerRequest.setPhone("1234567890");
        registerRequest.setPassword("password123");
        registerRequest.setAadhaar("123456789012");
        registerRequest.setDob(LocalDate.of(1990, 1, 1));
        registerRequest.setGender("Male");
        registerRequest.setAddress("123 Main St");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("john.doe@example.com");
        loginRequest.setPassword("password123");

        // Create ObjectMapper with JavaTimeModule for proper serialization
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String jsonData = mapper.writeValueAsString(registerRequest);

        dataFile = new MockMultipartFile("data", "", "application/json", jsonData.getBytes());
        photoFile = new MockMultipartFile("photo", "photo.jpg", "image/jpeg", "photo-content".getBytes());
        aadhaarFile = new MockMultipartFile("aadhaarPdf", "aadhaar.pdf", "application/pdf", "pdf-content".getBytes());
    }

    @Test
    @DisplayName("Should register user successfully with valid data")
    void register_Success() throws Exception {
        // Arrange
        doNothing().when(userService).register(any(RegisterRequest.class), any(), any());

        // Act & Assert
        mockMvc.perform(multipart("/api/auth/register")
                .file(dataFile)
                .file(photoFile)
                .file(aadhaarFile)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully. Await admin approval."));

        verify(userService).register(any(RegisterRequest.class), any(), any());
    }

    @Test
    @DisplayName("Should handle service exception during registration")
    void register_ServiceException_ReturnsBadRequest() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Email already exists"))
                .when(userService).register(any(RegisterRequest.class), any(), any());

        // Act & Assert
        mockMvc.perform(multipart("/api/auth/register")
                .file(dataFile)
                .file(photoFile)
                .file(aadhaarFile)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error parsing request: Email already exists"));

        verify(userService).register(any(RegisterRequest.class), any(), any());
    }

    @Test
    @DisplayName("Should handle invalid JSON in registration")
    void register_InvalidJson_ReturnsBadRequest() throws Exception {
        // Arrange
        MockMultipartFile invalidDataFile = new MockMultipartFile("data", "", "application/json", "invalid-json".getBytes());

        // Act & Assert
        mockMvc.perform(multipart("/api/auth/register")
                .file(invalidDataFile)
                .file(photoFile)
                .file(aadhaarFile)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Error parsing request:")));

        verify(userService, never()).register(any(RegisterRequest.class), any(), any());
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void login_Success() throws Exception {
        // Arrange
        String expectedToken = "jwt-token-12345";
        when(userService.login("john.doe@example.com", "password123")).thenReturn(expectedToken);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("\"" + expectedToken + "\""));

        verify(userService).login("john.doe@example.com", "password123");
    }

    @Test
    @DisplayName("Should handle login with invalid credentials")
    void login_InvalidCredentials_ThrowsException() throws Exception {
        // Arrange
        when(userService.login("john.doe@example.com", "wrongpassword"))
                .thenThrow(new RuntimeException("Invalid password"));

        loginRequest.setPassword("wrongpassword");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError());

        verify(userService).login("john.doe@example.com", "wrongpassword");
    }

    @Test
    @DisplayName("Should handle login with non-existent user")
    void login_UserNotFound_ThrowsException() throws Exception {
        // Arrange
        when(userService.login("nonexistent@example.com", "password123"))
                .thenThrow(new RuntimeException("User not found"));

        loginRequest.setEmail("nonexistent@example.com");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError());

        verify(userService).login("nonexistent@example.com", "password123");
    }

    @Test
    @DisplayName("Should handle malformed login request")
    void login_MalformedRequest_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"invalid\": \"json\"}"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).login(anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle missing required files in registration")
    void register_MissingFiles_ReturnsBadRequest() throws Exception {
        // Act & Assert - Missing photo file
        mockMvc.perform(multipart("/api/auth/register")
                .file(dataFile)
                .file(aadhaarFile)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any(RegisterRequest.class), any(), any());
    }

    @Test
    @DisplayName("Should handle empty login request body")
    void login_EmptyRequest_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());

        verify(userService, never()).login(anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle null values in login request")
    void login_NullValues_ThrowsException() throws Exception {
        // Arrange
        LoginRequest nullRequest = new LoginRequest();
        nullRequest.setEmail(null);
        nullRequest.setPassword(null);

        when(userService.login(null, null))
                .thenThrow(new RuntimeException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nullRequest)))
                .andExpect(status().isInternalServerError());

        verify(userService).login(null, null);
    }

    @Test
    @DisplayName("Should register with proper content type multipart")
    void register_ProperContentType_Success() throws Exception {
        // Arrange
        doNothing().when(userService).register(any(RegisterRequest.class), any(), any());

        // Act & Assert
        mockMvc.perform(multipart("/api/auth/register")
                .file(dataFile)
                .file(photoFile)
                .file(aadhaarFile))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully. Await admin approval."));

        verify(userService).register(any(RegisterRequest.class), any(), any());
    }
}
