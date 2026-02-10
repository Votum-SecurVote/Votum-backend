package com.votum.votum_backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class VoteRequest {
    private UUID electionId;
    private UUID ballotId;
    private UUID candidateId;
}
