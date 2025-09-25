package com.boardify.boardify_service.common.event;

import java.time.Instant;

public class BoardUpdatedEvent { public Long boardId; public String name; public Instant updatedAt = Instant.now(); public String version = "1"; }
