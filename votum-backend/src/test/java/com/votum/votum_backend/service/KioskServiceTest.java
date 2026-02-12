package com.votum.votum_backend.service;

import com.votum.votum_backend.dto.KioskLoginRequest;
import com.votum.votum_backend.dto.VoteRequest;
import com.votum.votum_backend.model.*;
import com.votum.votum_backend.repository.*;
import com.votum.votum_backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KioskService Tests")
class KioskServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ElectionRepository electionRepository;
    
    @Mock
    private BallotRepository ballotRepository;
    
    @Mock
    private CandidateRepository candidateRepository;
    
    @Mock
    private VoteRepository voteRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private KioskService kioskService;

    private KioskLoginRequest kioskLoginRequest;
    private User testUser;
    private Election testElection;
    private Ballot testBallot;
    private Candidate testCandidate;
    private VoteRequest voteRequest;

    @BeforeEach
    void setUp() {
        // Setup test data
        kioskLoginRequest = new KioskLoginRequest();
        kioskLoginRequest.setEmail("john.doe@example.com");
        kioskLoginRequest.setPassword("password123");
        kioskLoginRequest.setAadhaar("123456789012");

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setFullName("John Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setPhone("1234567890");
        testUser.setPasswordHash("hashedPassword");
        testUser.setAadhaarHash("KjM0nn5gaorS4w48hFIfk3dFDPCQg+Fi4KmxSAzg+XI="); // SHA-256 hash of "123456789012"
        testUser.setDob(LocalDate.of(1990, 1, 1));
        testUser.setGender("Male");
        testUser.setAddress("123 Main St");
        testUser.setStatus("APPROVED");
        testUser.setRole("USER");
        testUser.setCreatedAt(LocalDateTime.now());

        testElection = new Election();
        testElection.setId(UUID.randomUUID());
        testElection.setTitle("Presidential Election 2024");
        testElection.setStatus("ACTIVE");
        testElection.setStartDate(LocalDateTime.now().minusDays(1));
        testElection.setEndDate(LocalDateTime.now().plusDays(30));

        testBallot = new Ballot();
        testBallot.setId(UUID.randomUUID());
        testBallot.setElection(testElection);
        testBallot.setTitle("President");

        testCandidate = new Candidate();
        testCandidate.setId(UUID.randomUUID());
        testCandidate.setBallot(testBallot);
        testCandidate.setName("John Smith");
        testCandidate.setParty("Democratic Party");

        voteRequest = new VoteRequest();
        voteRequest.setElectionId(testElection.getId());
        voteRequest.setBallotId(testBallot.getId());
        voteRequest.setCandidateId(testCandidate.getId());
    }

    @Test
    @DisplayName("Should login successfully with valid kiosk credentials")
    void login_Success() {
        // Arrange
        String expectedToken = "kiosk-jwt-token";
        
        when(userRepository.findByEmail(kioskLoginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(kioskLoginRequest.getPassword(), testUser.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(testUser.getEmail(), testUser.getRole())).thenReturn(expectedToken);

        // Act
        String actualToken = kioskService.login(kioskLoginRequest);

        // Assert
        assertThat(actualToken).isEqualTo(expectedToken);
        verify(userRepository).findByEmail(kioskLoginRequest.getEmail());
        verify(passwordEncoder).matches(kioskLoginRequest.getPassword(), testUser.getPasswordHash());
        verify(jwtUtil).generateToken(testUser.getEmail(), testUser.getRole());
    }

    @Test
    @DisplayName("Should throw exception when user not found during kiosk login")
    void login_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(kioskLoginRequest.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> kioskService.login(kioskLoginRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("User not found");

        verify(userRepository).findByEmail(kioskLoginRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when password is invalid during kiosk login")
    void login_InvalidPassword_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(kioskLoginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(kioskLoginRequest.getPassword(), testUser.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> kioskService.login(kioskLoginRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Invalid password");

        verify(userRepository).findByEmail(kioskLoginRequest.getEmail());
        verify(passwordEncoder).matches(kioskLoginRequest.getPassword(), testUser.getPasswordHash());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when Aadhaar is invalid during kiosk login")
    void login_InvalidAadhaar_ThrowsException() {
        // Arrange
        kioskLoginRequest.setAadhaar("999999999999"); // Different Aadhaar
        
        when(userRepository.findByEmail(kioskLoginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(kioskLoginRequest.getPassword(), testUser.getPasswordHash())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> kioskService.login(kioskLoginRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Invalid Aadhaar number");

        verify(userRepository).findByEmail(kioskLoginRequest.getEmail());
        verify(passwordEncoder).matches(kioskLoginRequest.getPassword(), testUser.getPasswordHash());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when user is not approved during kiosk login")
    void login_UserNotApproved_ThrowsException() {
        // Arrange
        testUser.setStatus("PENDING");
        
        when(userRepository.findByEmail(kioskLoginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(kioskLoginRequest.getPassword(), testUser.getPasswordHash())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> kioskService.login(kioskLoginRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("User not approved");

        verify(userRepository).findByEmail(kioskLoginRequest.getEmail());
        verify(passwordEncoder).matches(kioskLoginRequest.getPassword(), testUser.getPasswordHash());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Should get active election successfully")
    void getActiveElection_Success() {
        // Arrange
        List<Election> activeElections = Arrays.asList(testElection);
        when(electionRepository.findByStatus("ACTIVE")).thenReturn(activeElections);

        // Act
        Election actualElection = kioskService.getActiveElection();

        // Assert
        assertThat(actualElection).isEqualTo(testElection);
        verify(electionRepository).findByStatus("ACTIVE");
    }

    @Test
    @DisplayName("Should throw exception when no active election found")
    void getActiveElection_NoActiveElection_ThrowsException() {
        // Arrange
        when(electionRepository.findByStatus("ACTIVE")).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> kioskService.getActiveElection())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("No active election found");

        verify(electionRepository).findByStatus("ACTIVE");
    }

    @Test
    @DisplayName("Should get ballots for election successfully")
    void getBallots_Success() {
        // Arrange
        List<Ballot> expectedBallots = Arrays.asList(testBallot);
        when(ballotRepository.findByElection_Id(testElection.getId())).thenReturn(expectedBallots);

        // Act
        List<Ballot> actualBallots = kioskService.getBallots(testElection.getId());

        // Assert
        assertThat(actualBallots).isEqualTo(expectedBallots);
        assertThat(actualBallots).hasSize(1);
        assertThat(actualBallots.get(0)).isEqualTo(testBallot);
        verify(ballotRepository).findByElection_Id(testElection.getId());
    }

    @Test
    @DisplayName("Should get candidates for ballot successfully")
    void getCandidates_Success() {
        // Arrange
        List<Candidate> expectedCandidates = Arrays.asList(testCandidate);
        when(candidateRepository.findByBallot_Id(testBallot.getId())).thenReturn(expectedCandidates);

        // Act
        List<Candidate> actualCandidates = kioskService.getCandidates(testBallot.getId());

        // Assert
        assertThat(actualCandidates).isEqualTo(expectedCandidates);
        assertThat(actualCandidates).hasSize(1);
        assertThat(actualCandidates.get(0)).isEqualTo(testCandidate);
        verify(candidateRepository).findByBallot_Id(testBallot.getId());
    }

    @Test
    @DisplayName("Should cast vote successfully")
    void castVote_Success() {
        // Arrange
        String email = "john.doe@example.com";
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(voteRepository.existsByUserIdAndElectionId(testUser.getId(), voteRequest.getElectionId())).thenReturn(false);
        when(voteRepository.save(any(Vote.class))).thenReturn(new Vote());

        // Act
        assertThatNoException().isThrownBy(() -> kioskService.castVote(email, voteRequest));

        // Assert
        verify(userRepository).findByEmail(email);
        verify(voteRepository).existsByUserIdAndElectionId(testUser.getId(), voteRequest.getElectionId());
        verify(voteRepository).save(any(Vote.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found during vote casting")
    void castVote_UserNotFound_ThrowsException() {
        // Arrange
        String email = "nonexistent@example.com";
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> kioskService.castVote(email, voteRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("User not found");

        verify(userRepository).findByEmail(email);
        verify(voteRepository, never()).existsByUserIdAndElectionId(any(), any());
        verify(voteRepository, never()).save(any(Vote.class));
    }

    @Test
    @DisplayName("Should throw exception when user already voted")
    void castVote_UserAlreadyVoted_ThrowsException() {
        // Arrange
        String email = "john.doe@example.com";
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(voteRepository.existsByUserIdAndElectionId(testUser.getId(), voteRequest.getElectionId())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> kioskService.castVote(email, voteRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("User already voted in this election");

        verify(userRepository).findByEmail(email);
        verify(voteRepository).existsByUserIdAndElectionId(testUser.getId(), voteRequest.getElectionId());
        verify(voteRepository, never()).save(any(Vote.class));
    }

    @Test
    @DisplayName("Should return empty list when no ballots found for election")
    void getBallots_NoBallots_ReturnsEmptyList() {
        // Arrange
        UUID electionId = UUID.randomUUID();
        when(ballotRepository.findByElection_Id(electionId)).thenReturn(Collections.emptyList());

        // Act
        List<Ballot> actualBallots = kioskService.getBallots(electionId);

        // Assert
        assertThat(actualBallots).isEmpty();
        verify(ballotRepository).findByElection_Id(electionId);
    }

    @Test
    @DisplayName("Should return empty list when no candidates found for ballot")
    void getCandidates_NoCandidates_ReturnsEmptyList() {
        // Arrange
        UUID ballotId = UUID.randomUUID();
        when(candidateRepository.findByBallot_Id(ballotId)).thenReturn(Collections.emptyList());

        // Act
        List<Candidate> actualCandidates = kioskService.getCandidates(ballotId);

        // Assert
        assertThat(actualCandidates).isEmpty();
        verify(candidateRepository).findByBallot_Id(ballotId);
    }

    @Test
    @DisplayName("Should get first election when multiple active elections exist")
    void getActiveElection_MultipleActiveElections_ReturnsFirst() {
        // Arrange
        Election secondElection = new Election();
        secondElection.setId(UUID.randomUUID());
        secondElection.setTitle("Senate Election 2024");
        secondElection.setStatus("ACTIVE");

        List<Election> activeElections = Arrays.asList(testElection, secondElection);
        when(electionRepository.findByStatus("ACTIVE")).thenReturn(activeElections);

        // Act
        Election actualElection = kioskService.getActiveElection();

        // Assert
        assertThat(actualElection).isEqualTo(testElection);
        verify(electionRepository).findByStatus("ACTIVE");
    }
}
