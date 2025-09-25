package com.boardify.boardify_service.list.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CreateListRequest {
    @NotBlank(message = "List name is required")
    private String name;
}
