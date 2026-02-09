package com.votum.votum_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ballots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ballot {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    private String title;

    private String description;

    private Integer maxSelections;

    private String status;

    private LocalDateTime createdAt;
}
