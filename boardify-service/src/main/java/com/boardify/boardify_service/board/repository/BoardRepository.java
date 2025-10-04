package com.boardify.boardify_service.board.repository;

import com.boardify.boardify_service.board.entity.BoardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BoardRepository extends JpaRepository<BoardEntity, Long> {
    List<BoardEntity> findByCreatedByEmail(String email);
    @Query("select b from BoardEntity b where b.createdBy.email = :email or exists (select m from b.members m where m.email = :email)")
    List<BoardEntity> findAllVisibleTo(String email);
}
