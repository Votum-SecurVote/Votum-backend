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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminElectionService {

    private final ElectionRepository electionRepository;
    private final BallotRepository ballotRepository;
    private final CandidateRepository candidateRepository;
    private final FileStorageService fileStorageService;

    /* =========================================================
                            ELECTION
       ========================================================= */

    /**
     * Creates an election and, if a logo file is provided, saves it to
     * Storage/elections/{electionId}/logo.png and stores the path on the entity.
     */
    public Election createElection(CreateElectionRequest request, MultipartFile logoFile)
            throws IOException {

        Election election = Election.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status("DRAFT")
                .createdAt(LocalDateTime.now())
                .build();

        // Persist first so we have the generated UUID
        election = electionRepository.save(election);

        if (logoFile != null && !logoFile.isEmpty()) {
            // Save logo as elections/{electionId}/logo.png
            String logoPath = fileStorageService.saveFile(
                    logoFile,
                    "elections",
                    election.getId().toString(),
                    "logo.png");
            election.setLogoPath(logoPath);
            election = electionRepository.save(election);
        } else {
            // Ensure the election directory exists even without a logo
            fileStorageService.createDirectory("elections", election.getId().toString());
        }

        return election;
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

        // Unpublish all ballots of the same election
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

    /**
     * Creates a candidate and, if a photo file is provided, saves it to
     * Storage/elections/{electionId}/candidates/{candidateId}.jpg
     * and stores the path on the entity.
     */
    public Candidate createCandidate(UUID ballotId, CreateCandidateRequest request,
                                     MultipartFile photoFile) throws IOException {

        Ballot ballot = getBallotOrThrow(ballotId);

        Candidate candidate = Candidate.builder()
                .ballot(ballot)
                .name(request.getName())
                .party(request.getParty())
                .symbol(request.getSymbol())
                .createdAt(LocalDateTime.now())
                .build();

        // Persist first so we have the generated UUID
        candidate = candidateRepository.save(candidate);

        if (photoFile != null && !photoFile.isEmpty()) {
            UUID electionId = ballot.getElection().getId();
            // Save photo as elections/{electionId}/candidates/{candidateId}.jpg
            String photoPath = fileStorageService.saveFile(
                    photoFile,
                    "elections",
                    electionId.toString(),
                    "candidates",
                    candidate.getId().toString() + ".jpg");
            candidate.setPhotoPath(photoPath);
            candidate = candidateRepository.save(candidate);
        }

        return candidate;
    }

    public List<Candidate> getCandidatesByBallot(UUID ballotId) {
        return candidateRepository.findByBallot_Id(ballotId);
    }
}
