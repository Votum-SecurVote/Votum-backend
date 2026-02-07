package com.votum.votum_backend.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    private String fullName;
    private String email;
    private String phone;
    private String password;
    private String aadhaar;
    private LocalDate dob;
    private String gender;
    private String address;
}
