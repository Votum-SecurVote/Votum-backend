package com.votum.votum_backend.service;

import com.votum.votum_backend.model.Admin;
import com.votum.votum_backend.repository.AdminRepository;
import com.votum.votum_backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Tests")
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AdminService adminService;

    private Admin testAdmin;

    @BeforeEach
    void setUp() {
        testAdmin = new Admin();
        testAdmin.setId(UUID.randomUUID());
        testAdmin.setEmail("admin@votum.com");
        testAdmin.setPasswordHash("hashedPassword");
        testAdmin.setFullName("Admin User");
    }

    @Test
    @DisplayName("Should login successfully with valid admin credentials")
    void login_Success() {
        // Arrange
        String email = "admin@votum.com";
        String password = "adminPassword";
        String expectedToken = "admin-jwt-token";

        when(adminRepository.findByEmail(email)).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches(password, testAdmin.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(email, "ADMIN")).thenReturn(expectedToken);

        // Act
        String actualToken = adminService.login(email, password);

        // Assert
        assertThat(actualToken).isEqualTo(expectedToken);
        verify(adminRepository).findByEmail(email);
        verify(passwordEncoder).matches(password, testAdmin.getPasswordHash());
        verify(jwtUtil).generateToken(email, "ADMIN");
    }

    @Test
    @DisplayName("Should throw exception when admin not found during login")
    void login_AdminNotFound_ThrowsException() {
        // Arrange
        String email = "nonexistent@votum.com";
        String password = "adminPassword";

        when(adminRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminService.login(email, password))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Admin not found");

        verify(adminRepository).findByEmail(email);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when admin password is invalid during login")
    void login_InvalidPassword_ThrowsException() {
        // Arrange
        String email = "admin@votum.com";
        String wrongPassword = "wrongPassword";

        when(adminRepository.findByEmail(email)).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches(wrongPassword, testAdmin.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> adminService.login(email, wrongPassword))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Invalid password");

        verify(adminRepository).findByEmail(email);
        verify(passwordEncoder).matches(wrongPassword, testAdmin.getPasswordHash());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle null admin email gracefully")
    void login_NullEmail_ThrowsException() {
        // Arrange
        String nullEmail = null;
        String password = "adminPassword";

        when(adminRepository.findByEmail(nullEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminService.login(nullEmail, password))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Admin not found");

        verify(adminRepository).findByEmail(nullEmail);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle empty admin email gracefully")
    void login_EmptyEmail_ThrowsException() {
        // Arrange
        String emptyEmail = "";
        String password = "adminPassword";

        when(adminRepository.findByEmail(emptyEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminService.login(emptyEmail, password))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Admin not found");

        verify(adminRepository).findByEmail(emptyEmail);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle null password gracefully")
    void login_NullPassword_ThrowsException() {
        // Arrange
        String email = "admin@votum.com";
        String nullPassword = null;

        when(adminRepository.findByEmail(email)).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches(nullPassword, testAdmin.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> adminService.login(email, nullPassword))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Invalid password");

        verify(adminRepository).findByEmail(email);
        verify(passwordEncoder).matches(nullPassword, testAdmin.getPasswordHash());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }
}
