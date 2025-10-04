package com.boardify.boardify_service.list.controller;

import com.boardify.boardify_service.list.dto.*;
import com.boardify.boardify_service.list.service.BoardListService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/lists")
public class BoardListController {
    private final BoardListService service;
    
    public BoardListController(BoardListService service) { 
        this.service = service; 
    }

    /**
     * Creates a new list.
     *
     * @param principal The authenticated user making the request.
     * @param boardId The ID of the board to which the list belongs.
     * @param req The request body containing the details of the list to create.
     * @return A ResponseEntity containing the created ListDto.
     */
    @PostMapping
    public ResponseEntity<ListDto> create(
            @AuthenticationPrincipal User principal, 
            @PathVariable Long boardId, 
            @Valid @RequestBody CreateListRequest req) {
        return ResponseEntity.ok(service.create(boardId, req, principal.getUsername()));
    }

    /**
     * Retrieves a list of all lists belonging to a specific board.
     *
     * @param principal The authenticated user making the request.
     * @param boardId The ID of the board to which the lists belong.
     * @return A ResponseEntity containing a list of ListDto objects, representing the lists.
     */
    @GetMapping
    public ResponseEntity<List<ListDto>> get(
            @AuthenticationPrincipal User principal, 
            @PathVariable Long boardId) {
        return ResponseEntity.ok(service.getByBoard(boardId, principal.getUsername()));
    }

    /**
     * Updates a list.
     *
     * @param principal The authenticated user making the request.
     * @param boardId The ID of the board to which the list belongs.
     * @param listId The ID of the list to update.
     * @param req The request body containing the details of the list to update.
     * @return A ResponseEntity containing the updated ListDto.
     */
    @PutMapping("/{listId}")
    public ResponseEntity<ListDto> updateList(
            @AuthenticationPrincipal User principal,
            @PathVariable Long boardId,
            @PathVariable Long listId,
            @Valid @RequestBody UpdateListRequest req) {
        return ResponseEntity.ok(service.update(listId, req));
    }
    /**
     * Deletes a list.
     *
     * @param principal The authenticated user making the request.
     * @param boardId The ID of the board to which the list belongs.
     * @param listId The ID of the list to delete.
     * @return A ResponseEntity containing no content if the list is deleted successfully.
     */
    @DeleteMapping("/{listId}")
    public ResponseEntity<Void> deleteList(
            @AuthenticationPrincipal User principal,
            @PathVariable Long boardId,
            @PathVariable Long listId) {
        service.delete(listId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reorders a list to a new position
     *
     * @param principal The authenticated user
     * @param boardId The ID of the board containing the list
     * @param listId The ID of the list to reorder
     * @param req Contains the target index for the list
     * @return 204 No Content on success
     */
    @PatchMapping("/{listId}/reorder")
    public ResponseEntity<Void> reorderList(
            @AuthenticationPrincipal User principal,
            @PathVariable Long boardId,
            @PathVariable Long listId,
            @Valid @RequestBody ReorderListRequest req) {
        service.reorder(listId, req.getTargetIndex());
        return ResponseEntity.noContent().build();
    }
}
