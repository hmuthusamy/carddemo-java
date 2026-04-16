package com.carddemo.repository;

import com.carddemo.model.UserData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository – Spring Data JPA repository replacing the COBOL USRSEC VSAM
 * file browse operations performed in COUSR00C.
 *
 * <p>COBOL equivalents:
 * <ul>
 *   <li>{@code STARTBR / READNEXT / READPREV / ENDBR} → {@link #findAllByActiveTrue(Pageable)}
 *   <li>{@code EXEC CICS READ DATASET('USRSEC') RIDFLD(SEC-USR-ID)} → {@link #findById(Object)}
 *   <li>{@code EXEC CICS WRITE DATASET('USRSEC')} → {@link #save(Object)} (via service)
 *   <li>{@code EXEC CICS REWRITE DATASET('USRSEC')} → {@link #save(Object)} (update path)
 *   <li>{@code EXEC CICS DELETE DATASET('USRSEC')} → logical delete via {@link #save(Object)}
 *       setting {@code active = false} (mirrors COUSR03C deactivation)
 * </ul>
 *
 * Migrated from: COUSR00C.CBL (list), COUSR01C.CBL (add), COUSR02C.CBL (update),
 *                COUSR03C.CBL (delete) – all user-management CICS programs.
 */
@Repository
public interface UserRepository extends JpaRepository<UserData, String> {

    /**
     * Paginated browse of active users.
     * Replaces COUSR00C STARTBR/READNEXT loop with 10-record page window.
     *
     * @param pageable page size, number and sort
     * @return page of active UserData records
     */
    Page<UserData> findAllByActiveTrue(Pageable pageable);

    /**
     * Find active user by ID.
     * Replaces {@code EXEC CICS READ DATASET('USRSEC') RIDFLD(SEC-USR-ID)}.
     *
     * @param userId 8-char user identifier (SEC-USR-ID)
     * @return Optional wrapping the user, empty if not found or deactivated
     */
    Optional<UserData> findByUserIdAndActiveTrue(String userId);

    /**
     * Check existence of a user ID (used during creation to prevent duplicates).
     * Replaces COBOL duplicate-key check on EXEC CICS WRITE RESP(DUPREC).
     */
    boolean existsByUserId(String userId);

    /**
     * List all active users without pagination (admin utilities).
     */
    List<UserData> findAllByActiveTrue();

    /**
     * Search active users by last name prefix (case-insensitive).
     * No direct COBOL equivalent; added for REST API usability.
     */
    @Query("SELECT u FROM UserData u WHERE u.active = true " +
           "AND LOWER(u.lastName) LIKE LOWER(CONCAT(:prefix, '%'))")
    List<UserData> findActiveByLastNameStartingWith(String prefix);
}
