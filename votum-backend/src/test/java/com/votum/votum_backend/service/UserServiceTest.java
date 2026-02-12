package com.votum.votum_backend.service;

import com.votum.votum_backend.dto.RegisterRequest;
import com.votum.votum_backend.dto.UserProfileResponse;
import com.votum.votum_backend.model.User;
import com.votum.votum_backend.model.UserBiometrics;
import com.votum.votum_backend.repository.UserBiometricsRepository;
import com.votum.votum_backend.repository.UserRepository;
import com.votum.votum_backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserBiometricsRepository biometricsRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtUtil jwtUtil;
    
    @Mock
    private MultipartFile photoFile;
    
    @Mock
    private MultipartFile aadhaarFile;

    @InjectMocks
    private UserService userService;

    @TempDir
    java.nio.file.Path tempDir;

    private RegisterRequest registerRequest;
    private User testUser;
    private UserBiometrics testBiometrics;

    @BeforeEach
    void setUp() {
        // Set up test data
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("John Doe");
        registerRequest.setEmail("john.doe@example.com");
        registerRequest.setPhone("1234567890");
        registerRequest.setPassword("password123");
        registerRequest.setAadhaar("123456789012");
        registerRequest.setDob(LocalDate.of(1990, 1, 1));
        registerRequest.setGender("Male");
        registerRequest.setAddress("123 Main St");

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setFullName("John Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setPhone("1234567890");
        testUser.setPasswordHash("hashedPassword");
        testUser.setAadhaarHash("hashedAadhaar");
        testUser.setDob(LocalDate.of(1990, 1, 1));
        testUser.setGender("Male");
        testUser.setAddress("123 Main St");
        testUser.setStatus("APPROVED");
        testUser.setRole("USER");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        testBiometrics = new UserBiometrics();
        testBiometrics.setUser(testUser);
        testBiometrics.setPhotoPath("/path/to/photo.jpg");
        testBiometrics.setAadhaarPdfPath("/path/to/aadhaar.pdf");

        // Set up storage path for file operations
        ReflectionTestUtils.setField(userService, "storagePath", tempDir.toString());
    }

    @Test
    @DisplayName("Should register user successfully with valid data")
    void registerUser_Success() throws IOException {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(biometricsRepository.save(any(UserBiometrics.class))).thenReturn(testBiometrics);
        
        when(photoFile.getOriginalFilename()).thenReturn("photo.jpg");
        when(photoFile.getInputStream()).thenReturn(new ByteArrayInputStream("photo-data".getBytes()));
        when(aadhaarFile.getOriginalFilename()).thenReturn("aadhaar.pdf");
        when(aadhaarFile.getInputStream()).thenReturn(new ByteArrayInputStream("aadhaar-data".getBytes()));

        // Act
        assertThatNoException().isThrownBy(() -> 
            userService.register(registerRequest, photoFile, aadhaarFile)
        );

        // Assert
        verify(userRepository).existsByEmail("john.doe@example.com");
        verify(userRepository).existsByPhone("1234567890");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(biometricsRepository).save(any(UserBiometrics.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void registerUser_EmailExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> 
            userService.register(registerRequest, photoFile, aadhaarFile)
        )
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Email already exists");

        verify(userRepository).existsByEmail("john.doe@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when phone already exists")
    void registerUser_PhoneExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone("1234567890")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> 
            userService.register(registerRequest, photoFile, aadhaarFile)
        )
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Phone already exists");

        verify(userRepository).existsByPhone("1234567890");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void login_Success() {
        // Arrange
        String email = "john.doe@example.com";
        String password = "password123";
        String expectedToken = "jwt-token";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(email, testUser.getRole())).thenReturn(expectedToken);

        // Act
        String actualToken = userService.login(email, password);

        // Assert
        assertThat(actualToken).isEqualTo(expectedToken);
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(password, testUser.getPasswordHash());
        verify(jwtUtil).generateToken(email, testUser.getRole());
    }

    @Test
    @DisplayName("Should throw exception when user not found during login")
    void login_UserNotFound_ThrowsException() {
        // Arrange
        String email = "nonexistent@example.com";
        String password = "password123";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.login(email, password))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("User not found");

        verify(userRepository).findByEmail(email);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when password is invalid during login")
    void login_InvalidPassword_ThrowsException() {
        // Arrange
        String email = "john.doe@example.com";
        String wrongPassword = "wrongPassword";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(wrongPassword, testUser.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.login(email, wrongPassword))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Invalid password");

        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(wrongPassword, testUser.getPasswordHash());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Should get user profile successfully")
    void getProfile_Success() {
        // Arrange
        String email = "john.doe@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(biometricsRepository.findById(testUser.getId())).thenReturn(Optional.of(testBiometrics));

        // Act
        UserProfileResponse profile = userService.getProfile(email);

        // Assert
        assertThat(profile).isNotNull();
        assertThat(profile.getFullName()).isEqualTo(testUser.getFullName());
        assertThat(profile.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(profile.getPhone()).isEqualTo(testUser.getPhone());
        assertThat(profile.getDob()).isEqualTo(testUser.getDob());
        assertThat(profile.getGender()).isEqualTo(testUser.getGender());
        assertThat(profile.getAddress()).isEqualTo(testUser.getAddress());
        assertThat(profile.getStatus()).isEqualTo(testUser.getStatus());
        assertThat(profile.getPhotoPath()).isEqualTo(testBiometrics.getPhotoPath());

        verify(userRepository).findByEmail(email);
        verify(biometricsRepository).findById(testUser.getId());
    }

    @Test
    @DisplayName("Should get user profile with null photo path when biometrics not found")
    void getProfile_NoBiometrics_Success() {
        // Arrange
        String email = "john.doe@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(biometricsRepository.findById(testUser.getId())).thenReturn(Optional.empty());

        // Act
        UserProfileResponse profile = userService.getProfile(email);

        // Assert
        assertThat(profile).isNotNull();
        assertThat(profile.getFullName()).isEqualTo(testUser.getFullName());
        assertThat(profile.getPhotoPath()).isNull();

        verify(userRepository).findByEmail(email);
        verify(biometricsRepository).findById(testUser.getId());
    }

    @Test
    @DisplayName("Should throw exception when user not found during profile retrieval")
    void getProfile_UserNotFound_ThrowsException() {
        // Arrange
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getProfile(email))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("User not found");

        verify(userRepository).findByEmail(email);
        verify(biometricsRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should handle IOException during file saving")
    void registerUser_FileIOException_ThrowsException() throws IOException {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        when(photoFile.getOriginalFilename()).thenReturn("photo.jpg");
        when(photoFile.getInputStream()).thenThrow(new IOException("File read error"));

        // Act & Assert
        assertThatThrownBy(() -> 
            userService.register(registerRequest, photoFile, aadhaarFile)
        )
        .isInstanceOf(IOException.class)
        .hasMessage("File read error");
    }
}
