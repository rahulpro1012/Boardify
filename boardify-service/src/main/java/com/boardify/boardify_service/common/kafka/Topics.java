package com.boardify.boardify_service.common.kafka;

public final class Topics {
    private Topics() {}
    public static final String BOARD_EVENTS = "boardify.board.events";
    public static final String LIST_EVENTS  = "boardify.list.events";
    public static final String TASK_EVENTS  = "boardify.task.events";
    public static final String COMMENT_EVENTS = "boardify.comment.events";
}
