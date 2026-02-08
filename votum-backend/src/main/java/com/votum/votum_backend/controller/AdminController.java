package com.votum.votum_backend.controller;

import com.votum.votum_backend.dto.LoginRequest;
import com.votum.votum_backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        String token = adminService.login(request.getEmail(), request.getPassword());

        return ResponseEntity.ok(token);
    }
}
