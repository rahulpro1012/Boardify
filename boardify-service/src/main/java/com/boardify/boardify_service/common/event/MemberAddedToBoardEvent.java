package com.boardify.boardify_service.common.event;

import java.time.Instant;

public class MemberAddedToBoardEvent { public Long boardId; public String memberEmail; public Instant addedAt = Instant.now(); public String version = "1"; }

