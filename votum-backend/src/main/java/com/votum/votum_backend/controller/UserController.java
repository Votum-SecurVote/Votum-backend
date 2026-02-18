package com.votum.votum_backend.controller;

import com.votum.votum_backend.dto.UserProfileResponse;
import com.votum.votum_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import com.votum.votum_backend.repository.ElectionRepository;
import com.votum.votum_backend.model.Election;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Autowired
    private ElectionRepository electionRepository;


    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(userService.getProfile(email));
    }

    @GetMapping("/elections")
    public ResponseEntity<List<Election>> getPublishedElections() {
        return ResponseEntity.ok(
            electionRepository.findByStatus("PUBLISHED")
        );
    }

}
