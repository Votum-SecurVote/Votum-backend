package com.votum.votum_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UserProfileResponse {

    private String fullName;
    private String userId;
    private String email;
    private String phone;
    private LocalDate dob;
    private String gender;
    private String address;
    private String status;
    private String photoPath;
    private String aadhaarPdfPath;
}
