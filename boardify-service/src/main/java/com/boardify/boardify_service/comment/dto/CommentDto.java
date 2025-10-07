package com.boardify.boardify_service.comment.dto;

import java.time.Instant;

public record CommentDto(Long id, Long taskId, String author, String text, Instant createdAt) {}
