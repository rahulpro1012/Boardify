package com.boardify.boardify_service.repository;

import com.boardify.boardify_service.board.BoardEntity;
import com.boardify.boardify_service.list.entity.BoardList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardListRepository extends JpaRepository<BoardList, Long> {
    List<BoardList> findByBoardOrderByPositionAsc(BoardEntity board);
    long countByBoard(BoardEntity board);

    @Query("SELECT COALESCE(MAX(l.position), 0) FROM BoardList l WHERE l.board.id = :boardId")
    Double findMaxPositionByBoardId(@Param("boardId") Long boardId);


}

