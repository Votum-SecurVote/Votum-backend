package com.votum.votum_backend.repository;

import com.votum.votum_backend.model.UserBiometrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserBiometricsRepository extends JpaRepository<UserBiometrics, UUID> {
}
