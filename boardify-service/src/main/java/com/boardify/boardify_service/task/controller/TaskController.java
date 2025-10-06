package com.boardify.boardify_service.task.controller;

import com.boardify.boardify_service.common.security.CurrentUserService;
import com.boardify.boardify_service.task.dto.CreateTaskRequest;
import com.boardify.boardify_service.task.dto.MoveTaskRequest;
import com.boardify.boardify_service.task.dto.TaskDto;
import com.boardify.boardify_service.task.dto.UpdateTaskRequest;
import com.boardify.boardify_service.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TaskController {
    private final TaskService service;
    private final CurrentUserService currentUser;
    
    public TaskController(TaskService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    /**
     * Creates a new task in a specific list.
     *
     * @param principal The authenticated user making the request.
     * @param listId The ID of the list to which the task will be added.
     * @param req The request body containing the details of the task to create.
     * @return A ResponseEntity containing the created TaskDto.
     */
    @PostMapping("/api/lists/{listId}/tasks")
    public ResponseEntity<TaskDto> create(
            @AuthenticationPrincipal User principal,
            @PathVariable Long listId,
            @Valid @RequestBody CreateTaskRequest req) {
        var me = currentUser.requireUser(principal);
        return ResponseEntity.ok(service.create(listId, req, me));
    }

    /**
     * Retrieves all tasks belonging to a specific list.
     *
     * @param listId The ID of the list whose tasks are to be retrieved.
     * @return A ResponseEntity containing a list of TaskDto objects.
     */
    @GetMapping("/api/lists/{listId}/tasks")
    public ResponseEntity<List<TaskDto>> get(@PathVariable Long listId) {
        return ResponseEntity.ok(service.getByList(listId));
    }

    /**
     * Updates an existing task.
     *
     * @param taskId The ID of the task to update.
     * @param req The request body containing the updated task details.
     * @return A ResponseEntity containing the updated TaskDto.
     */
    @PutMapping("/api/tasks/{taskId}")
    public ResponseEntity<TaskDto> update(
            @PathVariable Long taskId,
            @RequestBody UpdateTaskRequest req) {
        return ResponseEntity.ok(service.update(taskId, req));
    }

    /**
     * Moves a task to a different list and/or position.
     *
     * @param taskId The ID of the task to move.
     * @param req The request body containing the target list ID and position index.
     * @return A ResponseEntity with 200 OK status on success.
     */
    @PatchMapping("/api/tasks/{taskId}/move")
    public ResponseEntity<?> move(
            @PathVariable Long taskId,
            @RequestBody MoveTaskRequest req) {
        service.move(taskId, req.getToListId(), req.getTargetIndex());
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a task.
     *
     * @param taskId The ID of the task to delete.
     * @return A ResponseEntity with 204 No Content status on success.
     */
    @DeleteMapping("/api/tasks/{taskId}")
    public ResponseEntity<?> delete(@PathVariable Long taskId) {
        service.delete(taskId);
        return ResponseEntity.noContent().build();
    }
}

