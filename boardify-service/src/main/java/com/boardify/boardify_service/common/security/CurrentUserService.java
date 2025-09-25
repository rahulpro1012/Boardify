package com.boardify.boardify_service.common.security;

import com.boardify.boardify_service.repository.UserRepository;
import com.boardify.boardify_service.user.UserEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {
    private final UserRepository users;


    public CurrentUserService(UserRepository users) { this.users = users; }


    public UserEntity requireUser(@AuthenticationPrincipal User principal) {
        return users.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
