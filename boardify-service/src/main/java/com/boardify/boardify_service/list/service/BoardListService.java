package com.boardify.boardify_service.list.service;

import com.boardify.boardify_service.board.entity.BoardEntity;
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
import com.boardify.boardify_service.list.repository.BoardListRepository;
import com.boardify.boardify_service.board.repository.BoardRepository;
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
        BoardEntity board = boards.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));

        // only board members/owners can create lists
        boolean allowed = board.getCreatedBy().getEmail().equals(actingEmail)
                || board.getMembers().stream().anyMatch(m -> m.equals(actingEmail));
        if (!allowed) throw new RuntimeException("Forbidden");

        // calculate safe unique position
        Double maxPosition = lists.findMaxPositionByBoardId(boardId);
        Double newPosition = (maxPosition == null ? 1.0 : maxPosition + 1.0);

        BoardList newList = new BoardList();
        newList.setName(req.getName());
        newList.setBoard(board);
        newList.setPosition(newPosition);

        BoardList saved = lists.save(newList);

        // emit Kafka event
        ListCreatedEvent ev = new ListCreatedEvent();
        ev.boardId = boardId;
        ev.listId = saved.getId();
        ev.name = saved.getName();
        ev.position = saved.getPosition();
        events.publish(Topics.LIST_EVENTS, "list-created-" + saved.getId(), ev);

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
        BoardList l = lists.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));
        BoardEntity board = l.getBoard();
        List<BoardList> all = lists.findByBoardOrderByPositionAsc(board);

        // remove the moving list
        all.removeIf(x -> x.getId().equals(listId));

        // clamp targetIndex
        if (targetIndex < 0) targetIndex = 0;
        if (targetIndex > all.size()) targetIndex = all.size();

        // find neighbors at targetIndex
        Double newPos;
        if (targetIndex == 0) {
            double after = all.get(0).getPosition();
            newPos = after - 1;
        } else if (targetIndex == all.size()) {
            double before = all.get(all.size() - 1).getPosition();
            newPos = before + 1;
        } else {
            double before = all.get(targetIndex - 1).getPosition();
            double after = all.get(targetIndex).getPosition();
            newPos = (before + after) / 2;
        }

        l.setPosition(newPos);
        lists.save(l);

        // ✅ Normalization: if differences get too small, renumber all positions
        normalizePositionsIfNeeded(board);

        // publish event
        ListReorderedEvent ev = new ListReorderedEvent();
        ev.boardId = board.getId();
        ev.listId = listId;
        ev.targetIndex = targetIndex;
        events.publish(Topics.LIST_EVENTS, "list-reordered-" + listId, ev);
    }

    private void normalizePositionsIfNeeded(BoardEntity board) {
        List<BoardList> all = lists.findByBoardOrderByPositionAsc(board);
        boolean tooClose = false;

        for (int i = 1; i < all.size(); i++) {
            double diff = all.get(i).getPosition() - all.get(i - 1).getPosition();
            if (diff < 0.000001) { // threshold for "too close"
                tooClose = true;
                break;
            }
        }

        if (tooClose) {
            for (int i = 0; i < all.size(); i++) {
                all.get(i).setPosition((double) (i + 1));
            }
            lists.saveAll(all);
        }
    }


    private ListDto toDto(BoardList l) { return new ListDto(l.getId(), l.getBoard().getId(), l.getName(), l.getPosition()); }
}
