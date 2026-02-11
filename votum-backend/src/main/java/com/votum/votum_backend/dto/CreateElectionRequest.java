package com.votum.votum_backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateElectionRequest {

    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
