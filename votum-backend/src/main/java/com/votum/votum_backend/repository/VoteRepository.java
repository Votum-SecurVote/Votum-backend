package com.votum.votum_backend.repository;

import com.votum.votum_backend.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, UUID> {

    boolean existsByUserIdAndElectionId(UUID userId, UUID electionId);
}
