package com.boardify.boardify_service.board.service;

import com.boardify.boardify_service.board.BoardEntity;
import com.boardify.boardify_service.repository.BoardRepository;
import org.springframework.stereotype.Component;

@Component
public class BoardAccessGuard {
    private final BoardRepository boards;
    public BoardAccessGuard(BoardRepository boards) { this.boards = boards; }

    public BoardEntity requireBoard(Long id) {
        return boards.findById(id).orElseThrow(() -> new RuntimeException("Board not found"));
    }

    public void assertMember(BoardEntity b, String email) {
        boolean owner = b.getCreatedBy().getEmail().equals(email);
        boolean member = b.getMembers().stream().anyMatch(u -> u.getEmail().equals(email));
        if (!owner && !member) throw new RuntimeException("Forbidden: not a board member");
    }
}
