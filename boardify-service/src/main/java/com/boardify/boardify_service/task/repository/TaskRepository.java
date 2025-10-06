package com.boardify.boardify_service.task.repository;

import com.boardify.boardify_service.list.entity.BoardList;
import com.boardify.boardify_service.task.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    List<TaskEntity> findByListOrderByPositionAsc(BoardList list);
    long countByList(BoardList list);
}
