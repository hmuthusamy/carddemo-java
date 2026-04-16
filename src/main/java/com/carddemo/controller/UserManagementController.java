package com.carddemo.controller;

import com.carddemo.model.UserData;
import com.carddemo.service.UserAddService;
import com.carddemo.service.UserAlreadyExistsException;
import com.carddemo.service.UserManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

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
/**
 * UserManagementController – Spring Boot REST controller migrated from the
 * COBOL/CICS COUSR00C and COUSR01C program families (CU00/CU01 transactions,
 * USRSEC VSAM file).
 *
 * <h2>COBOL → REST Mapping</h2>
 * <table border="1">
 *   <tr><th>COBOL Program / Action</th><th>REST Endpoint</th></tr>
 *   <tr><td>COUSR00C – list users (STARTBR/READNEXT, 10-row page)</td>
 *       <td>GET  /api/users?page=0&amp;size=10</td></tr>
 *   <tr><td>COUSR00C – select single user (RIDFLD READ)</td>
 *       <td>GET  /api/users/{userId}</td></tr>
 *   <tr><td>COUSR01C – add new user (EXEC CICS WRITE) [COUSR01C migration]</td>
 *       <td>POST /api/users  → delegates to {@link UserAddService}</td></tr>
 *   <tr><td>COUSR02C – update user (EXEC CICS REWRITE, sel flag 'U')</td>
 *       <td>PUT  /api/users/{userId}</td></tr>
 *   <tr><td>COUSR03C – delete user (EXEC CICS DELETE, sel flag 'D')</td>
 *       <td>DELETE /api/users/{userId}</td></tr>
 * </table>
 *
 * <p>All operations require {@code ROLE_ADMIN}, mirroring the original COBOL
 * CU00/CU01 transactions that were restricted to admin terminal users.
 *
 * <p>Migrated from: COUSR00C.CBL, COUSR01C.CBL, COUSR02C.CBL, COUSR03C.CBL
 * (CardDemo v1.0-15-g27d6c6f, 2022-07-19)
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserManagementService userManagementService;

    /** COUSR01C-specific service: all PROCESS-ENTER-KEY validations + WRITE logic. */
    private final UserAddService userAddService;

    public UserManagementController(UserManagementService userManagementService,
                                    UserAddService userAddService) {
        this.userManagementService = userManagementService;
        this.userAddService        = userAddService;
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
     * <p>Delegates to {@link UserAddService#addUser} which faithfully reproduces
     * every validation rule from COUSR01C paragraph {@code PROCESS-ENTER-KEY}
     * and the duplicate-key logic from {@code WRITE-USER-SEC-FILE}.
     *
     * <p>HTTP status mapping:
     * <ul>
     *   <li>201 Created   – COBOL DFHRESP(NORMAL) → "User &lt;id&gt; has been added ..."</li>
     *   <li>400 Bad Request – blank field validation (COBOL "…can NOT be empty…")</li>
     *   <li>409 Conflict  – COBOL DFHRESP(DUPKEY/DUPREC) → "User ID already exist..."</li>
     * </ul>
     *
     * @param request validated request body (BMS map field equivalents)
     * @return 201 Created with the persisted user (password not returned)
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(
            @Valid @RequestBody AddUserRequest request) {

        UserData saved = userAddService.addUser(
                request.userId(),
                request.firstName(),
                request.lastName(),
                request.password(),
                request.userType()
        );

        // COBOL success message: "User <id> has been added ..."
        Map<String, Object> body = Map.of(
                "message",   "User " + saved.getUserId() + " has been added ...",
                "userId",    saved.getUserId(),
                "firstName", saved.getFirstName(),
                "lastName",  saved.getLastName(),
                "userType",  saved.getUserType()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
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
    // Exception handlers (COUSR01C-specific)
    // -----------------------------------------------------------------------

    /**
     * COBOL: DFHRESP(DUPKEY) / DFHRESP(DUPREC)
     * → "User ID already exist..." → HTTP 409 Conflict
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(
            UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "DUPLICATE_USER", "message", ex.getMessage()));
    }

    /**
     * COBOL: blank-field EVALUATE WHEN checks in PROCESS-ENTER-KEY
     * → HTTP 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "VALIDATION_ERROR", "message", ex.getMessage()));
    }

    /** Bean-validation constraint failures → HTTP 400 Bad Request */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleBeanValidation(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "VALIDATION_ERROR", "message", message));
    }

    // -----------------------------------------------------------------------
    // Inner DTO – AddUserRequest  (COUSR01C BMS map fields)
    // -----------------------------------------------------------------------

    /**
     * Request body for POST /api/users (COUSR01C – transaction CU01).
     *
     * <p>Mirrors the COUSR01C BMS map input fields (copybook COUSR01):
     * <pre>
     *   USERIDI  → userId    (SEC-USR-ID    PIC X(08))
     *   FNAMEI   → firstName (SEC-USR-FNAME PIC X(20))
     *   LNAMEI   → lastName  (SEC-USR-LNAME PIC X(20))
     *   PASSWDI  → password  (SEC-USR-PWD   PIC X(08), BCrypt-encoded on write)
     *   USRTYPEI → userType  (SEC-USR-TYPE  PIC X(01): 'R'=Regular / 'A'=Admin)
     * </pre>
     */
    public record AddUserRequest(
            @NotBlank(message = "User ID can NOT be empty...")
            @Size(max = 8, message = "User ID must not exceed 8 characters (SEC-USR-ID PIC X(08))")
            String userId,

            @NotBlank(message = "First Name can NOT be empty...")
            @Size(max = 20, message = "First name must not exceed 20 characters (SEC-USR-FNAME PIC X(20))")
            String firstName,

            @NotBlank(message = "Last Name can NOT be empty...")
            @Size(max = 20, message = "Last name must not exceed 20 characters (SEC-USR-LNAME PIC X(20))")
            String lastName,

            @NotBlank(message = "Password can NOT be empty...")
            String password,

            @NotBlank(message = "User Type can NOT be empty...")
            @Pattern(regexp = "[RAra]",
                     message = "User Type must be 'R' (Regular) or 'A' (Admin)")
            String userType
    ) {}
}
