package com.votum.votum_backend.service;

import com.votum.votum_backend.dto.AdminMetricsResponse;
import com.votum.votum_backend.dto.CreateBallotRequest;
import com.votum.votum_backend.dto.CreateCandidateRequest;
import com.votum.votum_backend.dto.CreateElectionRequest;
import com.votum.votum_backend.model.Ballot;
import com.votum.votum_backend.model.Candidate;
import com.votum.votum_backend.model.Election;
import com.votum.votum_backend.repository.AuditLogRepository;
import com.votum.votum_backend.repository.BallotRepository;
import com.votum.votum_backend.repository.CandidateRepository;
import com.votum.votum_backend.repository.ElectionRepository;
import com.votum.votum_backend.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;

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

        Election savedElection = electionRepository.save(election);
        auditLogService.logAction("Created election: " + savedElection.getTitle());
        return savedElection;
    }

    public Election publishElection(UUID electionId) {
        Election election = getElectionOrThrow(electionId);
        election.setStatus("PUBLISHED");
        Election savedElection = electionRepository.save(election);
        auditLogService.logAction("Published election: " + savedElection.getTitle());
        return savedElection;
    }

    public Election unpublishElection(UUID electionId) {
        Election election = getElectionOrThrow(electionId);
        election.setStatus("DRAFT");
        Election savedElection = electionRepository.save(election);
        auditLogService.logAction("Unpublished election: " + savedElection.getTitle());
        return savedElection;
    }

    public void deleteElection(UUID electionId) {
        Election election = getElectionOrThrow(electionId);
        auditLogService.logAction("Deleted election: " + election.getTitle());
        electionRepository.delete(election);
    }

    public List<Election> getAllElections() {
        return electionRepository.findAll();
    }

    public AdminMetricsResponse getMetrics() {
        long totalElections = electionRepository.count();
        long activeElections = electionRepository.findAll().stream()
                .filter(e -> "PUBLISHED".equals(e.getStatus())).count();
        long pendingUsers = userRepository.findByStatus("PENDING").size();
        long approvedUsers = userRepository.findByStatus("APPROVED").size();

        return AdminMetricsResponse.builder()
                .totalElections(totalElections)
                .activeElections(activeElections)
                .pendingCandidates(pendingUsers)
                .approvedCandidates(approvedUsers)
                .recentActivity(auditLogRepository.findTop10ByOrderByTimeDesc())
                .build();
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

        Ballot savedBallot = ballotRepository.save(ballot);
        auditLogService.logAction("Created ballot: " + savedBallot.getTitle() + " for election: " + election.getTitle());
        return savedBallot;
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

        Ballot publishedBallot = ballotRepository.save(ballot);
        auditLogService.logAction("Published ballot: " + publishedBallot.getTitle() + " for election: " + ballot.getElection().getTitle());
        return publishedBallot;
    }

    public Ballot unpublishBallot(UUID ballotId) {
        Ballot ballot = getBallotOrThrow(ballotId);
        ballot.setStatus("DRAFT");
        Ballot unpublishedBallot = ballotRepository.save(ballot);
        auditLogService.logAction("Unpublished ballot: " + unpublishedBallot.getTitle() + " for election: " + ballot.getElection().getTitle());
        return unpublishedBallot;
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
                                     MultipartFile photoFile,
                                     MultipartFile symbolFile) throws IOException {

        Ballot ballot = getBallotOrThrow(ballotId);

        Candidate candidate = Candidate.builder()
                .ballot(ballot)
                .name(request.getName())
                .party(request.getParty())
                .createdAt(LocalDateTime.now())
                .build();

        // Persist first so we have the generated UUID
        candidate = candidateRepository.save(candidate);

        UUID electionId = ballot.getElection().getId();

        if (photoFile != null && !photoFile.isEmpty()) {
            // Save photo as elections/{electionId}/candidates/{candidateId}.jpg
            String photoPath = fileStorageService.saveFile(
                    photoFile,
                    "elections",
                    electionId.toString(),
                    "candidates",
                    candidate.getId().toString() + ".jpg");
            candidate.setPhotoPath(photoPath);
        }

        if (symbolFile != null && !symbolFile.isEmpty()) {
            // Save symbol logo as elections/{electionId}/candidates/{candidateId}_symbol.png
            String symbolPath = fileStorageService.saveFile(
                    symbolFile,
                    "elections",
                    electionId.toString(),
                    "candidates",
                    candidate.getId().toString() + "_symbol.png");
            candidate.setSymbolPath(symbolPath);
        }

        if ((photoFile != null && !photoFile.isEmpty()) ||
            (symbolFile != null && !symbolFile.isEmpty())) {
            candidate = candidateRepository.save(candidate);
        }

        auditLogService.logAction("Added candidate: " + candidate.getName() + " to ballot: " + ballot.getTitle());
        return candidate;
    }

    public List<Candidate> getCandidatesByBallot(UUID ballotId) {
        return candidateRepository.findByBallot_Id(ballotId);
    }
}
