package com.boardify.boardify_service.task.dto;

import lombok.Getter;

@Getter
public class MoveTaskRequest {
    private Long toListId; private int targetIndex;

}
