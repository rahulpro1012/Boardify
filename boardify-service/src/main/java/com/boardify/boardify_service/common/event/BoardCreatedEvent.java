package com.boardify.boardify_service.common.event;

import java.time.Instant;
import java.util.Set;

public class BoardCreatedEvent {
    public Long boardId; public String name; public String createdBy; public Instant createdAt; public Set<String> members; public String version = "1";
}

