package com.votum.votum_backend.dto;

import lombok.Data;

@Data
public class CreateBallotRequest {

    private String title;
    private String description;
    private Integer maxSelections;
}
