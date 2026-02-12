package com.votum.votum_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String testSecret = "testSecretKeyThatIsLongEnoughForHMACSHA256AlgorithmRequirementsAndSecure";
    private final long testExpiration = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(testSecret, testExpiration);
    }

    @Test
    @DisplayName("Should generate valid JWT token with email and role")
    void generateToken_Success() {
        // Arrange
        String email = "test@example.com";
        String role = "USER";

        // Act
        String token = jwtUtil.generateToken(email, role);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    @DisplayName("Should extract email from valid token")
    void extractEmail_Success() {
        // Arrange
        String expectedEmail = "test@example.com";
        String role = "USER";
        String token = jwtUtil.generateToken(expectedEmail, role);

        // Act
        String actualEmail = jwtUtil.extractEmail(token);

        // Assert
        assertThat(actualEmail).isEqualTo(expectedEmail);
    }

    @Test
    @DisplayName("Should extract role from valid token")
    void extractRole_Success() {
        // Arrange
        String email = "test@example.com";
        String expectedRole = "ADMIN";
        String token = jwtUtil.generateToken(email, expectedRole);

        // Act
        String actualRole = jwtUtil.extractRole(token);

        // Assert
        assertThat(actualRole).isEqualTo(expectedRole);
    }

    @Test
    @DisplayName("Should validate valid token")
    void validateToken_ValidToken_ReturnsTrue() {
        // Arrange
        String email = "test@example.com";
        String role = "USER";
        String token = jwtUtil.generateToken(email, role);

        // Act
        boolean isValid = jwtUtil.validateToken(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should invalidate malformed token")
    void validateToken_MalformedToken_ReturnsFalse() {
        // Arrange
        String malformedToken = "invalid.token.format";

        // Act
        boolean isValid = jwtUtil.validateToken(malformedToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate null token")
    void validateToken_NullToken_ReturnsFalse() {
        // Arrange
        String nullToken = null;

        // Act
        boolean isValid = jwtUtil.validateToken(nullToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate empty token")
    void validateToken_EmptyToken_ReturnsFalse() {
        // Arrange
        String emptyToken = "";

        // Act
        boolean isValid = jwtUtil.validateToken(emptyToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate token with wrong signature")
    void validateToken_WrongSignature_ReturnsFalse() {
        // Arrange
        String email = "test@example.com";
        String role = "USER";
        String token = jwtUtil.generateToken(email, role);
        
        // Tamper with the token by changing the last character
        String tamperedToken = token.substring(0, token.length() - 1) + "x";

        // Act
        boolean isValid = jwtUtil.validateToken(tamperedToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should generate different tokens for different inputs")
    void generateToken_DifferentInputs_DifferentTokens() {
        // Arrange
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";
        String role = "USER";

        // Act
        String token1 = jwtUtil.generateToken(email1, role);
        String token2 = jwtUtil.generateToken(email2, role);

        // Assert
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Should generate different tokens for different roles")
    void generateToken_DifferentRoles_DifferentTokens() {
        // Arrange
        String email = "test@example.com";
        String role1 = "USER";
        String role2 = "ADMIN";

        // Act
        String token1 = jwtUtil.generateToken(email, role1);
        String token2 = jwtUtil.generateToken(email, role2);

        // Assert
        assertThat(token1).isNotEqualTo(token2);
        
        // Verify roles are different
        String extractedRole1 = jwtUtil.extractRole(token1);
        String extractedRole2 = jwtUtil.extractRole(token2);
        
        assertThat(extractedRole1).isEqualTo(role1);
        assertThat(extractedRole2).isEqualTo(role2);
        assertThat(extractedRole1).isNotEqualTo(extractedRole2);
    }

    @Test
    @DisplayName("Should extract custom claim using generic method")
    void extractClaim_CustomClaim_Success() {
        // Arrange
        String email = "test@example.com";
        String role = "USER";
        String token = jwtUtil.generateToken(email, role);

        // Act
        Date issuedAt = jwtUtil.extractClaim(token, Claims::getIssuedAt);
        Date expiration = jwtUtil.extractClaim(token, Claims::getExpiration);

        // Assert
        assertThat(issuedAt).isNotNull();
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(issuedAt);
    }

    @Test
    @DisplayName("Should throw exception when extracting email from invalid token")
    void extractEmail_InvalidToken_ThrowsException() {
        // Arrange
        String invalidToken = "invalid.token";

        // Act & Assert
        assertThatThrownBy(() -> jwtUtil.extractEmail(invalidToken))
            .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Should throw exception when extracting role from invalid token")
    void extractRole_InvalidToken_ThrowsException() {
        // Arrange
        String invalidToken = "invalid.token";

        // Act & Assert
        assertThatThrownBy(() -> jwtUtil.extractRole(invalidToken))
            .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Should handle expired token validation")
    void validateToken_ExpiredToken_ReturnsFalse() {
        // Note: This test uses a very short expiration time to simulate expiry
        // In practice, you might need to mock the clock or use a different approach
        
        // Arrange
        JwtUtil shortExpirationJwtUtil = new JwtUtil(testSecret, 1L); // 1ms expiration
        String email = "test@example.com";
        String role = "USER";
        String token = shortExpirationJwtUtil.generateToken(email, role);

        // Wait for token to expire
        try {
            Thread.sleep(10); // Wait 10ms to ensure token expires
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act
        boolean isValid = shortExpirationJwtUtil.validateToken(token);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should maintain consistency between generation and extraction")
    void tokenConsistency_GenerateAndExtract_Success() {
        // Arrange
        String expectedEmail = "consistency@test.com";
        String expectedRole = "ADMIN";

        // Act
        String token = jwtUtil.generateToken(expectedEmail, expectedRole);
        String actualEmail = jwtUtil.extractEmail(token);
        String actualRole = jwtUtil.extractRole(token);
        boolean isValid = jwtUtil.validateToken(token);

        // Assert
        assertThat(actualEmail).isEqualTo(expectedEmail);
        assertThat(actualRole).isEqualTo(expectedRole);
        assertThat(isValid).isTrue();
    }
}
