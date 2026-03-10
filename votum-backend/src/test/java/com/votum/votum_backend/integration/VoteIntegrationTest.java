package com.votum.votum_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.votum.votum_backend.dto.KioskLoginRequest;
import com.votum.votum_backend.dto.VoteRequest;
import com.votum.votum_backend.model.*;
import com.votum.votum_backend.repository.*;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Voting Flow
 * Tests vote casting, double voting prevention, and vote persistence
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Vote Integration Tests")
class VoteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private BallotRepository ballotRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Election activeElection;
    private Ballot ballot;
    private Candidate candidate1;
    private Candidate candidate2;

    @BeforeEach
    void setUp() {
        // Clean database
        voteRepository.deleteAll();
        candidateRepository.deleteAll();
        ballotRepository.deleteAll();
        electionRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setFullName("Voter John");
        testUser.setEmail("voter@example.com");
        testUser.setPhone("9876543210");
        testUser.setPasswordHash(passwordEncoder.encode("voterpass"));
        testUser.setAadhaarHash("KjM0nn5gaorS4w48hFIfk3dFDPCQg+Fi4KmxSAzg+XI="); // Hash of "123456789012"
        testUser.setRole("USER");
        testUser.setDob(LocalDate.of(1990, 1, 1));
        testUser.setGender("Male");
        testUser.setAddress("123 Voter Street");
        testUser.setStatus("APPROVED");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        // Create active election
        activeElection = Election.builder()
                .title("Presidential Election 2024")
                .description("National Presidential Election")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .status("PUBLISHED")
                .createdAt(LocalDateTime.now())
                .build();
        activeElection = electionRepository.save(activeElection);

        // Create ballot
        ballot = new Ballot();
        ballot.setElection(activeElection);
        ballot.setTitle("President");
        ballot.setDescription("Choose your president");
        ballot = ballotRepository.save(ballot);

        // Create candidates
        candidate1 = new Candidate();
        candidate1.setBallot(ballot);
        candidate1.setName("John Smith");
        candidate1.setParty("Democratic Party");
        candidate1 = candidateRepository.save(candidate1);

        candidate2 = new Candidate();
        candidate2.setBallot(ballot);
        candidate2.setName("Jane Doe");
        candidate2.setParty("Republican Party");
        candidate2 = candidateRepository.save(candidate2);
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
    @DisplayName("Should successfully cast a vote and store in database")
    void testCastVote_Success() throws Exception {
        // Arrange - Login at kiosk
        KioskLoginRequest kioskLogin = new KioskLoginRequest();
        kioskLogin.setEmail("voter@example.com");
        kioskLogin.setPassword("voterpass");
        kioskLogin.setAadhaar("123456789012");

        String token = mockMvc.perform(post("/api/kiosk/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kioskLogin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Prepare vote request
        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setElectionId(activeElection.getId());
        voteRequest.setBallotId(ballot.getId());
        voteRequest.setCandidateId(candidate1.getId());

        // Act - Cast vote
        mockMvc.perform(post("/api/kiosk/vote")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Vote cast successfully"));

        // Assert - Verify vote is stored in database
        List<Vote> votes = voteRepository.findAll();
        assertThat(votes).hasSize(1);

        Vote savedVote = votes.get(0);
        assertThat(savedVote.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savedVote.getElectionId()).isEqualTo(activeElection.getId());
        assertThat(savedVote.getBallotId()).isEqualTo(ballot.getId());
        assertThat(savedVote.getCandidateId()).isEqualTo(candidate1.getId());
        assertThat(savedVote.getVotedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should prevent double voting in same election")
    void testCastVote_PreventDoubleVoting() throws Exception {
        // Arrange - Login at kiosk
        KioskLoginRequest kioskLogin = new KioskLoginRequest();
        kioskLogin.setEmail("voter@example.com");
        kioskLogin.setPassword("voterpass");
        kioskLogin.setAadhaar("123456789012");

        String token = mockMvc.perform(post("/api/kiosk/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kioskLogin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setElectionId(activeElection.getId());
        voteRequest.setBallotId(ballot.getId());
        voteRequest.setCandidateId(candidate1.getId());

        // Act - Cast first vote (should succeed)
        mockMvc.perform(post("/api/kiosk/vote")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isOk());

        // Act - Try to cast second vote (should fail)
        voteRequest.setCandidateId(candidate2.getId()); // Try to vote for different candidate

        mockMvc.perform(post("/api/kiosk/vote")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().is4xxClientError());

        // Assert - Verify only one vote exists
        List<Vote> votes = voteRepository.findAll();
        assertThat(votes).hasSize(1);
    }

    @Test
    @DisplayName("Should retrieve active election")
    void testGetActiveElection_Success() throws Exception {
        // Arrange - Login at kiosk
        KioskLoginRequest kioskLogin = new KioskLoginRequest();
        kioskLogin.setEmail("voter@example.com");
        kioskLogin.setPassword("voterpass");
        kioskLogin.setAadhaar("123456789012");

        String token = mockMvc.perform(post("/api/kiosk/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kioskLogin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Act & Assert
        mockMvc.perform(get("/api/kiosk/elections/active")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activeElection.getId().toString()))
                .andExpect(jsonPath("$.title").value("Presidential Election 2024"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("Should retrieve ballots for election")
    void testGetBallots_Success() throws Exception {
        // Arrange - Login at kiosk
        KioskLoginRequest kioskLogin = new KioskLoginRequest();
        kioskLogin.setEmail("voter@example.com");
        kioskLogin.setPassword("voterpass");
        kioskLogin.setAadhaar("123456789012");

        String token = mockMvc.perform(post("/api/kiosk/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kioskLogin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Act & Assert
        mockMvc.perform(get("/api/kiosk/elections/" + activeElection.getId() + "/ballots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(ballot.getId().toString()))
                .andExpect(jsonPath("$[0].title").value("President"));
    }

    @Test
    @DisplayName("Should retrieve candidates for ballot")
    void testGetCandidates_Success() throws Exception {
        // Arrange - Login at kiosk
        KioskLoginRequest kioskLogin = new KioskLoginRequest();
        kioskLogin.setEmail("voter@example.com");
        kioskLogin.setPassword("voterpass");
        kioskLogin.setAadhaar("123456789012");

        String token = mockMvc.perform(post("/api/kiosk/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kioskLogin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Act & Assert
        mockMvc.perform(get("/api/kiosk/ballots/" + ballot.getId() + "/candidates")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("John Smith"))
                .andExpect(jsonPath("$[1].name").value("Jane Doe"));
    }

    @Test
    @DisplayName("Should fail to vote without authentication")
    void testCastVote_NoAuthentication() throws Exception {
        // Arrange
        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setElectionId(activeElection.getId());
        voteRequest.setBallotId(ballot.getId());
        voteRequest.setCandidateId(candidate1.getId());

        // Act & Assert
        mockMvc.perform(post("/api/kiosk/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should verify vote persistence across transactions")
    void testVotePersistence() throws Exception {
        // Arrange - Login
        KioskLoginRequest kioskLogin = new KioskLoginRequest();
        kioskLogin.setEmail("voter@example.com");
        kioskLogin.setPassword("voterpass");
        kioskLogin.setAadhaar("123456789012");

        String token = mockMvc.perform(post("/api/kiosk/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kioskLogin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setElectionId(activeElection.getId());
        voteRequest.setBallotId(ballot.getId());
        voteRequest.setCandidateId(candidate1.getId());

        // Act - Cast vote
        mockMvc.perform(post("/api/kiosk/vote")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isOk());

        // Assert - Query database directly
        List<Vote> allVotes = voteRepository.findAll();
        assertThat(allVotes).hasSize(1);

        Vote vote = allVotes.get(0);
        assertThat(vote.getId()).isNotNull();
        assertThat(vote.getUser()).isNotNull();
        assertThat(vote.getUser().getEmail()).isEqualTo("voter@example.com");
        assertThat(vote.getElectionId()).isEqualTo(activeElection.getId());
        assertThat(vote.getCandidateId()).isEqualTo(candidate1.getId());
    }
}
