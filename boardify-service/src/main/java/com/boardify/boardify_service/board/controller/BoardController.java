package com.boardify.boardify_service.board.controller;

import com.boardify.boardify_service.board.dto.BoardDto;
import com.boardify.boardify_service.board.dto.BoardMemberRequest;
import com.boardify.boardify_service.board.dto.CreateBoardRequest;
import com.boardify.boardify_service.board.dto.UpdateBoardRequest;
import com.boardify.boardify_service.board.service.BoardService;
import com.boardify.boardify_service.common.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
public class BoardController {
    private final BoardService boards; private final CurrentUserService currentUser;

    public BoardController(BoardService boards, CurrentUserService currentUser) { this.boards = boards; this.currentUser = currentUser; }

    /**
     * Handles board-related operations such as creating, retrieving, updating, and deleting boards,
     * as well as managing board members.
     */
    @PostMapping
/**
 * Creates a new board.
 *
 * @param principal The authenticated user making the request.
 * @param req The request body containing the details of the board to be created.
 * @return A ResponseEntity containing the created BoardDto.
 */
    public ResponseEntity<BoardDto> create(@AuthenticationPrincipal User principal, @Valid @RequestBody CreateBoardRequest req) {
        var creator = currentUser.requireUser(principal);
        return ResponseEntity.ok(boards.create(req, creator));
    }

    @GetMapping
/**
 * Retrieves a list of boards visible to the authenticated user.
 *
 * @param principal The authenticated user making the request.
 * @return A ResponseEntity containing a list of BoardDto objects.
 */
    public ResponseEntity<List<BoardDto>> list(@AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(boards.listVisibleTo(principal.getUsername()));
    }

    @GetMapping("/{id}")
/**
 * Retrieves a specific board by its ID.
 *
 * @param principal The authenticated user making the request.
 * @param id The ID of the board to retrieve.
 * @return A ResponseEntity containing the requested BoardDto.
 */
    public ResponseEntity<BoardDto> get(@AuthenticationPrincipal User principal, @PathVariable Long id) {
        return ResponseEntity.ok(boards.get(id, principal.getUsername()));
    }

    @PutMapping("/{id}")
/**
 * Updates an existing board.
 *
 * @param principal The authenticated user making the request.
 * @param id The ID of the board to update.
 * @param req The request body containing the updated board details.
 * @return A ResponseEntity containing the updated BoardDto.
 */
    public ResponseEntity<BoardDto> update(@AuthenticationPrincipal User principal, @PathVariable Long id, @Valid @RequestBody UpdateBoardRequest req) {
        return ResponseEntity.ok(boards.update(id, req, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
/**
 * Deletes a specific board by its ID.
 *
 * @param principal The authenticated user making the request.
 * @param id The ID of the board to delete.
 * @return A ResponseEntity with no content.
 */
    public ResponseEntity<?> delete(@AuthenticationPrincipal User principal, @PathVariable Long id) {
        boards.delete(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
//
//    @PostMapping("/{id}/members")
///**
// * Adds a member to a specific board.
// *
// * @param principal The authenticated user making the request.
// * @param id The ID of the board.
// * @param email The email of the member to add.
// * @return A ResponseEntity indicating the operation was successful.
// */
//    public ResponseEntity<?> addMember(@AuthenticationPrincipal User principal, @PathVariable Long id, @RequestParam String email) {
//        boards.addMember(id, email, principal.getUsername());
//        return ResponseEntity.ok().build();
//    }

    /**
     * Adds a member to a specific board.
     * Only the board owner can add members. The member will receive access to the board
     * and be able to view and interact with it based on their permissions.
     *
     * @param id The unique identifier of the board to add the member to
     * @param request The request body containing the email of the member to add
     * @param actingUserEmail The email of the currently authenticated user (automatically injected)
     * @return ResponseEntity with status 200 (OK) if the member was added successfully
     * @throws RuntimeException if the board is not found, user is not authorized,
     *         or the member to be added doesn't exist
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<Void> addMember(
            @PathVariable Long id,
            @Valid @RequestBody BoardMemberRequest request,
            @AuthenticationPrincipal(expression = "username") String actingUserEmail) {
        boards.addMember(id, request.memberEmail(), actingUserEmail);
        return ResponseEntity.ok().build();
    }

    /**
     * Removes a member from a specific board.
     * Only the board owner can remove members. This operation is irreversible and will
     * immediately revoke the member's access to the board.
     *
     * @param id The unique identifier of the board to remove the member from
     * @param request The request body containing the email of the member to remove
     * @param actingUserEmail The email of the currently authenticated user (automatically injected)
     * @return ResponseEntity with status 204 (No Content) if the member was removed successfully
     * @throws RuntimeException if the board is not found, user is not authorized,
     *         or the member is not part of the board
     */
    @DeleteMapping("/{id}/members")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @Valid @RequestBody BoardMemberRequest request,
            @AuthenticationPrincipal(expression = "username") String actingUserEmail) {
        boards.removeMember(id, request.memberEmail(), actingUserEmail);
        return ResponseEntity.noContent().build();
    }

}

