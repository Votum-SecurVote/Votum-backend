package com.votum.votum_backend.repository;

import com.votum.votum_backend.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {

    Optional<Admin> findByEmail(String email);
}
