package com.votum.votum_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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

    private String title;

    private String description;

    private Integer maxSelections;

    private String status;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "election_id", nullable = false)
    @JsonBackReference
    private Election election;

    @OneToMany(mappedBy = "ballot", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<Candidate> candidates;

}
