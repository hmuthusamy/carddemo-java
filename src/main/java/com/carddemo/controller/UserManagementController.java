package com.carddemo.controller;

import com.carddemo.model.UserData;
import com.carddemo.service.UserManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

/**
 * UserManagementController – Spring Boot REST controller migrated from the
 * COBOL/CICS COUSR00C program family (CU00 transaction, USRSEC VSAM file).
 *
 * <h2>COBOL → REST Mapping</h2>
 * <table border="1">
 *   <tr><th>COBOL Program / Action</th><th>REST Endpoint</th></tr>
 *   <tr><td>COUSR00C – list users (STARTBR/READNEXT, 10-row page)</td>
 *       <td>GET  /api/users?page=0&amp;size=10</td></tr>
 *   <tr><td>COUSR00C – select single user (RIDFLD READ)</td>
 *       <td>GET  /api/users/{userId}</td></tr>
 *   <tr><td>COUSR01C – add new user (EXEC CICS WRITE)</td>
 *       <td>POST /api/users</td></tr>
 *   <tr><td>COUSR02C – update user (EXEC CICS REWRITE, sel flag 'U')</td>
 *       <td>PUT  /api/users/{userId}</td></tr>
 *   <tr><td>COUSR03C – delete user (EXEC CICS DELETE, sel flag 'D')</td>
 *       <td>DELETE /api/users/{userId}</td></tr>
 * </table>
 *
 * <p>All operations require {@code ROLE_ADMIN}, mirroring the original COBOL
 * CU00 transaction that was restricted to admin terminal users.
 *
 * <p>Migrated from: COUSR00C.CBL, COUSR01C.CBL, COUSR02C.CBL, COUSR03C.CBL
 * (CardDemo v1.0-15-g27d6c6f, 2022-07-19)
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    // -----------------------------------------------------------------------
    // GET /api/users  →  COUSR00C PROCESS-PAGE-FORWARD (STARTBR/READNEXT)
    // -----------------------------------------------------------------------

    /**
     * Lists active users with pagination.
     *
     * <p>Replaces the COBOL STARTBR/READNEXT/ENDBR browse loop that displayed
     * 10 records per CICS screen (WS-IDX 1 to 10).  PF7 (back) / PF8 (forward)
     * paging is now handled via {@code page} and {@code size} query params.
     *
     * @param page zero-based page number (default 0; was CDEMO-CU00-PAGE-NUM)
     * @param size records per page     (default 10; was WS-IDX upper bound)
     * @return paginated list of UserData
     */
    @GetMapping
    public ResponseEntity<Page<UserData>> listUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<UserData> users = userManagementService.listUsers(page, size);
        return ResponseEntity.ok(users);
    }

    // -----------------------------------------------------------------------
    // GET /api/users/{userId}  →  EXEC CICS READ RIDFLD(SEC-USR-ID)
    // -----------------------------------------------------------------------

    /**
     * Retrieves a single active user by ID.
     *
     * <p>Replaces the COBOL READ of the USRSEC file keyed on SEC-USR-ID.
     * Returns 404 when COBOL would have returned RESP(NOTFND).
     *
     * @param userId 8-char user identifier (SEC-USR-ID)
     * @return UserData or 404
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserData> getUserById(@PathVariable String userId) {
        return userManagementService.getUserById(userId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found: " + userId));
    }

    // -----------------------------------------------------------------------
    // POST /api/users  →  COUSR01C – EXEC CICS WRITE DATASET('USRSEC')
    // -----------------------------------------------------------------------

    /**
     * Creates a new user account.
     *
     * <p>Replaces COUSR01C which wrote a new record to the USRSEC VSAM file.
     * Returns 409 Conflict if the userId already exists (was COBOL RESP=DUPREC).
     *
     * <p>Request body includes a plain-text {@code password} field that is
     * BCrypt-encoded before storage (COBOL stored plaintext in SEC-USR-PWD PIC X(08)).
     *
     * @param request CreateUserRequest carrying user fields + plain password
     * @return 201 Created with the saved UserData
     */
    @PostMapping
    public ResponseEntity<UserData> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        try {
            UserData user = new UserData();
            user.setUserId(request.userId());
            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());
            user.setUserType(request.userType());

            UserData saved = userManagementService.createUser(user, request.password());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // PUT /api/users/{userId}  →  COUSR02C – EXEC CICS REWRITE DATASET('USRSEC')
    // -----------------------------------------------------------------------

    /**
     * Updates an existing user's details.
     *
     * <p>Replaces COUSR02C which rewrote the USRSEC VSAM record.  In COUSR00C
     * this was triggered by entering selection flag 'U' next to a listed user.
     * Returns 404 when the user does not exist or is inactive.
     *
     * @param userId  path variable matching SEC-USR-ID
     * @param updates UserData with new field values (null fields are ignored)
     * @return updated UserData
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserData> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserData updates) {

        try {
            UserData updated = userManagementService.updateUser(userId, updates);
            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // DELETE /api/users/{userId}  →  COUSR03C – EXEC CICS DELETE DATASET('USRSEC')
    // -----------------------------------------------------------------------

    /**
     * Deactivates (soft-deletes) a user.
     *
     * <p>In COBOL, COUSR03C performed a physical EXEC CICS DELETE on the VSAM
     * USRSEC file when the operator entered selection flag 'D' beside a user.
     * This REST endpoint performs a logical delete (sets {@code active = false})
     * to preserve audit history – a modernisation improvement.
     *
     * @param userId SEC-USR-ID to deactivate
     * @return 204 No Content on success, 404 if not found
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deactivateUser(@PathVariable String userId) {
        try {
            userManagementService.deactivateUser(userId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Inner DTO – CreateUserRequest
    // -----------------------------------------------------------------------

    /**
     * Request body for POST /api/users.
     * Separates the plain-text password field from the UserData entity so that
     * passwords are never serialised back in responses.
     *
     * <p>Mirrors COBOL COUSR01C input fields:
     * <pre>
     *   SEC-USR-ID    → userId    (PIC X(08))
     *   SEC-USR-FNAME → firstName (PIC X(20))
     *   SEC-USR-LNAME → lastName  (PIC X(20))
     *   SEC-USR-PWD   → password  (PIC X(08), stored BCrypt-hashed)
     *   SEC-USR-TYPE  → userType  (PIC X(01): 'A' or 'U')
     * </pre>
     */
    public record CreateUserRequest(
            @NotBlank String userId,
            String firstName,
            String lastName,
            @NotBlank String password,
            @NotBlank String userType
    ) {}
}
