package com.boardify.boardify_service.list.service;

import com.boardify.boardify_service.board.BoardEntity;
import com.boardify.boardify_service.common.event.ListCreatedEvent;
import com.boardify.boardify_service.common.event.ListDeletedEvent;
import com.boardify.boardify_service.common.event.ListReorderedEvent;
import com.boardify.boardify_service.common.event.ListUpdatedEvent;
import com.boardify.boardify_service.common.kafka.EventPublisher;
import com.boardify.boardify_service.common.kafka.Topics;
import com.boardify.boardify_service.list.dto.CreateListRequest;
import com.boardify.boardify_service.list.dto.ListDto;
import com.boardify.boardify_service.list.dto.UpdateListRequest;
import com.boardify.boardify_service.list.entity.BoardList;
import com.boardify.boardify_service.repository.BoardListRepository;
import com.boardify.boardify_service.repository.BoardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BoardListService {
    private final BoardListRepository lists; private final BoardRepository boards; private final EventPublisher events;

    public BoardListService(BoardListRepository lists, BoardRepository boards, EventPublisher events) {
        this.lists = lists; this.boards = boards; this.events = events; }

    @Transactional
    public ListDto create(Long boardId, CreateListRequest req, String actingEmail) {
        BoardEntity board = boards.findById(boardId).orElseThrow(() -> new RuntimeException("Board not found"));
        // access check can be added here
        int nextPos = (int) lists.countByBoard(board);
        BoardList entity = new BoardList(); entity.setBoard(board); entity.setName(req.getName()); entity.setPosition(nextPos);
        BoardList saved = lists.save(entity);

        ListCreatedEvent ev = new ListCreatedEvent(); ev.boardId = boardId; ev.listId = saved.getId(); ev.name = saved.getName(); ev.position = saved.getPosition();
        events.publish(Topics.LIST_EVENTS, "list-created-"+saved.getId(), ev);
        return toDto(saved);
    }

    public List<ListDto> getByBoard(Long boardId, String email) {
        BoardEntity board = boards.findById(boardId).orElseThrow(() -> new RuntimeException("Board not found"));
        return lists.findByBoardOrderByPositionAsc(board).stream().map(this::toDto).toList();
    }

    @Transactional
    public ListDto update(Long listId, UpdateListRequest req) {
        BoardList l = lists.findById(listId).orElseThrow(() -> new RuntimeException("List not found"));
        if (req.getName() != null) l.setName(req.getName());
        if (req.getPosition() != null) l.setPosition(req.getPosition());
        BoardList saved = lists.save(l);
        ListUpdatedEvent ev = new ListUpdatedEvent(); ev.listId = saved.getId(); ev.name = saved.getName(); ev.position = saved.getPosition();
        events.publish(Topics.LIST_EVENTS, "list-updated-"+saved.getId(), ev);
        return toDto(saved);
    }

    @Transactional
    public void delete(Long listId) {
        BoardList l = lists.findById(listId).orElseThrow(() -> new RuntimeException("List not found"));
        Long boardId = l.getBoard().getId();
        lists.delete(l);
        ListDeletedEvent ev = new ListDeletedEvent(); ev.listId = listId; ev.boardId = boardId;
        events.publish(Topics.LIST_EVENTS, "list-deleted-"+listId, ev);
    }

    @Transactional
    public void reorder(Long listId, int targetIndex) {
        BoardList l = lists.findById(listId).orElseThrow(() -> new RuntimeException("List not found"));
        BoardEntity board = l.getBoard();
        List<BoardList> all = lists.findByBoardOrderByPositionAsc(board);

        all.removeIf(x -> x.getId().equals(listId));
        if (targetIndex < 0) targetIndex = 0;
        if (targetIndex > all.size()) targetIndex = all.size();
        all.add(targetIndex, l);

        for (int i = 0; i < all.size(); i++) all.get(i).setPosition(i);
        lists.saveAll(all);

        ListReorderedEvent ev = new ListReorderedEvent(); ev.boardId = board.getId(); ev.listId = listId; ev.targetIndex = targetIndex;
        events.publish(Topics.LIST_EVENTS, "list-reordered-"+listId, ev);
    }

    private ListDto toDto(BoardList l) { return new ListDto(l.getId(), l.getBoard().getId(), l.getName(), l.getPosition()); }
}
