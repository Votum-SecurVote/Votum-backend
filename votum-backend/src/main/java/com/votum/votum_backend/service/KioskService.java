package com.votum.votum_backend.service;

import com.votum.votum_backend.dto.KioskLoginRequest;
import com.votum.votum_backend.dto.VoteRequest;
import com.votum.votum_backend.model.*;
import com.votum.votum_backend.repository.*;
import com.votum.votum_backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KioskService {

    private final UserRepository userRepository;
    private final ElectionRepository electionRepository;
    private final BallotRepository ballotRepository;
    private final CandidateRepository candidateRepository;
    private final VoteRepository voteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 🔐 Login
    public String login(KioskLoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        // Verify Aadhaar
        String aadhaarHash = hashAadhaar(request.getAadhaar());
        if (!aadhaarHash.equals(user.getAadhaarHash())) {
            throw new RuntimeException("Invalid Aadhaar number");
        }

        if (!user.getStatus().equals("APPROVED")) {
            throw new RuntimeException("User not approved");
        }

        return jwtUtil.generateToken(user.getEmail(), user.getRole());
    }

    // 🗳 Get active election
    public Election getActiveElection() {
        List<Election> activeElections = electionRepository.findByStatus("PUBLISHED");
        if (activeElections.isEmpty()) {
            throw new RuntimeException("No active election found");
        }
        // Return the first active election as per requirement
        return activeElections.get(0);
    }

    // 🗂 Get ballots
    public List<Ballot> getBallots(UUID electionId) {
        return ballotRepository.findByElection_Id(electionId);
    }

    // 👤 Get candidates
    public List<Candidate> getCandidates(UUID ballotId) {
        return candidateRepository.findByBallot_Id(ballotId);
    }

    // 🗳 Cast vote
    public void castVote(String email, VoteRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent double voting
        if (voteRepository.existsByUserIdAndElectionId(user.getId(), request.getElectionId())) {
            throw new RuntimeException("User already voted in this election");
        }

        Vote vote = new Vote();
        vote.setUser(user);
        vote.setElectionId(request.getElectionId());
        vote.setBallotId(request.getBallotId());
        vote.setCandidateId(request.getCandidateId());
        vote.setVotedAt(LocalDateTime.now());

        voteRepository.save(vote);
    }

    private String hashAadhaar(String aadhaar) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.Base64.getEncoder().encodeToString(md.digest(aadhaar.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Aadhaar hashing failed");
        }
    }

}
