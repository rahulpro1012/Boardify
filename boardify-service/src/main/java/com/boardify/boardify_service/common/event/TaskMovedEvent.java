package com.boardify.boardify_service.common.event;

public class TaskMovedEvent { public Long taskId; public Long fromListId; public Long toListId; public int targetIndex; public String version = "1"; }