package com.votum.votum_backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admins")
@Data
public class Admin {

    @Id
    @GeneratedValue
    private UUID id;

    private String fullName;

    @Column(unique = true)
    private String email;

    private String passwordHash;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
