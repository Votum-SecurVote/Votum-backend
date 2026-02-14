package com.votum.votum_backend.controller;

import com.votum.votum_backend.dto.LoginRequest;
import com.votum.votum_backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.votum.votum_backend.service.AdminElectionService;
import com.votum.votum_backend.dto.CreateElectionRequest;
import com.votum.votum_backend.model.Election;
import com.votum.votum_backend.dto.CreateBallotRequest;
import java.util.UUID;
import com.votum.votum_backend.dto.CreateCandidateRequest;
import java.util.List;
import com.votum.votum_backend.model.Ballot;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AdminElectionService adminElectionService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        String token = adminService.login(request.getEmail(), request.getPassword());

        return ResponseEntity.ok(token);
    }

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
    public ResponseEntity<?> getBallotsByElection(
        @PathVariable UUID electionId) {

        return ResponseEntity.ok(
            adminElectionService.getBallotsByElection(electionId)
        );
    }

    @GetMapping("/ballots/{ballotId}/candidates")
    public ResponseEntity<?> getCandidatesByBallot(
        @PathVariable UUID ballotId) {

        return ResponseEntity.ok(
            adminElectionService.getCandidatesByBallot(ballotId)
        );
    }

}
