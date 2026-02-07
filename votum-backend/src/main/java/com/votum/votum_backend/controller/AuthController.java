package com.votum.votum_backend.controller;

import com.votum.votum_backend.dto.RegisterRequest;
import com.votum.votum_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        userService.register(request);

        return ResponseEntity.ok("User registered successfully. Await admin approval.");
    }
}
