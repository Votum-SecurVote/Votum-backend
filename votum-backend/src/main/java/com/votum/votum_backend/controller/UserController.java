package com.votum.votum_backend.controller;

import com.votum.votum_backend.dto.UserProfileResponse;
import com.votum.votum_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(userService.getProfile(email));
    }
}
