package com.votum.votum_backend.dto;

import lombok.Data;

@Data
public class CreateCandidateRequest {
    private String name;
    private String party;
    private String symbol;
}
