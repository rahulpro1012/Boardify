package com.boardify.boardify_service.comment.controller;

import com.boardify.boardify_service.comment.dto.AddCommentRequest;
import com.boardify.boardify_service.comment.dto.CommentDto;
import com.boardify.boardify_service.comment.service.CommentService;
import com.boardify.boardify_service.common.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CommentController {
    private final CommentService service; private final CurrentUserService currentUser;
    public CommentController(CommentService service, CurrentUserService currentUser) { this.service = service; this.currentUser = currentUser; }

    @PostMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<CommentDto> add(@AuthenticationPrincipal User principal, @PathVariable Long taskId, @Valid @RequestBody AddCommentRequest req) {
        var me = currentUser.requireUser(principal);
        return ResponseEntity.ok(service.add(taskId, req, me));
    }

    @GetMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<List<CommentDto>> list(@PathVariable Long taskId) { return ResponseEntity.ok(service.get(taskId)); }
}

