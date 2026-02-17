package com.votum.votum_backend.controller;

import com.votum.votum_backend.dto.*;
import com.votum.votum_backend.model.Election;
import com.votum.votum_backend.service.AdminElectionService;
import com.votum.votum_backend.service.AdminService;
import com.votum.votum_backend.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AdminElectionService adminElectionService;
    private final UserService userService;

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

    @PostMapping("/elections")
    public ResponseEntity<?> createElection(@RequestBody CreateElectionRequest request) {
        Election election = adminElectionService.createElection(request);
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

    @PostMapping("/ballots/{ballotId}/candidates")
    public ResponseEntity<?> createCandidate(
            @PathVariable UUID ballotId,
            @RequestBody CreateCandidateRequest request) {
        return ResponseEntity.ok(
                adminElectionService.createCandidate(ballotId, request)
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

}
