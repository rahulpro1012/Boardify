package com.boardify.boardify_service.board.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateBoardRequest {
    @NotBlank(message = "Board name is required")
    private String name;
    public String getName() { return name; }

}
