package com.boardify.boardify_service.task.dto;

import lombok.Getter;

@Getter
public class UpdateTaskRequest {
    private String title; private String description; private String assignedTo;
}
