package com.boardify.boardify_service.comment.service;

import com.boardify.boardify_service.comment.dto.AddCommentRequest;
import com.boardify.boardify_service.comment.dto.CommentDto;
import com.boardify.boardify_service.comment.entity.CommentEntity;
import com.boardify.boardify_service.comment.repository.CommentRepository;
import com.boardify.boardify_service.common.event.CommentAddedEvent;
import com.boardify.boardify_service.common.kafka.EventPublisher;
import com.boardify.boardify_service.common.kafka.Topics;
import com.boardify.boardify_service.exception.TaskNotFoundException;
import com.boardify.boardify_service.task.entity.TaskEntity;
import com.boardify.boardify_service.task.repository.TaskRepository;
import com.boardify.boardify_service.user.entity.UserEntity;
import com.boardify.boardify_service.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {
    private final CommentRepository comments; private final TaskRepository tasks; private final UserRepository users; private final EventPublisher events;

    public CommentService(CommentRepository comments, TaskRepository tasks, UserRepository users, EventPublisher events) {
        this.comments = comments; this.tasks = tasks; this.users = users; this.events = events; }

    @Transactional
    public CommentDto add(Long taskId, AddCommentRequest req, UserEntity author) {
        TaskEntity t = tasks.findById(taskId).orElseThrow(() -> new TaskNotFoundException("Task not found"));
        CommentEntity c = new CommentEntity(); c.setTask(t); c.setAuthor(author); c.setText(req.getText());
        CommentEntity saved = comments.save(c);
        CommentAddedEvent ev = new CommentAddedEvent(); ev.taskId = taskId; ev.commentId = saved.getId(); ev.author = author.getEmail(); ev.text = saved.getText();
        events.publish(Topics.COMMENT_EVENTS, "comment-added-"+saved.getId(), ev);
        return new CommentDto(saved.getId(), taskId, author.getEmail(), saved.getText(), saved.getCreatedAt());
    }

    public List<CommentDto> get(Long taskId) {
        TaskEntity t = tasks.findById(taskId).orElseThrow(() -> new TaskNotFoundException("Task not found"));
        return comments.findByTaskOrderByCreatedAtAsc(t).stream()
                .map(x -> new CommentDto(x.getId(), taskId, x.getAuthor()!=null?x.getAuthor().getEmail():null, x.getText(), x.getCreatedAt()))
                .toList();
    }
}
