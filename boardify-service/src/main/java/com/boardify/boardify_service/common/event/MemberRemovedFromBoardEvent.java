package com.boardify.boardify_service.common.event;

import java.time.Instant;

public record MemberRemovedFromBoardEvent(Long boardId, String memberEmail, Instant removedAt ) {
}
