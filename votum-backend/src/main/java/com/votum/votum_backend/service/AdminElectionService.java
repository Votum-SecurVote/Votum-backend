package com.votum.votum_backend.service;

import com.votum.votum_backend.dto.CreateElectionRequest;
import com.votum.votum_backend.model.Election;
import com.votum.votum_backend.repository.ElectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;
import com.votum.votum_backend.model.Ballot;
import com.votum.votum_backend.model.Candidate;
import com.votum.votum_backend.model.Election;
import com.votum.votum_backend.dto.CreateBallotRequest;
import com.votum.votum_backend.dto.CreateCandidateRequest;
import com.votum.votum_backend.repository.BallotRepository;
import com.votum.votum_backend.repository.CandidateRepository;
import com.votum.votum_backend.repository.ElectionRepository;
import java.util.List;


import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminElectionService {

    private final ElectionRepository electionRepository;
    private final BallotRepository ballotRepository;
    private final CandidateRepository candidateRepository;

    public Election createElection(CreateElectionRequest request) {

        Election election = Election.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status("DRAFT")
                .createdAt(LocalDateTime.now())
                .build();

        return electionRepository.save(election);
    }

    public Ballot createBallot(UUID electionId, CreateBallotRequest request) {

        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new RuntimeException("Election not found"));

        Ballot ballot = Ballot.builder()
                .election(election)
                .title(request.getTitle())
                .description(request.getDescription())
                .maxSelections(request.getMaxSelections())
                .status("DRAFT")
                .createdAt(LocalDateTime.now())
                .build();

        return ballotRepository.save(ballot);
    }

    public Candidate createCandidate(UUID ballotId, CreateCandidateRequest request) {

        Ballot ballot = ballotRepository.findById(ballotId)
                .orElseThrow(() -> new RuntimeException("Ballot not found"));

        Candidate candidate = Candidate.builder()
                .ballot(ballot)
                .name(request.getName())
                .party(request.getParty())
                .symbol(request.getSymbol())
                .createdAt(LocalDateTime.now())
                .build();

        return candidateRepository.save(candidate);
    }

    public List<Election> getAllElections() {
        return electionRepository.findAll();
    }

    public List<Ballot> getBallotsByElection(UUID electionId) {
        return ballotRepository.findByElection_Id(electionId);
    }

    public List<Candidate> getCandidatesByBallot(UUID ballotId) {
        return candidateRepository.findByBallot_Id(ballotId);
    }

}
