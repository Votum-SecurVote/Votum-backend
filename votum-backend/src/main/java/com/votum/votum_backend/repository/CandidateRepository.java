package com.votum.votum_backend.repository;

import com.votum.votum_backend.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, UUID> {
}
