package com.carddemo.service;

import com.carddemo.model.UserData;
import com.carddemo.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UserManagementService – business logic layer migrated from the COBOL CICS
 * COUSR00C / COUSR01C / COUSR02C / COUSR03C program family.
 *
 * <h2>COBOL-to-Java Mapping</h2>
 * <table border="1">
 *   <tr><th>COBOL Para/Function</th><th>Java Method</th></tr>
 *   <tr><td>PROCESS-PAGE-FORWARD (STARTBR/READNEXT, 10-rec window)</td>
 *       <td>{@link #listUsers(int, int)}</td></tr>
 *   <tr><td>EXEC CICS READ DATASET('USRSEC') RIDFLD(SEC-USR-ID)</td>
 *       <td>{@link #getUserById(String)}</td></tr>
 *   <tr><td>COUSR01C – EXEC CICS WRITE DATASET('USRSEC')</td>
 *       <td>{@link #createUser(UserData, String)}</td></tr>
 *   <tr><td>COUSR02C – EXEC CICS REWRITE DATASET('USRSEC')</td>
 *       <td>{@link #updateUser(String, UserData)}</td></tr>
 *   <tr><td>COUSR03C – EXEC CICS DELETE DATASET('USRSEC')</td>
 *       <td>{@link #deactivateUser(String)}</td></tr>
 * </table>
 *
 * <p>The COBOL user type codes are preserved:
 * <ul>
 *   <li>'A' – Administrator (was privileged CICS user with CU00 transid access)
 *   <li>'U' – Regular user
 * </ul>
 */
@Service
@Transactional
public class UserManagementService {

    /** Default page size mirrors the COBOL 10-row browse window (WS-IDX 1–10). */
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // -----------------------------------------------------------------------
    // LIST  (COUSR00C PROCESS-PAGE-FORWARD / PROCESS-PAGE-BACKWARD)
    // -----------------------------------------------------------------------

    /**
     * Returns a paginated list of active users sorted by userId ascending,
     * mirroring the VSAM STARTBR/READNEXT browse keyed on SEC-USR-ID.
     *
     * @param page zero-based page index (replaces CDEMO-CU00-PAGE-NUM)
     * @param size records per page (default matches WS-IDX 1..10 window)
     * @return Page of active UserData
     */
    @Transactional(readOnly = true)
    public Page<UserData> listUsers(int page, int size) {
        int effectiveSize = (size > 0) ? size : DEFAULT_PAGE_SIZE;
        Pageable pageable = PageRequest.of(page, effectiveSize,
                                           Sort.by("userId").ascending());
        return userRepository.findAllByActiveTrue(pageable);
    }

    /**
     * Returns all active users without pagination (admin export use-case).
     */
    @Transactional(readOnly = true)
    public List<UserData> listAllUsers() {
        return userRepository.findAllByActiveTrue();
    }

    // -----------------------------------------------------------------------
    // GET BY ID  (EXEC CICS READ DATASET('USRSEC') RIDFLD(SEC-USR-ID))
    // -----------------------------------------------------------------------

    /**
     * Retrieves a single active user by ID.
     * Returns {@link Optional#empty()} when COBOL would have returned NOTFND.
     *
     * @param userId 8-char SEC-USR-ID value
     * @return Optional UserData
     */
    @Transactional(readOnly = true)
    public Optional<UserData> getUserById(String userId) {
        return userRepository.findByUserIdAndActiveTrue(normaliseId(userId));
    }

    // -----------------------------------------------------------------------
    // CREATE  (COUSR01C – EXEC CICS WRITE DATASET('USRSEC'))
    // -----------------------------------------------------------------------

    /**
     * Creates a new user record.
     * Corresponds to COUSR01C EXEC CICS WRITE; throws if the userId already
     * exists (COBOL RESP=DUPREC handling).
     *
     * @param user      UserData with userId, names and userType populated
     * @param plainPwd  plain-text password; BCrypt-encoded before persistence
     * @return the saved UserData
     * @throws IllegalArgumentException if userId already exists
     */
    public UserData createUser(UserData user, String plainPwd) {
        String id = normaliseId(user.getUserId());
        if (userRepository.existsByUserId(id)) {
            throw new IllegalArgumentException(
                    "User ID already exists: " + id);   // maps to COBOL DUPREC error msg
        }
        user.setUserId(id);
        user.setPasswordHash(passwordEncoder.encode(plainPwd));
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(null);
        return userRepository.save(user);
    }

    // -----------------------------------------------------------------------
    // UPDATE  (COUSR02C – EXEC CICS REWRITE DATASET('USRSEC'))
    // -----------------------------------------------------------------------

    /**
     * Updates an existing user's mutable fields (names, type, optionally password).
     * Corresponds to COUSR02C EXEC CICS REWRITE; throws if user not found.
     *
     * @param userId  target SEC-USR-ID
     * @param updates UserData carrying the new values (userId field ignored)
     * @return updated UserData
     * @throws java.util.NoSuchElementException if user not found / inactive
     */
    public UserData updateUser(String userId, UserData updates) {
        String id = normaliseId(userId);
        UserData existing = userRepository.findByUserIdAndActiveTrue(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(
                        "User not found: " + id));

        if (updates.getFirstName() != null) {
            existing.setFirstName(updates.getFirstName());
        }
        if (updates.getLastName() != null) {
            existing.setLastName(updates.getLastName());
        }
        if (updates.getUserType() != null) {
            existing.setUserType(updates.getUserType());
        }
        // Password update is optional – only if a new hash is explicitly provided
        if (updates.getPasswordHash() != null && !updates.getPasswordHash().isBlank()) {
            existing.setPasswordHash(passwordEncoder.encode(updates.getPasswordHash()));
        }
        existing.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(existing);
    }

    // -----------------------------------------------------------------------
    // DEACTIVATE  (COUSR03C – selection flag 'D', EXEC CICS DELETE DATASET)
    // -----------------------------------------------------------------------

    /**
     * Logically deactivates (soft-deletes) a user.
     * In COBOL COUSR03C the record was physically deleted from the VSAM file
     * via EXEC CICS DELETE; here we set {@code active = false} to preserve
     * audit history.
     *
     * @param userId SEC-USR-ID to deactivate
     * @throws java.util.NoSuchElementException if user not found or already inactive
     */
    public void deactivateUser(String userId) {
        String id = normaliseId(userId);
        UserData existing = userRepository.findByUserIdAndActiveTrue(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(
                        "User not found or already inactive: " + id));
        existing.setActive(false);
        existing.setUpdatedAt(LocalDateTime.now());
        userRepository.save(existing);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Normalises a user ID to 8 chars (COBOL PIC X(08) right-pad with spaces
     * semantics, trimmed here for DB storage). Uppercases to match COBOL field.
     */
    private String normaliseId(String userId) {
        if (userId == null) throw new IllegalArgumentException("userId must not be null");
        String trimmed = userId.trim().toUpperCase();
        if (trimmed.isEmpty() || trimmed.length() > 8) {
            throw new IllegalArgumentException(
                    "userId must be 1-8 characters, got: '" + userId + "'");
        }
        return trimmed;
    }
}
