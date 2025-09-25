package com.boardify.boardify_service.board.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record BoardMemberRequest(
    @NotNull(message = "Member email is required")
    @Email(message = "Member email should be valid")
    String memberEmail
) {}
