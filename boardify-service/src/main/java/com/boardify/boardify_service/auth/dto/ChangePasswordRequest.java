package com.boardify.boardify_service.auth.dto;

public record ChangePasswordRequest(String currentPassword, String newPassword) { }