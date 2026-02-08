package com.votum.votum_backend.repository;

import com.votum.votum_backend.model.Election;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ElectionRepository extends JpaRepository<Election, UUID> {
}
