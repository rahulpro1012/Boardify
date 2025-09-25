package com.boardify.boardify_service.repository;

import com.boardify.boardify_service.board.BoardEntity;
import com.boardify.boardify_service.list.entity.BoardList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardListRepository extends JpaRepository<BoardList, Long> {
    List<BoardList> findByBoardOrderByPositionAsc(BoardEntity board);
    long countByBoard(BoardEntity board);
}

