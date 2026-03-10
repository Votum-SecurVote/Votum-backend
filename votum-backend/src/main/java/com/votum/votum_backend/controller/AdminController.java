package com.votum.votum_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.votum.votum_backend.dto.*;
import com.votum.votum_backend.model.Election;
import com.votum.votum_backend.service.AdminElectionService;
import com.votum.votum_backend.service.AdminService;
import com.votum.votum_backend.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AdminElectionService adminElectionService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String token = adminService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(token);
    }

    // ================= USER APPROVAL =================

    @GetMapping("/pending-users")
    public ResponseEntity<List<UserProfileResponse>> getPendingUsers() {
        return ResponseEntity.ok(userService.getPendingUsers());
    }

    @PutMapping("/approve/{userId}")
    public ResponseEntity<?> approveUser(@PathVariable UUID userId) {
        userService.approveUser(userId);
        return ResponseEntity.ok("User approved");
    }

    @PutMapping("/reject/{userId}")
    public ResponseEntity<?> rejectUser(@PathVariable UUID userId) {
        userService.rejectUser(userId);
        return ResponseEntity.ok("User rejected");
    }

    // ================= ELECTION =================

    /**
     * Creates an election with an optional logo image.
     *
     * Request: multipart/form-data
     *   - request (part): JSON CreateElectionRequest
     *   - logo    (part): image file (optional)
     */
    @PostMapping(value = "/elections", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createElection(
            @RequestPart("request") String requestJson,
            @RequestPart(value = "logo", required = false) MultipartFile logo)
            throws IOException {

        CreateElectionRequest request =
                objectMapper.readValue(requestJson, CreateElectionRequest.class);

        Election election = adminElectionService.createElection(request, logo);
        return ResponseEntity.ok(election);
    }

    @PostMapping("/elections/{electionId}/ballots")
    public ResponseEntity<?> createBallot(
            @PathVariable UUID electionId,
            @RequestBody CreateBallotRequest request) {
        return ResponseEntity.ok(
                adminElectionService.createBallot(electionId, request)
        );
    }

    /**
     * Creates a candidate with an optional photo image.
     *
     * Request: multipart/form-data
     *   - request (part): JSON CreateCandidateRequest
     *   - photo   (part): image file (optional)
     */
    @PostMapping(value = "/ballots/{ballotId}/candidates",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createCandidate(
            @PathVariable UUID ballotId,
            @RequestPart("request") String requestJson,
            @RequestPart(value = "photo", required = false) MultipartFile photo)
            throws IOException {

        CreateCandidateRequest request =
                objectMapper.readValue(requestJson, CreateCandidateRequest.class);

        return ResponseEntity.ok(
                adminElectionService.createCandidate(ballotId, request, photo)
        );
    }

    @GetMapping("/elections")
    public ResponseEntity<?> getAllElections() {
        return ResponseEntity.ok(adminElectionService.getAllElections());
    }

    @GetMapping("/elections/{electionId}/ballots")
    public ResponseEntity<?> getBallotsByElection(@PathVariable UUID electionId) {
        return ResponseEntity.ok(
                adminElectionService.getBallotsByElection(electionId)
        );
    }

    @GetMapping("/ballots/{ballotId}/candidates")
    public ResponseEntity<?> getCandidatesByBallot(@PathVariable UUID ballotId) {
        return ResponseEntity.ok(
                adminElectionService.getCandidatesByBallot(ballotId)
        );
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/elections/{id}/publish")
    public ResponseEntity<Election> publishElection(@PathVariable UUID id) {
        return ResponseEntity.ok(
            adminElectionService.publishElection(id)
        );
    }

    @PutMapping("/elections/{id}/unpublish")
    public ResponseEntity<Election> unpublishElection(@PathVariable UUID id) {
        return ResponseEntity.ok(
            adminElectionService.unpublishElection(id)
        );
    }

    @DeleteMapping("/elections/{id}")
    public ResponseEntity<String> deleteElection(@PathVariable UUID id) {
        adminElectionService.deleteElection(id);
        return ResponseEntity.ok("Election deleted successfully");
    }

}
