package com.votum.votum_backend.dto;

import lombok.Data;

@Data
public class KioskLoginRequest {
    private String email;
    private String password;
    private String aadhaar;
}
