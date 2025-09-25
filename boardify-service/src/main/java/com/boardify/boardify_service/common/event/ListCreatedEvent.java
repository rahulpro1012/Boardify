package com.boardify.boardify_service.common.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListCreatedEvent { public Long boardId; public Long listId; public String name; public int position; public String version = "1"; }
