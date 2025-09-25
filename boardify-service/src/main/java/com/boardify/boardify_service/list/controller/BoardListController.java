package com.boardify.boardify_service.list.controller;


import com.boardify.boardify_service.list.dto.CreateListRequest;
import com.boardify.boardify_service.list.dto.ListDto;
import com.boardify.boardify_service.list.service.BoardListService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/lists")
public class BoardListController {
    private final BoardListService service;
    public BoardListController(BoardListService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ListDto> create(@AuthenticationPrincipal User principal, @PathVariable Long boardId, @Valid @RequestBody CreateListRequest req) {
        return ResponseEntity.ok(service.create(boardId, req, principal.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<ListDto>> get(@AuthenticationPrincipal User principal, @PathVariable Long boardId) {
        return ResponseEntity.ok(service.getByBoard(boardId, principal.getUsername()));
    }
}

