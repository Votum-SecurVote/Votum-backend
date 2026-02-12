package com.votum.votum_backend.service;

import com.votum.votum_backend.dto.CreateBallotRequest;
import com.votum.votum_backend.dto.CreateCandidateRequest;
import com.votum.votum_backend.dto.CreateElectionRequest;
import com.votum.votum_backend.model.Ballot;
import com.votum.votum_backend.model.Candidate;
import com.votum.votum_backend.model.Election;
import com.votum.votum_backend.repository.BallotRepository;
import com.votum.votum_backend.repository.CandidateRepository;
import com.votum.votum_backend.repository.ElectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminElectionService Tests")
class AdminElectionServiceTest {

    @Mock
    private ElectionRepository electionRepository;
    
    @Mock
    private BallotRepository ballotRepository;
    
    @Mock
    private CandidateRepository candidateRepository;

    @InjectMocks
    private AdminElectionService adminElectionService;

    private CreateElectionRequest electionRequest;
    private CreateBallotRequest ballotRequest;
    private CreateCandidateRequest candidateRequest;
    private Election testElection;
    private Ballot testBallot;
    private Candidate testCandidate;

    @BeforeEach
    void setUp() {
        // Set up test data
        electionRequest = new CreateElectionRequest();
        electionRequest.setTitle("Presidential Election 2024");
        electionRequest.setDescription("Election for President of the United States");
        electionRequest.setStartDate(LocalDateTime.now().plusDays(1));
        electionRequest.setEndDate(LocalDateTime.now().plusDays(30));

        ballotRequest = new CreateBallotRequest();
        ballotRequest.setTitle("Presidential Ballot");
        ballotRequest.setDescription("Choose your preferred presidential candidate");
        ballotRequest.setMaxSelections(1);

        candidateRequest = new CreateCandidateRequest();
        candidateRequest.setName("John Smith");
        candidateRequest.setParty("Democratic Party");
        candidateRequest.setSymbol("🐴");

        testElection = Election.builder()
                .id(UUID.randomUUID())
                .title("Presidential Election 2024")
                .description("Election for President of the United States")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .status("DRAFT")
                .createdAt(LocalDateTime.now())
                .build();

        testBallot = Ballot.builder()
                .id(UUID.randomUUID())
                .election(testElection)
                .title("Presidential Ballot")
                .description("Choose your preferred presidential candidate")
                .maxSelections(1)
                .status("DRAFT")
                .createdAt(LocalDateTime.now())
                .build();

        testCandidate = Candidate.builder()
                .id(UUID.randomUUID())
                .ballot(testBallot)
                .name("John Smith")
                .party("Democratic Party")
                .symbol("🐴")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create election successfully with valid data")
    void createElection_Success() {
        // Arrange
        when(electionRepository.save(any(Election.class))).thenReturn(testElection);

        // Act
        Election createdElection = adminElectionService.createElection(electionRequest);

        // Assert
        assertThat(createdElection).isNotNull();
        assertThat(createdElection.getTitle()).isEqualTo(testElection.getTitle());
        assertThat(createdElection.getDescription()).isEqualTo(testElection.getDescription());
        assertThat(createdElection.getStartDate()).isEqualTo(testElection.getStartDate());
        assertThat(createdElection.getEndDate()).isEqualTo(testElection.getEndDate());
        assertThat(createdElection.getStatus()).isEqualTo("DRAFT");

        verify(electionRepository).save(any(Election.class));
    }

    @Test
    @DisplayName("Should create election with correct default status")
    void createElection_DefaultStatus() {
        // Arrange
        when(electionRepository.save(any(Election.class))).thenAnswer(invocation -> {
            Election savedElection = invocation.getArgument(0);
            assertThat(savedElection.getStatus()).isEqualTo("DRAFT");
            return testElection;
        });

        // Act
        Election createdElection = adminElectionService.createElection(electionRequest);

        // Assert
        verify(electionRepository).save(any(Election.class));
    }

    @Test
    @DisplayName("Should create ballot successfully for existing election")
    void createBallot_Success() {
        // Arrange
        UUID electionId = testElection.getId();
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(testElection));
        when(ballotRepository.save(any(Ballot.class))).thenReturn(testBallot);

        // Act
        Ballot createdBallot = adminElectionService.createBallot(electionId, ballotRequest);

        // Assert
        assertThat(createdBallot).isNotNull();
        assertThat(createdBallot.getTitle()).isEqualTo(testBallot.getTitle());
        assertThat(createdBallot.getDescription()).isEqualTo(testBallot.getDescription());
        assertThat(createdBallot.getMaxSelections()).isEqualTo(testBallot.getMaxSelections());
        assertThat(createdBallot.getStatus()).isEqualTo("DRAFT");
        assertThat(createdBallot.getElection()).isEqualTo(testElection);

        verify(electionRepository).findById(electionId);
        verify(ballotRepository).save(any(Ballot.class));
    }

    @Test
    @DisplayName("Should throw exception when creating ballot for non-existent election")
    void createBallot_ElectionNotFound_ThrowsException() {
        // Arrange
        UUID nonExistentElectionId = UUID.randomUUID();
        when(electionRepository.findById(nonExistentElectionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminElectionService.createBallot(nonExistentElectionId, ballotRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Election not found");

        verify(electionRepository).findById(nonExistentElectionId);
        verify(ballotRepository, never()).save(any(Ballot.class));
    }

    @Test
    @DisplayName("Should create candidate successfully for existing ballot")
    void createCandidate_Success() {
        // Arrange
        UUID ballotId = testBallot.getId();
        when(ballotRepository.findById(ballotId)).thenReturn(Optional.of(testBallot));
        when(candidateRepository.save(any(Candidate.class))).thenReturn(testCandidate);

        // Act
        Candidate createdCandidate = adminElectionService.createCandidate(ballotId, candidateRequest);

        // Assert
        assertThat(createdCandidate).isNotNull();
        assertThat(createdCandidate.getName()).isEqualTo(testCandidate.getName());
        assertThat(createdCandidate.getParty()).isEqualTo(testCandidate.getParty());
        assertThat(createdCandidate.getSymbol()).isEqualTo(testCandidate.getSymbol());
        assertThat(createdCandidate.getBallot()).isEqualTo(testBallot);

        verify(ballotRepository).findById(ballotId);
        verify(candidateRepository).save(any(Candidate.class));
    }

    @Test
    @DisplayName("Should throw exception when creating candidate for non-existent ballot")
    void createCandidate_BallotNotFound_ThrowsException() {
        // Arrange
        UUID nonExistentBallotId = UUID.randomUUID();
        when(ballotRepository.findById(nonExistentBallotId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminElectionService.createCandidate(nonExistentBallotId, candidateRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Ballot not found");

        verify(ballotRepository).findById(nonExistentBallotId);
        verify(candidateRepository, never()).save(any(Candidate.class));
    }

    @Test
    @DisplayName("Should handle null election request gracefully")
    void createElection_NullRequest_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> adminElectionService.createElection(null))
            .isInstanceOf(NullPointerException.class);

        verify(electionRepository, never()).save(any(Election.class));
    }

    @Test
    @DisplayName("Should handle null ballot request gracefully")
    void createBallot_NullRequest_ThrowsException() {
        // Arrange
        UUID electionId = testElection.getId();
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(testElection));

        // Act & Assert
        assertThatThrownBy(() -> adminElectionService.createBallot(electionId, null))
            .isInstanceOf(NullPointerException.class);

        verify(electionRepository).findById(electionId);
        verify(ballotRepository, never()).save(any(Ballot.class));
    }

    @Test
    @DisplayName("Should handle null candidate request gracefully")
    void createCandidate_NullRequest_ThrowsException() {
        // Arrange
        UUID ballotId = testBallot.getId();
        when(ballotRepository.findById(ballotId)).thenReturn(Optional.of(testBallot));

        // Act & Assert
        assertThatThrownBy(() -> adminElectionService.createCandidate(ballotId, null))
            .isInstanceOf(NullPointerException.class);

        verify(ballotRepository).findById(ballotId);
        verify(candidateRepository, never()).save(any(Candidate.class));
    }

    @Test
    @DisplayName("Should verify election properties are set correctly during creation")
    void createElection_VerifyProperties() {
        // Arrange
        when(electionRepository.save(any(Election.class))).thenAnswer(invocation -> {
            Election election = invocation.getArgument(0);
            
            // Verify all properties are set correctly
            assertThat(election.getTitle()).isEqualTo(electionRequest.getTitle());
            assertThat(election.getDescription()).isEqualTo(electionRequest.getDescription());
            assertThat(election.getStartDate()).isEqualTo(electionRequest.getStartDate());
            assertThat(election.getEndDate()).isEqualTo(electionRequest.getEndDate());
            assertThat(election.getStatus()).isEqualTo("DRAFT");
            assertThat(election.getCreatedAt()).isNotNull();
            
            return testElection;
        });

        // Act
        adminElectionService.createElection(electionRequest);

        // Assert
        verify(electionRepository).save(any(Election.class));
    }

    @Test
    @DisplayName("Should verify ballot properties are set correctly during creation")
    void createBallot_VerifyProperties() {
        // Arrange
        UUID electionId = testElection.getId();
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(testElection));
        when(ballotRepository.save(any(Ballot.class))).thenAnswer(invocation -> {
            Ballot ballot = invocation.getArgument(0);
            
            // Verify all properties are set correctly
            assertThat(ballot.getElection()).isEqualTo(testElection);
            assertThat(ballot.getTitle()).isEqualTo(ballotRequest.getTitle());
            assertThat(ballot.getDescription()).isEqualTo(ballotRequest.getDescription());
            assertThat(ballot.getMaxSelections()).isEqualTo(ballotRequest.getMaxSelections());
            assertThat(ballot.getStatus()).isEqualTo("DRAFT");
            assertThat(ballot.getCreatedAt()).isNotNull();
            
            return testBallot;
        });

        // Act
        adminElectionService.createBallot(electionId, ballotRequest);

        // Assert
        verify(electionRepository).findById(electionId);
        verify(ballotRepository).save(any(Ballot.class));
    }

    @Test
    @DisplayName("Should verify candidate properties are set correctly during creation")
    void createCandidate_VerifyProperties() {
        // Arrange
        UUID ballotId = testBallot.getId();
        when(ballotRepository.findById(ballotId)).thenReturn(Optional.of(testBallot));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> {
            Candidate candidate = invocation.getArgument(0);
            
            // Verify all properties are set correctly
            assertThat(candidate.getBallot()).isEqualTo(testBallot);
            assertThat(candidate.getName()).isEqualTo(candidateRequest.getName());
            assertThat(candidate.getParty()).isEqualTo(candidateRequest.getParty());
            assertThat(candidate.getSymbol()).isEqualTo(candidateRequest.getSymbol());
            assertThat(candidate.getCreatedAt()).isNotNull();
            
            return testCandidate;
        });

        // Act
        adminElectionService.createCandidate(ballotId, candidateRequest);

        // Assert
        verify(ballotRepository).findById(ballotId);
        verify(candidateRepository).save(any(Candidate.class));
    }
}
