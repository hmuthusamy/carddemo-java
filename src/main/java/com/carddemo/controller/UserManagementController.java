package com.carddemo.controller;

import com.carddemo.dto.UserUpdateRequest;
import com.carddemo.dto.UserUpdateResponse;
import com.carddemo.exception.UserNotFoundException;
import com.carddemo.exception.UserValidationException;
import com.carddemo.model.UserSec;
import com.carddemo.service.UserUpdateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user management operations.
 *
 * <p>Migrated from CICS programs COUSR02C (update) and provides
 * a read endpoint equivalent to COUSR02C PROCESS-ENTER-KEY.
 *
 * <pre>
 *  GET  /api/users/{userId}  →  lookup (PROCESS-ENTER-KEY)
 *  PUT  /api/users/{userId}  →  update (UPDATE-USER-INFO)
 * </pre>
 */
@RestController
@RequestMapping("/api/users")
public class UserManagementController {

    private final UserUpdateService userUpdateService;

    public UserManagementController(UserUpdateService userUpdateService) {
        this.userUpdateService = userUpdateService;
    }

    /**
     * Retrieves current user data – mirrors COUSR02C PROCESS-ENTER-KEY.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserSec> getUser(@PathVariable String userId) {
        UserSec user = userUpdateService.lookupUser(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Updates user fields – delegates to {@link UserUpdateService#updateUser}.
     * Maps to COUSR02C UPDATE-USER-INFO paragraph triggered by PF5/PF3.
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserUpdateResponse> updateUser(
            @PathVariable String userId,
            @RequestBody UserUpdateRequest request) {

        UserUpdateResponse response = userUpdateService.updateUser(userId, request);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(response);
    }

    // ---------------------------------------------------------------
    // Exception handlers – translate service exceptions to HTTP codes
    // ---------------------------------------------------------------

    @ExceptionHandler(UserValidationException.class)
    public ResponseEntity<UserUpdateResponse> handleValidation(UserValidationException ex) {
        return ResponseEntity.badRequest()
                .body(new UserUpdateResponse(null, ex.getMessage(), false));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<UserUpdateResponse> handleNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new UserUpdateResponse(null, ex.getMessage(), false));
    }
}
