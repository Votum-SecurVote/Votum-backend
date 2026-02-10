package com.votum.votum_backend.repository;

import com.votum.votum_backend.model.Ballot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface BallotRepository extends JpaRepository<Ballot, UUID> {
    List<Ballot> findByElection_Id(UUID electionId);
}
