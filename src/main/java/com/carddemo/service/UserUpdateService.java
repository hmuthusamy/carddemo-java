package com.carddemo.service;

import com.carddemo.dto.UserUpdateRequest;
import com.carddemo.dto.UserUpdateResponse;
import com.carddemo.exception.UserNotFoundException;
import com.carddemo.exception.UserValidationException;
import com.carddemo.model.UserSec;
import com.carddemo.repository.UserSecRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Boot service migrated from COBOL/CICS program COUSR02C.CBL.
 *
 * <p>COBOL → Java mapping:
 * <pre>
 *  PROCESS-ENTER-KEY   → lookupUser(userId)
 *  UPDATE-USER-INFO    → updateUser(userId, request)
 *  READ-USER-SEC-FILE  → userSecRepository.findById(userId)
 *  UPDATE-USER-SEC-FILE→ userSecRepository.save(entity)
 * </pre>
 *
 * <p>Validation rules preserved verbatim from the EVALUATE TRUE block
 * in UPDATE-USER-INFO (lines ≈ 145-183 of COUSR02C.CBL):
 * <ol>
 *   <li>userId   blank → "User ID can NOT be empty..."</li>
 *   <li>firstName blank → "First Name can NOT be empty..."</li>
 *   <li>lastName  blank → "Last Name can NOT be empty..."</li>
 *   <li>password  blank → "Password can NOT be empty..."</li>
 *   <li>userType  blank → "User Type can NOT be empty..."</li>
 * </ol>
 *
 * <p>Partial-update logic: only permitted fields (firstName, lastName,
 * password, userType) may be changed. The userId is immutable (PK).
 * If none of the four fields differs from the stored value the method
 * mirrors the COBOL "Please modify to update ..." branch and returns
 * a response with {@code success=false}.
 */
@Service
@Transactional
public class UserUpdateService {

    // ---------------------------------------------------------------
    // Validation messages – exact text from COUSR02C EVALUATE block
    // ---------------------------------------------------------------
    static final String MSG_USERID_EMPTY    = "User ID can NOT be empty...";
    static final String MSG_FNAME_EMPTY     = "First Name can NOT be empty...";
    static final String MSG_LNAME_EMPTY     = "Last Name can NOT be empty...";
    static final String MSG_PASSWORD_EMPTY  = "Password can NOT be empty...";
    static final String MSG_USERTYPE_EMPTY  = "User Type can NOT be empty...";
    static final String MSG_NOTHING_CHANGED = "Please modify to update ...";

    private final UserSecRepository userSecRepository;

    public UserUpdateService(UserSecRepository userSecRepository) {
        this.userSecRepository = userSecRepository;
    }

    // ---------------------------------------------------------------
    // PROCESS-ENTER-KEY equivalent: look up user, return current data
    // ---------------------------------------------------------------

    /**
     * Reads a user record by id – mirrors COUSR02C PROCESS-ENTER-KEY.
     *
     * @param userId the SEC-USR-ID value (up to 8 chars)
     * @return the persisted {@link UserSec} entity
     * @throws UserValidationException if userId is blank
     * @throws UserNotFoundException   if no record exists for userId
     */
    @Transactional(readOnly = true)
    public UserSec lookupUser(String userId) {
        validateUserId(userId);
        return userSecRepository.findById(userId.trim())
                .orElseThrow(() -> new UserNotFoundException(userId.trim()));
    }

    // ---------------------------------------------------------------
    // UPDATE-USER-INFO equivalent: validate + selective update + save
    // ---------------------------------------------------------------

    /**
     * Updates permitted fields on an existing user record.
     *
     * <p>Mirrors the COUSR02C UPDATE-USER-INFO paragraph including:
     * <ul>
     *   <li>Field-level blank validation (EVALUATE TRUE branches)</li>
     *   <li>Selective field comparison and flag USR-MODIFIED-YES</li>
     *   <li>CICS REWRITE only when at least one field changed</li>
     * </ul>
     *
     * @param userId  path variable – immutable primary key (SEC-USR-ID)
     * @param request DTO containing the four updatable fields
     * @return {@link UserUpdateResponse} with outcome message
     * @throws UserValidationException if any required field is blank
     * @throws UserNotFoundException   if the user does not exist
     */
    public UserUpdateResponse updateUser(String userId, UserUpdateRequest request) {

        // ── Step 1: Validate inputs (EVALUATE TRUE block) ──────────
        validateUserId(userId);
        validateFirstName(request.getFirstName());
        validateLastName(request.getLastName());
        validatePassword(request.getPassword());
        validateUserType(request.getUserType());

        // ── Step 2: READ-USER-SEC-FILE ──────────────────────────────
        UserSec existing = userSecRepository.findById(userId.trim())
                .orElseThrow(() -> new UserNotFoundException(userId.trim()));

        // ── Step 3: Selective field comparison (USR-MODIFIED flag) ──
        boolean modified = false;

        if (!request.getFirstName().trim().equals(existing.getUsrFname().trim())) {
            existing.setUsrFname(request.getFirstName().trim());
            modified = true;
        }
        if (!request.getLastName().trim().equals(existing.getUsrLname().trim())) {
            existing.setUsrLname(request.getLastName().trim());
            modified = true;
        }
        if (!request.getPassword().equals(existing.getUsrPwd())) {
            existing.setUsrPwd(request.getPassword());
            modified = true;
        }
        if (!request.getUserType().trim().equals(existing.getUsrType().trim())) {
            existing.setUsrType(request.getUserType().trim());
            modified = true;
        }

        // ── Step 4: UPDATE-USER-SEC-FILE or "nothing changed" ───────
        if (!modified) {
            // Mirrors: MOVE 'Please modify to update ...' TO WS-MESSAGE
            return new UserUpdateResponse(userId.trim(), MSG_NOTHING_CHANGED, false);
        }

        userSecRepository.save(existing);

        // Mirrors: STRING 'User ' SEC-USR-ID ' has been updated ...'
        String successMsg = "User " + userId.trim() + " has been updated ...";
        return new UserUpdateResponse(userId.trim(), successMsg, true);
    }

    // ---------------------------------------------------------------
    // Private validation helpers – each maps to one WHEN branch
    // ---------------------------------------------------------------

    private void validateUserId(String userId) {
        if (isBlank(userId)) {
            throw new UserValidationException(MSG_USERID_EMPTY);
        }
    }

    private void validateFirstName(String firstName) {
        if (isBlank(firstName)) {
            throw new UserValidationException(MSG_FNAME_EMPTY);
        }
    }

    private void validateLastName(String lastName) {
        if (isBlank(lastName)) {
            throw new UserValidationException(MSG_LNAME_EMPTY);
        }
    }

    private void validatePassword(String password) {
        if (isBlank(password)) {
            throw new UserValidationException(MSG_PASSWORD_EMPTY);
        }
    }

    private void validateUserType(String userType) {
        if (isBlank(userType)) {
            throw new UserValidationException(MSG_USERTYPE_EMPTY);
        }
    }

    /** Equivalent of COBOL "= SPACES OR LOW-VALUES" test. */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
