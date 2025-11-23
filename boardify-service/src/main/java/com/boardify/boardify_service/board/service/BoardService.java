package com.boardify.boardify_service.board.service;

import com.boardify.boardify_service.board.entity.BoardEntity;
import com.boardify.boardify_service.board.dto.BoardDto;
import com.boardify.boardify_service.board.dto.CreateBoardRequest;
import com.boardify.boardify_service.board.dto.UpdateBoardRequest;
import com.boardify.boardify_service.common.event.*;
import com.boardify.boardify_service.common.kafka.EventPublisher;
import com.boardify.boardify_service.common.kafka.Topics;
import com.boardify.boardify_service.board.repository.BoardRepository;
import com.boardify.boardify_service.exception.BoardNotFoundException;
import com.boardify.boardify_service.exception.UnauthorizedException;
import com.boardify.boardify_service.exception.UserNotFoundException;
import com.boardify.boardify_service.user.repository.UserRepository;
import com.boardify.boardify_service.user.entity.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

@Service
public class BoardService {
    private final BoardRepository boards; private final UserRepository users; private final EventPublisher events;

    public BoardService(BoardRepository boards, UserRepository users, EventPublisher events) {
        this.boards = boards; this.users = users; this.events = events; }

    @Transactional
    public BoardDto create(CreateBoardRequest req, UserEntity creator) {
        BoardEntity b = new BoardEntity();
        b.setName(req.getName());
        b.setCreatedBy(creator);
        if (req.getMemberEmails() != null && !req.getMemberEmails().isEmpty()) {
            var members = users.findAllByEmailIn(req.getMemberEmails()); // add this repo method
            b.getMembers().addAll(members);
        }
        BoardEntity saved = boards.save(b);

        BoardCreatedEvent ev = new BoardCreatedEvent();
        ev.boardId = saved.getId(); ev.name = saved.getName(); ev.createdBy = creator.getEmail(); ev.createdAt = Instant.now();
        ev.members = saved.getMembers().stream().map(UserEntity::getEmail).collect(Collectors.toSet());
        events.publish(Topics.BOARD_EVENTS, "board-created-"+saved.getId(), ev);

        return toDto(saved);
    }

    public List<BoardDto> listVisibleTo(String email) {
        return boards.findAllVisibleTo(email).stream().map(this::toDto).toList();
    }

    public BoardDto get(Long id, String email) {
        BoardEntity b = boards.findById(id).orElseThrow(() -> new BoardNotFoundException("Board not found"));
        boolean allowed = b.getCreatedBy().getEmail().equals(email) || b.getMembers().stream().anyMatch(u -> u.getEmail().equals(email));
        if (!allowed) throw new UnauthorizedException("Forbidden");
        return toDto(b);
    }

    @Transactional
    public BoardDto update(Long id, UpdateBoardRequest req, String email) {
        BoardEntity b = boards.findById(id).orElseThrow(() -> new BoardNotFoundException("Board not found"));
        // Owner or member can rename; tune to your RBAC
        boolean allowed = b.getCreatedBy().getEmail().equals(email) || b.getMembers().stream().anyMatch(u -> u.getEmail().equals(email));
        if (!allowed) throw new UnauthorizedException("Forbidden");
        b.setName(req.getName());
        BoardEntity saved = boards.save(b);

        BoardUpdatedEvent ev = new BoardUpdatedEvent(); ev.boardId = saved.getId(); ev.name = saved.getName();
        events.publish(Topics.BOARD_EVENTS, "board-updated-"+saved.getId(), ev);
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id, String email) {
        BoardEntity b = boards.findById(id).orElseThrow(() -> new BoardNotFoundException("Board not found"));
        if (!b.getCreatedBy().getEmail().equals(email)) throw new UnauthorizedException("Only owner can delete board");
        boards.delete(b);
        BoardDeletedEvent ev = new BoardDeletedEvent(); ev.boardId = id;
        events.publish(Topics.BOARD_EVENTS, "board-deleted-"+id, ev);
    }

    @Transactional
    public void addMember(Long id, String memberEmail, String actingUserEmail) {
        BoardEntity b = boards.findById(id).orElseThrow(() -> new BoardNotFoundException("Board not found"));
        if (!b.getCreatedBy().getEmail().equals(actingUserEmail)) throw new UnauthorizedException("Only owner can add members");
        UserEntity u = users.findByEmail(memberEmail).orElseThrow(() -> new UserNotFoundException("User not found"));
        b.getMembers().add(u); boards.save(b);
        MemberAddedToBoardEvent ev = new MemberAddedToBoardEvent(); ev.boardId = id; ev.memberEmail = memberEmail;
        events.publish(Topics.BOARD_EVENTS, "board-member-added-"+id+"-"+memberEmail, ev);
    }

    @Transactional
    public void removeMember(Long id, String memberEmail, String actingUserEmail) {
        BoardEntity b = boards.findById(id)
                .orElseThrow(() -> new BoardNotFoundException("Board not found"));

        if (!b.getCreatedBy().getEmail().equals(actingUserEmail)) {
            throw new UnauthorizedException("Only owner can remove members");
        }

        boolean isMember = b.getMembers().stream()
                .anyMatch(user -> user.getEmail().equals(memberEmail));
                
        if (!isMember) {
            throw new UserNotFoundException("User is not a member of this board");
        }

        b.getMembers().removeIf(user -> user.getEmail().equals(memberEmail));
        boards.save(b);

        MemberRemovedFromBoardEvent ev = new MemberRemovedFromBoardEvent(id, memberEmail, Instant.now());
        events.publish(Topics.BOARD_EVENTS, "board-member-removed-" + id + "-" + memberEmail, ev);
    }


    private BoardDto toDto(BoardEntity b) {
        Set<String> memberEmails = b.getMembers().stream().map(UserEntity::getEmail).collect(Collectors.toSet());
        return new BoardDto(b.getId(), b.getName(), b.getCreatedBy().getEmail(), b.getCreatedAt(), memberEmails);
    }
}
