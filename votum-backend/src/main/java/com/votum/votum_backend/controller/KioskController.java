package com.votum.votum_backend.controller;

import com.votum.votum_backend.dto.KioskLoginRequest;
import com.votum.votum_backend.dto.VoteRequest;
import com.votum.votum_backend.model.Ballot;
import com.votum.votum_backend.model.Candidate;
import com.votum.votum_backend.model.Election;
import com.votum.votum_backend.service.KioskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/kiosk")
@RequiredArgsConstructor
public class KioskController {

    private final KioskService kioskService;

    // Login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody KioskLoginRequest request) {
        String token = kioskService.login(request);
        return ResponseEntity.ok(token);
    }

    // Get active election
    @GetMapping("/elections/active")
    public ResponseEntity<Election> getActiveElection() {
        return ResponseEntity.ok(kioskService.getActiveElection());
    }

    // Get ballots for election
    @GetMapping("/elections/{electionId}/ballots")
    public ResponseEntity<List<Ballot>> getBallots(@PathVariable UUID electionId) {
        return ResponseEntity.ok(kioskService.getBallots(electionId));
    }

    // Get candidates for ballot
    @GetMapping("/ballots/{ballotId}/candidates")
    public ResponseEntity<List<Candidate>> getCandidates(@PathVariable UUID ballotId) {
        return ResponseEntity.ok(kioskService.getCandidates(ballotId));
    }

    // Cast vote
    @PostMapping("/vote")
    public ResponseEntity<?> vote(Authentication authentication,
                                  @RequestBody VoteRequest request) {

        kioskService.castVote(authentication.getName(), request);
        return ResponseEntity.ok("Vote cast successfully");
    }
}
