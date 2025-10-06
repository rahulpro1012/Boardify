package com.boardify.boardify_service.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CreateTaskRequest {
    @NotBlank(message = "Task title is required")
    private String title; private String description; private String assigneeEmail; // o
}
