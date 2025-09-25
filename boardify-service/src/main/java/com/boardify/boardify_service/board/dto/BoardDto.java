package com.boardify.boardify_service.board.dto;

import java.time.Instant;
import java.util.Set;

public record BoardDto(
    Long id,
    String name,
    String createdBy,
    Instant createdAt,
    Set<String> members
) { }