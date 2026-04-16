package com.carddemo.service;

import com.carddemo.dto.UserDeleteResponse;
import com.carddemo.exception.UserDeleteNotAllowedException;
import com.carddemo.exception.UserNotFoundException;
import com.carddemo.model.User;
import com.carddemo.model.UserStatus;
import com.carddemo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Service layer migrated from COBOL/CICS program {@code COUSR03C}.
 *
 * <h2>Original COBOL program: COUSR03C.CBL</h2>
 * <p>The COBOL program performs the following key steps:
 * <ol>
 *   <li>Validates that the user ID field is not empty (PROCESS-ENTER-KEY /
 *       DELETE-USER-INFO paragraphs).</li>
 *   <li>Reads the USRSEC VSAM file with {@code EXEC CICS READ … UPDATE} to
 *       verify the user exists (READ-USER-SEC-FILE paragraph).</li>
 *   <li>Executes {@code EXEC CICS DELETE DATASET(WS-USRSEC-FILE)} to physically
 *       remove the record (DELETE-USER-SEC-FILE paragraph).</li>
 *   <li>Returns success message: "User &lt;id&gt; has been deleted ..."</li>
 *   <li>On NOTFND: returns "User ID NOT found..."</li>
 * </ol>
 *
 * <h2>Migration decisions and deviations</h2>
 * <dl>
 *   <dt><strong>Soft delete vs. physical delete</strong></dt>
 *   <dd>The original COBOL executes a physical CICS DELETE on the VSAM file,
 *       permanently removing the record.  Per the migration specification, this
 *       service performs a <em>soft delete</em>: the user record is retained in
 *       the database but its {@code status} is set to {@link UserStatus#INACTIVE}
 *       and a {@code deletedAt} timestamp is recorded.  This preserves audit
 *       history and supports account recovery without altering the observable
 *       API contract (HTTP 200 / success message identical to COBOL output).</dd>
 *
 *   <dt><strong>Last-admin guard</strong></dt>
 *   <dd>In the original mainframe environment, deleting the final admin user
 *       was prevented implicitly by RACF security rules at the platform level.
 *       Because no equivalent platform guard exists in Spring Boot, this service
 *       makes that constraint explicit: attempting to soft-delete the last active
 *       admin throws {@link UserDeleteNotAllowedException} (HTTP 409 Conflict).
 *       This preserves the original system's safety property.</dd>
 *
 *   <dt><strong>Transaction handling</strong></dt>
 *   <dd>The CICS UPDATE lock acquired by the COBOL READ is equivalent to the
 *       {@link Transactional} annotation here: both ensure the read and the
 *       write/delete happen atomically.</dd>
 *
 *   <dt><strong>User-ID validation</strong></dt>
 *   <dd>The COBOL guards against empty/spaces user ID ("User ID can NOT be
 *       empty...").  Here {@link #validateUserId(String)} enforces the same
 *       constraint with {@link IllegalArgumentException}.</dd>
 * </dl>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeleteService {

    /** COBOL constant: user type code for administrator. */
    static final String ADMIN_USER_TYPE = "A";

    private final UserRepository userRepository;

    /**
     * Looks up a user by ID and returns their details for confirmation display.
     *
     * <p>COBOL equivalent: {@code PROCESS-ENTER-KEY} paragraph — reads the USRSEC
     * file and sends the delete-confirmation screen with user details populated.
     *
     * @param userId the user's ID (max 8 chars, maps to COBOL SEC-USR-ID)
     * @return the {@link User} entity for display purposes
     * @throws IllegalArgumentException if {@code userId} is blank
     * @throws UserNotFoundException    if no active user with that ID exists
     *                                  (COBOL DFHRESP(NOTFND) → "User ID NOT found...")
     */
    @Transactional(readOnly = true)
    public User lookupUser(String userId) {
        validateUserId(userId);
        log.debug("COUSR03C/PROCESS-ENTER-KEY: looking up user '{}'", userId);
        return findActiveUser(userId);
    }

    /**
     * Soft-deletes (deactivates) the user identified by {@code userId}.
     *
     * <p><strong>COBOL equivalent:</strong> {@code DELETE-USER-INFO} →
     * {@code READ-USER-SEC-FILE} → {@code DELETE-USER-SEC-FILE} paragraphs.
     *
     * <p><strong>Steps (mirroring COBOL flow):</strong>
     * <ol>
     *   <li>Validate userId is not blank (COBOL: "User ID can NOT be empty...").</li>
     *   <li>Read user from USRSEC — throws {@link UserNotFoundException} on NOTFND.</li>
     *   <li>Guard against deleting the last active admin (RACF implicit protection).</li>
     *   <li>Set {@code status = INACTIVE} and {@code deletedAt = now()} (replaces
     *       physical CICS DELETE).</li>
     *   <li>Save and return success response matching COBOL message format.</li>
     * </ol>
     *
     * @param userId the user's ID to deactivate
     * @return {@link UserDeleteResponse} with message "User &lt;id&gt; has been deleted ..."
     * @throws IllegalArgumentException       if {@code userId} is blank
     * @throws UserNotFoundException          if user does not exist or is already inactive
     * @throws UserDeleteNotAllowedException  if deleting this user would leave no active admins
     */
    @Transactional
    public UserDeleteResponse deleteUser(String userId) {
        validateUserId(userId);
        log.info("COUSR03C/DELETE-USER-INFO: request to delete user '{}'", userId);

        // Step 1: READ-USER-SEC-FILE equivalent — verify existence (UPDATE lock → @Transactional)
        User user = findActiveUser(userId);

        // Step 2: Last-admin cascade guard (RACF implicit rule made explicit in Java)
        if (ADMIN_USER_TYPE.equals(user.getUserType())) {
            long activeAdminCount = userRepository.countByUserTypeAndStatus(
                    ADMIN_USER_TYPE, UserStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                log.warn("COUSR03C: rejected delete of '{}' — last active admin", userId);
                throw new UserDeleteNotAllowedException(
                        "Cannot delete user '" + userId.trim() + "': they are the last active administrator. "
                        + "Assign admin privileges to another user first.");
            }
        }

        // Step 3: DELETE-USER-SEC-FILE equivalent — soft delete (status=INACTIVE + audit ts)
        user.setStatus(UserStatus.INACTIVE);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("COUSR03C/DELETE-USER-SEC-FILE: user '{}' soft-deleted successfully", userId);
        return UserDeleteResponse.success(userId);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Validates that the user ID is not blank.
     *
     * <p>COBOL guard:
     * <pre>
     *   WHEN USRIDINI OF COUSR3AI = SPACES OR LOW-VALUES
     *       MOVE 'User ID can NOT be empty...' TO WS-MESSAGE
     * </pre>
     */
    private void validateUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("User ID can NOT be empty...");
        }
    }

    /**
     * Finds a user that currently has ACTIVE status.
     *
     * <p>COBOL: DFHRESP(NOTFND) → "User ID NOT found..."
     */
    private User findActiveUser(String userId) {
        User user = userRepository.findById(userId.trim())
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (UserStatus.INACTIVE == user.getStatus()) {
            // Already deleted — treat as not found (matches COBOL VSAM behaviour
            // where a physically deleted record cannot be re-read).
            throw new UserNotFoundException(userId);
        }
        return user;
    }
}
