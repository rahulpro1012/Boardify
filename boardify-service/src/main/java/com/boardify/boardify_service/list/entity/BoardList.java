package com.boardify.boardify_service.list.entity;

import com.boardify.boardify_service.board.BoardEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "board_lists")
@Getter
@Setter
public class BoardList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private BoardEntity board;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int position; // 0..N for ordering left->right

}