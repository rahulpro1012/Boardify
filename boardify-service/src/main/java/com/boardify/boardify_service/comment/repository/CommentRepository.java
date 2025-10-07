package com.boardify.boardify_service.comment.repository;

import com.boardify.boardify_service.comment.entity.CommentEntity;
import com.boardify.boardify_service.task.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    List<CommentEntity> findByTaskOrderByCreatedAtAsc(TaskEntity task);
}