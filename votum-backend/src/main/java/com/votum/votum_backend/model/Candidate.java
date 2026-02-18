package com.votum.votum_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "candidates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "ballot_id", nullable = false)
    @JsonBackReference
    private Ballot ballot;

    @Column(nullable = false)
    private String name;

    private String party;

    private String symbol;

    private LocalDateTime createdAt;
}
