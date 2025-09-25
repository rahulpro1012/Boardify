package com.boardify.boardify_service.board.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class CreateBoardRequest {
    @NotBlank(message = "Board name is required")
    private String name;
    private Set<String> memberEmails;

}