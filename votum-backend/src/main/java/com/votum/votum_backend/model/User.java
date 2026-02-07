package com.votum.votum_backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    private String fullName;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

    private String passwordHash;

    @Column(unique = true)
    private String aadhaarHash;

    private LocalDate dob;

    private String gender;

    private String address;

    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
