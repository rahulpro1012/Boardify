package com.boardify.boardify_service.user.dto;

import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String role // Optional: "ADMIN", "USER", etc.
) {}
