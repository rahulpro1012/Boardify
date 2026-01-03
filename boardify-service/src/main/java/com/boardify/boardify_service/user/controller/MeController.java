package com.boardify.boardify_service.user.controller;

import com.boardify.boardify_service.user.dto.UserDto;
import com.boardify.boardify_service.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
public class MeController {

    private final UserRepository userRepository;

    MeController(UserRepository userRepository){
        this.userRepository = userRepository;

    }


    @GetMapping("/test")
    public String me(@AuthenticationPrincipal User user) {
        return "Security is working";

    }
    @GetMapping("/api/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal User principal) {
        // 1. 'principal' contains the user details from the JWT token
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        // 2. Fetch full details from your Database using the email/username from the token
        // Assuming you have a 'userService' or 'userRepository' injected
        var userEntity = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Convert to DTO
        UserDto response = new UserDto(
                userEntity.getId(),
                userEntity.getEmail(),
                userEntity.getRoles().toString() // Replace with userEntity.getRole() if you have roles
        );

        return ResponseEntity.ok(response);
    }
}