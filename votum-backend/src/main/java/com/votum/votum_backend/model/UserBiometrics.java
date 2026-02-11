package com.votum.votum_backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "user_biometrics")
@Data
public class UserBiometrics {

    @Id
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "bytea")
    private byte[] faceEmbedding;

    private String photoPath;

    private String aadhaarPdfPath;
}
