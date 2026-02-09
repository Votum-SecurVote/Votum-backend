package com.votum.votum_backend.repository;

import com.votum.votum_backend.model.Ballot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BallotRepository extends JpaRepository<Ballot, UUID> {
}
