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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminElectionService {

    private final ElectionRepository electionRepository;
    private final BallotRepository ballotRepository;
    private final CandidateRepository candidateRepository;

    /* =========================================================
                            ELECTION
       ========================================================= */

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

    public Election publishElection(UUID electionId) {
        Election election = getElectionOrThrow(electionId);
        election.setStatus("PUBLISHED");
        return electionRepository.save(election);
    }

    public Election unpublishElection(UUID electionId) {
        Election election = getElectionOrThrow(electionId);
        election.setStatus("DRAFT");
        return electionRepository.save(election);
    }

    public void deleteElection(UUID electionId) {
        Election election = getElectionOrThrow(electionId);
        electionRepository.delete(election);
    }

    public List<Election> getAllElections() {
        return electionRepository.findAll();
    }

    private Election getElectionOrThrow(UUID id) {
        return electionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Election not found"));
    }

    /* =========================================================
                            BALLOT
       ========================================================= */

    public Ballot createBallot(UUID electionId, CreateBallotRequest request) {

        Election election = getElectionOrThrow(electionId);

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

    public Ballot publishBallot(UUID ballotId) {

        Ballot ballot = getBallotOrThrow(ballotId);

        // Unpublish all ballots of same election
        List<Ballot> ballots =
                ballotRepository.findByElection_Id(ballot.getElection().getId());

        for (Ballot b : ballots) {
            b.setStatus("DRAFT");
        }

        ballot.setStatus("PUBLISHED");

        ballotRepository.saveAll(ballots);

        return ballotRepository.save(ballot);
    }

    public Ballot unpublishBallot(UUID ballotId) {
        Ballot ballot = getBallotOrThrow(ballotId);
        ballot.setStatus("DRAFT");
        return ballotRepository.save(ballot);
    }

    public List<Ballot> getBallotsByElection(UUID electionId) {
        return ballotRepository.findByElection_Id(electionId);
    }

    private Ballot getBallotOrThrow(UUID id) {
        return ballotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ballot not found"));
    }

    /* =========================================================
                            CANDIDATE
       ========================================================= */

    public Candidate createCandidate(UUID ballotId, CreateCandidateRequest request) {

        Ballot ballot = getBallotOrThrow(ballotId);

        Candidate candidate = Candidate.builder()
                .ballot(ballot)
                .name(request.getName())
                .party(request.getParty())
                .symbol(request.getSymbol())
                .createdAt(LocalDateTime.now())
                .build();

        return candidateRepository.save(candidate);
    }

    public List<Candidate> getCandidatesByBallot(UUID ballotId) {
        return candidateRepository.findByBallot_Id(ballotId);
    }
}
