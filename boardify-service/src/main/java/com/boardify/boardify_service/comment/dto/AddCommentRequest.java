package com.boardify.boardify_service.comment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AddCommentRequest {
    @NotBlank(message = "Comment text is required")
    private String text;
}
