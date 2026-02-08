package com.votum.votum_backend.service;

import com.votum.votum_backend.dto.CreateElectionRequest;
import com.votum.votum_backend.model.Election;
import com.votum.votum_backend.repository.ElectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminElectionService {

    private final ElectionRepository electionRepository;

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
}
