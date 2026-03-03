package com.votum.votum_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.votum.votum_backend.dto.*;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Admin Flow
 * Tests election creation, ballot management, candidate addition, and results
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Admin Integration Tests")
class AdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminRepository adminRepository;

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

    private Admin testAdmin;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clean database
        voteRepository.deleteAll();
        candidateRepository.deleteAll();
        ballotRepository.deleteAll();
        electionRepository.deleteAll();
        userRepository.deleteAll();
        adminRepository.deleteAll();

        // Create test admin
        testAdmin = new Admin();
        testAdmin.setFullName("Admin User");
        testAdmin.setEmail("admin@votum.com");
        testAdmin.setPasswordHash(passwordEncoder.encode("admin123"));
        testAdmin.setCreatedAt(LocalDateTime.now());
        testAdmin = adminRepository.save(testAdmin);

        // Get admin token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@votum.com");
        loginRequest.setPassword("admin123");

        adminToken = mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
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
    @DisplayName("Should successfully create an election")
    void testCreateElection_Success() throws Exception {
        // Arrange
        CreateElectionRequest request = new CreateElectionRequest();
        request.setTitle("General Election 2024");
        request.setDescription("National General Election");
        request.setStartDate(LocalDateTime.now().plusDays(1));
        request.setEndDate(LocalDateTime.now().plusDays(30));

        // Act & Assert
        mockMvc.perform(post("/api/admin/elections")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("General Election 2024"))
                .andExpect(jsonPath("$.description").value("National General Election"));

        // Verify in database
        List<Election> elections = electionRepository.findAll();
        assertThat(elections).hasSize(1);
        assertThat(elections.get(0).getTitle()).isEqualTo("General Election 2024");
    }

    @Test
    @DisplayName("Should successfully add ballot to election")
    void testCreateBallot_Success() throws Exception {
        // Arrange - Create election first
        Election election = Election.builder()
                .title("Test Election")
                .description("Test Description")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .status("DRAFT")
                .createdAt(LocalDateTime.now())
                .build();
        election = electionRepository.save(election);

        CreateBallotRequest request = new CreateBallotRequest();
        request.setTitle("Governor");
        request.setDescription("Choose your state governor");

        // Act & Assert
        mockMvc.perform(post("/api/admin/elections/" + election.getId() + "/ballots")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Governor"))
                .andExpect(jsonPath("$.description").value("Choose your state governor"));

        // Verify in database
        List<Ballot> ballots = ballotRepository.findAll();
        assertThat(ballots).hasSize(1);
        assertThat(ballots.get(0).getTitle()).isEqualTo("Governor");
        assertThat(ballots.get(0).getElection().getId()).isEqualTo(election.getId());
    }

    @Test
    @DisplayName("Should successfully add candidate to ballot")
    void testCreateCandidate_Success() throws Exception {
        // Arrange - Create election and ballot
        Election election = Election.builder()
                .title("Test Election")
                .description("Test Description")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .status("DRAFT")
                .createdAt(LocalDateTime.now())
                .build();
        election = electionRepository.save(election);

        Ballot ballot = new Ballot();
        ballot.setElection(election);
        ballot.setTitle("Mayor");
        ballot.setDescription("City Mayor Election");
        ballot = ballotRepository.save(ballot);

        CreateCandidateRequest request = new CreateCandidateRequest();
        request.setName("Michael Brown");
        request.setParty("Independent");
        request.setSymbol("🏛️");

        // Act & Assert
        mockMvc.perform(post("/api/admin/ballots/" + ballot.getId() + "/candidates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Michael Brown"))
                .andExpect(jsonPath("$.party").value("Independent"))
                .andExpect(jsonPath("$.symbol").value("🏛️"));

        // Verify in database
        List<Candidate> candidates = candidateRepository.findAll();
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).getName()).isEqualTo("Michael Brown");
        assertThat(candidates.get(0).getBallot().getId()).isEqualTo(ballot.getId());
    }

    @Test
    @DisplayName("Should retrieve all elections")
    void testGetAllElections_Success() throws Exception {
        // Arrange - Create multiple elections
        Election election1 = Election.builder()
                .title("Presidential Election 2024")
                .description("National Presidential Election")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
        electionRepository.save(election1);

        Election election2 = Election.builder()
                .title("Local Election 2024")
                .description("City Council Election")
                .startDate(LocalDateTime.now().plusDays(5))
                .endDate(LocalDateTime.now().plusDays(35))
                .status("DRAFT")
                .createdAt(LocalDateTime.now())
                .build();
        electionRepository.save(election2);

        // Act & Assert
        mockMvc.perform(get("/api/admin/elections")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Presidential Election 2024"))
                .andExpect(jsonPath("$[1].title").value("Local Election 2024"));
    }

    @Test
    @DisplayName("Should retrieve ballots for specific election")
    void testGetBallotsByElection_Success() throws Exception {
        // Arrange
        Election election = Election.builder()
                .title("Test Election")
                .description("Test Description")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
        election = electionRepository.save(election);

        Ballot ballot1 = new Ballot();
        ballot1.setElection(election);
        ballot1.setTitle("President");
        ballot1.setDescription("Presidential Ballot");
        ballotRepository.save(ballot1);

        Ballot ballot2 = new Ballot();
        ballot2.setElection(election);
        ballot2.setTitle("Vice President");
        ballot2.setDescription("Vice Presidential Ballot");
        ballotRepository.save(ballot2);

        // Act & Assert
        mockMvc.perform(get("/api/admin/elections/" + election.getId() + "/ballots")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("President"))
                .andExpect(jsonPath("$[1].title").value("Vice President"));
    }

    @Test
    @DisplayName("Should retrieve candidates for specific ballot")
    void testGetCandidatesByBallot_Success() throws Exception {
        // Arrange
        Election election = Election.builder()
                .title("Test Election")
                .description("Test Description")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
        election = electionRepository.save(election);

        Ballot ballot = new Ballot();
        ballot.setElection(election);
        ballot.setTitle("President");
        ballot.setDescription("Presidential Ballot");
        ballot = ballotRepository.save(ballot);

        Candidate candidate1 = new Candidate();
        candidate1.setBallot(ballot);
        candidate1.setName("Alice Johnson");
        candidate1.setParty("Party A");
        candidate1.setSymbol("🌟");
        candidateRepository.save(candidate1);

        Candidate candidate2 = new Candidate();
        candidate2.setBallot(ballot);
        candidate2.setName("Bob Smith");
        candidate2.setParty("Party B");
        candidate2.setSymbol("⭐");
        candidateRepository.save(candidate2);

        // Act & Assert
        mockMvc.perform(get("/api/admin/ballots/" + ballot.getId() + "/candidates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Alice Johnson"))
                .andExpect(jsonPath("$[1].name").value("Bob Smith"));
    }

    @Test
    @DisplayName("Should approve pending user")
    void testApproveUser_Success() throws Exception {
        // Arrange - Create pending user
        User pendingUser = new User();
        pendingUser.setFullName("Pending User");
        pendingUser.setEmail("pending@example.com");
        pendingUser.setPhone("9999999999");
        pendingUser.setPasswordHash(passwordEncoder.encode("password"));
        pendingUser.setAadhaarHash("hash");
        pendingUser.setRole("USER");
        pendingUser.setDob(LocalDate.of(1990, 1, 1));
        pendingUser.setGender("Male");
        pendingUser.setAddress("Test Address");
        pendingUser.setStatus("PENDING");
        pendingUser.setCreatedAt(LocalDateTime.now());
        pendingUser = userRepository.save(pendingUser);

        // Act & Assert
        mockMvc.perform(put("/api/admin/approve/" + pendingUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("User approved"));

        // Verify in database
        User approvedUser = userRepository.findById(pendingUser.getId()).orElseThrow();
        assertThat(approvedUser.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("Should reject pending user")
    void testRejectUser_Success() throws Exception {
        // Arrange - Create pending user
        User pendingUser = new User();
        pendingUser.setFullName("Pending User");
        pendingUser.setEmail("pending@example.com");
        pendingUser.setPhone("9999999999");
        pendingUser.setPasswordHash(passwordEncoder.encode("password"));
        pendingUser.setAadhaarHash("hash");
        pendingUser.setRole("USER");
        pendingUser.setDob(LocalDate.of(1990, 1, 1));
        pendingUser.setGender("Male");
        pendingUser.setAddress("Test Address");
        pendingUser.setStatus("PENDING");
        pendingUser.setCreatedAt(LocalDateTime.now());
        pendingUser = userRepository.save(pendingUser);

        // Act & Assert
        mockMvc.perform(put("/api/admin/reject/" + pendingUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("User rejected"));

        // Verify in database
        User rejectedUser = userRepository.findById(pendingUser.getId()).orElseThrow();
        assertThat(rejectedUser.getStatus()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("Should retrieve pending users")
    void testGetPendingUsers_Success() throws Exception {
        // Arrange - Create users with different statuses
        User pendingUser1 = new User();
        pendingUser1.setFullName("Pending User 1");
        pendingUser1.setEmail("pending1@example.com");
        pendingUser1.setPhone("9111111111");
        pendingUser1.setPasswordHash(passwordEncoder.encode("password"));
        pendingUser1.setAadhaarHash("hash1");
        pendingUser1.setRole("USER");
        pendingUser1.setDob(LocalDate.of(1990, 1, 1));
        pendingUser1.setGender("Male");
        pendingUser1.setAddress("Address 1");
        pendingUser1.setStatus("PENDING");
        pendingUser1.setCreatedAt(LocalDateTime.now());
        userRepository.save(pendingUser1);

        User approvedUser = new User();
        approvedUser.setFullName("Approved User");
        approvedUser.setEmail("approved@example.com");
        approvedUser.setPhone("9222222222");
        approvedUser.setPasswordHash(passwordEncoder.encode("password"));
        approvedUser.setAadhaarHash("hash2");
        approvedUser.setRole("USER");
        approvedUser.setDob(LocalDate.of(1990, 1, 1));
        approvedUser.setGender("Female");
        approvedUser.setAddress("Address 2");
        approvedUser.setStatus("APPROVED");
        approvedUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(approvedUser);

        // Act & Assert
        mockMvc.perform(get("/api/admin/pending-users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("pending1@example.com"))
                .andExpect(jsonPath("$[0].fullName").value("Pending User 1"));
    }

    @Test
    @DisplayName("Should verify complete election creation flow")
    void testCompleteElectionFlow_Success() throws Exception {
        // Step 1: Create Election
        CreateElectionRequest electionRequest = new CreateElectionRequest();
        electionRequest.setTitle("Complete Flow Election");
        electionRequest.setDescription("Testing complete flow");
        electionRequest.setStartDate(LocalDateTime.now().plusDays(1));
        electionRequest.setEndDate(LocalDateTime.now().plusDays(30));

        String electionResponse = mockMvc.perform(post("/api/admin/elections")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(electionRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Election createdElection = objectMapper.readValue(electionResponse, Election.class);

        // Step 2: Create Ballot
        CreateBallotRequest ballotRequest = new CreateBallotRequest();
        ballotRequest.setTitle("President");
        ballotRequest.setDescription("Presidential Race");

        String ballotResponse = mockMvc.perform(post("/api/admin/elections/" + createdElection.getId() + "/ballots")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ballotRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Ballot createdBallot = objectMapper.readValue(ballotResponse, Ballot.class);

        // Step 3: Add Candidates
        CreateCandidateRequest candidate1Request = new CreateCandidateRequest();
        candidate1Request.setName("Candidate One");
        candidate1Request.setParty("Party One");
        candidate1Request.setSymbol("1️⃣");

        mockMvc.perform(post("/api/admin/ballots/" + createdBallot.getId() + "/candidates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(candidate1Request)))
                .andExpect(status().isOk());

        CreateCandidateRequest candidate2Request = new CreateCandidateRequest();
        candidate2Request.setName("Candidate Two");
        candidate2Request.setParty("Party Two");
        candidate2Request.setSymbol("2️⃣");

        mockMvc.perform(post("/api/admin/ballots/" + createdBallot.getId() + "/candidates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(candidate2Request)))
                .andExpect(status().isOk());

        // Verify all data persisted
        assertThat(electionRepository.findAll()).hasSize(1);
        assertThat(ballotRepository.findAll()).hasSize(1);
        assertThat(candidateRepository.findAll()).hasSize(2);

        // Verify relationships
        Ballot savedBallot = ballotRepository.findAll().get(0);
        assertThat(savedBallot.getElection().getId()).isEqualTo(createdElection.getId());

        List<Candidate> savedCandidates = candidateRepository.findAll();
        assertThat(savedCandidates).allMatch(c -> c.getBallot().getId().equals(createdBallot.getId()));
    }
}
