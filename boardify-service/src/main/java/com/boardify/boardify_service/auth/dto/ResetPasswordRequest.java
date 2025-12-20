package com.boardify.boardify_service.auth.dto;

public record ResetPasswordRequest(String token, String newPassword) {}
