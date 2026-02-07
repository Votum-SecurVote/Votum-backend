package com.votum.votum_backend.controller;

import com.votum.votum_backend.dto.RegisterRequest;
import com.votum.votum_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping(value = "/register", consumes = "multipart/form-data")
    public ResponseEntity<?> register(
            @RequestPart("data") String requestJson,
            @RequestPart("photo") MultipartFile photo,
            @RequestPart("aadhaarPdf") MultipartFile aadhaarPdf
    ) throws Exception {

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            RegisterRequest request = mapper.readValue(requestJson, RegisterRequest.class);
            
            userService.register(request, photo, aadhaarPdf);
            return ResponseEntity.ok("User registered successfully. Await admin approval.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error parsing request: " + e.getMessage());
        }
    }
}
