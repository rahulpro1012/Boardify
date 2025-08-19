package com.boardify.boardify_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
public class MeController {
    @GetMapping("/me")
    public String me(@AuthenticationPrincipal User user) {
        return "Security is working";

    }
}