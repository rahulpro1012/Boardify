package com.boardify.boardify_service.task.dto;

public record TaskDto(Long id, Long listId, String title, String description, int position, String assignedTo) {}
