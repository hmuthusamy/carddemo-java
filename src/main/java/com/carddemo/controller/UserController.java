package com.carddemo.controller;

import com.carddemo.dto.UserDeleteResponse;
import com.carddemo.model.User;
import com.carddemo.service.UserDeleteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing the user management / delete endpoints.
 *
 * <p>Provides the HTTP interface for the logic migrated from COUSR03C (delete)
 * and the user lookup (display-confirmation) flow.
 *
 * <h2>CICS → REST mapping</h2>
 * <table border="1">
 *   <tr><th>COBOL/CICS action</th><th>HTTP endpoint</th></tr>
 *   <tr><td>PROCESS-ENTER-KEY (display user for confirmation)</td>
 *       <td>GET /api/users/{userId}</td></tr>
 *   <tr><td>DELETE-USER-INFO / DELETE-USER-SEC-FILE (PF5 key)</td>
 *       <td>DELETE /api/users/{userId}</td></tr>
 * </table>
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserDeleteService userDeleteService;

    /**
     * Retrieves user details for the delete-confirmation screen.
     *
     * <p>COBOL equivalent: {@code PROCESS-ENTER-KEY} — reads user and sends
     * the COUSR3A screen populated with first name, last name, and user type.
     *
     * @param userId path variable matching COBOL SEC-USR-ID (max 8 chars)
     * @return 200 OK with user data, or 404 if not found
     */
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable String userId) {
        log.debug("GET /api/users/{} — COUSR03C PROCESS-ENTER-KEY equivalent", userId);
        User user = userDeleteService.lookupUser(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Soft-deletes (deactivates) the specified user.
     *
     * <p>COBOL equivalent: PF5 key → {@code DELETE-USER-INFO} →
     * {@code DELETE-USER-SEC-FILE} paragraphs in COUSR03C.
     *
     * <p>Returns HTTP 200 with a success message mirroring the COBOL output:
     * <em>"User &lt;id&gt; has been deleted ..."</em>
     *
     * @param userId path variable — the user to delete
     * @return 200 OK with {@link UserDeleteResponse}, 404 if not found,
     *         409 Conflict if the user is the last active admin
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<UserDeleteResponse> deleteUser(@PathVariable String userId) {
        log.info("DELETE /api/users/{} — COUSR03C DELETE-USER-INFO equivalent", userId);
        UserDeleteResponse response = userDeleteService.deleteUser(userId);
        return ResponseEntity.ok(response);
    }
}
