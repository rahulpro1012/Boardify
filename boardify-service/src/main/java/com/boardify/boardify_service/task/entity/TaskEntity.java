package com.boardify.boardify_service.task.entity;

import com.boardify.boardify_service.list.entity.BoardList;
import com.boardify.boardify_service.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "tasks")
@Data
public class TaskEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id")
    private BoardList list;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(nullable = false)
    private int position; // 0..N within its list

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private UserEntity assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private UserEntity createdBy;

    private Instant createdAt = Instant.now();

    // getters/setters omitted for brevity
    // ...
}

