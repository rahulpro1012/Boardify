package com.boardify.boardify_service.task.service;

import com.boardify.boardify_service.common.event.TaskCreatedEvent;
import com.boardify.boardify_service.common.event.TaskDeletedEvent;
import com.boardify.boardify_service.common.event.TaskMovedEvent;
import com.boardify.boardify_service.common.event.TaskUpdatedEvent;
import com.boardify.boardify_service.common.kafka.EventPublisher;
import com.boardify.boardify_service.common.kafka.Topics;
import com.boardify.boardify_service.exception.ListNotFoundException;
import com.boardify.boardify_service.exception.TaskNotFoundException;
import com.boardify.boardify_service.list.entity.BoardList;
import com.boardify.boardify_service.list.repository.BoardListRepository;
import com.boardify.boardify_service.user.repository.UserRepository;
import com.boardify.boardify_service.task.dto.CreateTaskRequest;
import com.boardify.boardify_service.task.dto.TaskDto;
import com.boardify.boardify_service.task.dto.UpdateTaskRequest;
import com.boardify.boardify_service.task.entity.TaskEntity;
import com.boardify.boardify_service.task.repository.TaskRepository;
import com.boardify.boardify_service.user.entity.UserEntity;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {
    private final TaskRepository tasks; private final BoardListRepository lists; private final UserRepository users; private final EventPublisher events;

    public TaskService(TaskRepository tasks, BoardListRepository lists, UserRepository users, EventPublisher events) {
        this.tasks = tasks; this.lists = lists; this.users = users; this.events = events; }

    @Transactional
    public TaskDto create(Long listId, CreateTaskRequest req, UserEntity creator) {
        BoardList list = lists.findById(listId).orElseThrow(() -> new ListNotFoundException("List not found"));
        int nextPos = (int) tasks.countByList(list);
        TaskEntity t = new TaskEntity(); t.setList(list); t.setTitle(req.getTitle()); t.setDescription(req.getDescription()); t.setPosition(nextPos); t.setCreatedBy(creator);
        if (req.getAssigneeEmail() != null) {
            users.findByEmail(req.getAssigneeEmail()).ifPresent(t::setAssignedTo);
        }
        TaskEntity saved = tasks.save(t);

        TaskCreatedEvent ev = new TaskCreatedEvent(); ev.taskId = saved.getId(); ev.listId = listId; ev.title = saved.getTitle(); ev.position = saved.getPosition(); ev.assignedTo = saved.getAssignedTo() != null ? saved.getAssignedTo().getEmail() : null;
        events.publish(Topics.TASK_EVENTS, "task-created-"+saved.getId(), ev);
        return toDto(saved);
    }

    public List<TaskDto> getByList(Long listId) {
        BoardList list = lists.findById(listId).orElseThrow(() -> new ListNotFoundException("List not found"));
        return tasks.findByListOrderByPositionAsc(list).stream().map(this::toDto).toList();
    }

    @Transactional
    public TaskDto update(Long taskId, UpdateTaskRequest req) {
        TaskEntity t = tasks.findById(taskId).orElseThrow(() -> new TaskNotFoundException("Task not found"));
        if (req.getTitle() != null) t.setTitle(req.getTitle());
        if (req.getDescription() != null) t.setDescription(req.getDescription());
        if (req.getAssignedTo() != null) {
            UserEntity assignee = users.findByEmail(req.getAssignedTo()).orElse(null);
            t.setAssignedTo(assignee);
        }
        TaskEntity saved = tasks.save(t);
        TaskUpdatedEvent ev = new TaskUpdatedEvent(); ev.taskId = saved.getId(); ev.listId = saved.getList().getId(); ev.title = saved.getTitle(); ev.assignedTo = saved.getAssignedTo() != null ? saved.getAssignedTo().getEmail() : null;
        events.publish(Topics.TASK_EVENTS, "task-updated-"+saved.getId(), ev);
        return toDto(saved);
    }

    @Transactional
    public void delete(Long taskId) {
        TaskEntity t = tasks.findById(taskId).orElseThrow(() -> new TaskNotFoundException("Task not found"));
        Long listId = t.getList().getId();
        tasks.delete(t);
        TaskDeletedEvent ev = new TaskDeletedEvent(); ev.taskId = taskId; ev.listId = listId;
        events.publish(Topics.TASK_EVENTS, "task-deleted-"+taskId, ev);
    }

    /** Drag & drop move (within same list or across lists) with stable reindexing */
    @Transactional
    public void move(Long taskId, Long toListId, int targetIndex) {
        TaskEntity t = tasks.findById(taskId).orElseThrow(() -> new TaskNotFoundException("Task not found"));
        BoardList from = t.getList();
        BoardList to = lists.findById(toListId).orElseThrow(() -> new ListNotFoundException("Target list not found"));

        if (from.getId().equals(to.getId())) {
            // reorder within same list
            List<TaskEntity> all = tasks.findByListOrderByPositionAsc(from);
            all.removeIf(x -> x.getId().equals(taskId));
            if (targetIndex < 0) targetIndex = 0;
            if (targetIndex > all.size()) targetIndex = all.size();
            all.add(targetIndex, t);
            for (int i = 0; i < all.size(); i++) all.get(i).setPosition(i);
            tasks.saveAll(all);
        } else {
            // move across lists
            List<TaskEntity> fromTasks = tasks.findByListOrderByPositionAsc(from);
            fromTasks.removeIf(x -> x.getId().equals(taskId));
            for (int i = 0; i < fromTasks.size(); i++) fromTasks.get(i).setPosition(i);

            List<TaskEntity> toTasks = tasks.findByListOrderByPositionAsc(to);
            if (targetIndex < 0) targetIndex = 0;
            if (targetIndex > toTasks.size()) targetIndex = toTasks.size();
            t.setList(to);
            toTasks.add(targetIndex, t);
            for (int i = 0; i < toTasks.size(); i++) toTasks.get(i).setPosition(i);

            tasks.saveAll(fromTasks);
            tasks.saveAll(toTasks);
        }

        TaskMovedEvent ev = new TaskMovedEvent(); ev.taskId = taskId; ev.fromListId = from.getId(); ev.toListId = to.getId(); ev.targetIndex = targetIndex;
        events.publish(Topics.TASK_EVENTS, "task-moved-"+taskId, ev);
    }

    private TaskDto toDto(TaskEntity t) {
        return new TaskDto(t.getId(), t.getList().getId(), t.getTitle(), t.getDescription(), t.getPosition(),
                t.getAssignedTo() != null ? t.getAssignedTo().getEmail() : null);
    }
}
