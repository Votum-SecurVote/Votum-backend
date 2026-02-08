package com.votum.votum_backend.controller;

import com.votum.votum_backend.dto.LoginRequest;
import com.votum.votum_backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.votum.votum_backend.service.AdminElectionService;
import com.votum.votum_backend.dto.CreateElectionRequest;
import com.votum.votum_backend.model.Election;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AdminElectionService adminElectionService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        String token = adminService.login(request.getEmail(), request.getPassword());

        return ResponseEntity.ok(token);
    }

    @PostMapping("/elections")
    public ResponseEntity<?> createElection(@RequestBody CreateElectionRequest request) {

        Election election = adminElectionService.createElection(request);

        return ResponseEntity.ok(election);
    }
}
