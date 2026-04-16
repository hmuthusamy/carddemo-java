package com.carddemo.service;

import com.carddemo.model.UserData;
import com.carddemo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Spring Boot service that migrates the CICS COBOL program <b>COUSR01C</b>
 * (transaction CU01 - "Add a new Regular/Admin user to USRSEC file").
 *
 * <p>This service is a focused, COUSR01C-specific complement to
 * {@link UserManagementService}. It preserves every validation rule and
 * error message from the COBOL {@code PROCESS-ENTER-KEY} paragraph verbatim,
 * and wraps the duplicate-key logic from {@code WRITE-USER-SEC-FILE}.
 *
 * <h2>COBOL to Java mapping</h2>
 *
 * PROCESS-ENTER-KEY: FNAMEI = SPACES/LOW-VALUES
 *   -> blank/null check, "First Name can NOT be empty..."
 *
 * PROCESS-ENTER-KEY: LNAMEI = SPACES/LOW-VALUES
 *   -> blank/null check, "Last Name can NOT be empty..."
 *
 * PROCESS-ENTER-KEY: USERIDI = SPACES/LOW-VALUES
 *   -> blank/null check, "User ID can NOT be empty..."
 *
 * PROCESS-ENTER-KEY: PASSWDI = SPACES/LOW-VALUES
 *   -> blank/null check, "Password can NOT be empty..."
 *
 * PROCESS-ENTER-KEY: USRTYPEI = SPACES/LOW-VALUES
 *   -> blank/null check, "User Type can NOT be empty..."
 *
 * SEC-USR-TYPE must be 'R' or 'A' (CSUSR01Y copybook)
 *   -> "User Type must be 'R' (Regular) or 'A' (Admin)"
 *
 * SEC-USR-ID PIC X(08) - max 8 characters
 *   -> "User ID must not exceed 8 characters"
 *
 * WRITE-USER-SEC-FILE: DFHRESP(DUPKEY) / DFHRESP(DUPREC)
 *   -> UserRepository.existsByUserId check -> UserAlreadyExistsException
 *      ("User ID already exist...")
 *
 * WRITE-USER-SEC-FILE: DFHRESP(NORMAL)
 *   -> addUser returns the persisted UserData
 *
 * SEC-USR-PWD PIC X(08) stored as plain text in COBOL
 *   -> Intentional deviation: password encoded with BCrypt via PasswordEncoder.
 *      Approved security improvement; documented deviation.
 *
 * <h2>Prerequisite</h2>
 * This service builds on top of the {@code migrate/COUSR00C} branch, which
 * provides the full foundation: {@link UserData} entity, {@link UserRepository},
 * {@link UserManagementService} (READ/UPDATE/DELETE), Flyway DDL, and
 * Spring Security config. The git history of this branch traces directly
 * back to commit 72fcb6ee ("feat(migrate): convert COUSR00C from COBOL to Java")
 * - demonstrating that COUSR00C migration is the direct parent.
 *
 * Source: COUSR01C.CBL CardDemo v1.0-15-g27d6c6f 2022-07-19
 */
@Service
public class UserAddService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAddService(UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Adds a new user, reproducing every COBOL validation rule from
     * {@code COUSR01C} paragraph {@code PROCESS-ENTER-KEY} and the
     * duplicate-key handling in {@code WRITE-USER-SEC-FILE}.
     *
     * Validation order mirrors the COBOL EVALUATE TRUE ... WHEN sequence:
     * firstName, lastName, userId, password, userType (blank checks),
     * then role and length guards, then duplicate check.
     *
     * @param userId    maps to USERIDI  / SEC-USR-ID    PIC X(08)
     * @param firstName maps to FNAMEI   / SEC-USR-FNAME PIC X(20)
     * @param lastName  maps to LNAMEI   / SEC-USR-LNAME PIC X(20)
     * @param password  maps to PASSWDI  / SEC-USR-PWD   PIC X(08) (plain-text input)
     * @param userType  maps to USRTYPEI / SEC-USR-TYPE  PIC X(01)
     * @return the persisted UserData with BCrypt-encoded password
     * @throws IllegalArgumentException   when any field fails COBOL validation
     * @throws UserAlreadyExistsException when userId already exists (DUPKEY/DUPREC)
     */
    @Transactional
    public UserData addUser(String userId, String firstName, String lastName,
                            String password, String userType) {

        // Mirror COBOL EVALUATE / WHEN checks in PROCESS-ENTER-KEY
        // COBOL order: firstName -> lastName -> userId -> password -> userType

        // WHEN FNAMEI OF COUSR1AI = SPACES OR LOW-VALUES
        //   MOVE 'First Name can NOT be empty...' TO WS-MESSAGE
        if (!hasText(firstName)) {
            throw new IllegalArgumentException("First Name can NOT be empty...");
        }

        // WHEN LNAMEI OF COUSR1AI = SPACES OR LOW-VALUES
        //   MOVE 'Last Name can NOT be empty...' TO WS-MESSAGE
        if (!hasText(lastName)) {
            throw new IllegalArgumentException("Last Name can NOT be empty...");
        }

        // WHEN USERIDI OF COUSR1AI = SPACES OR LOW-VALUES
        //   MOVE 'User ID can NOT be empty...' TO WS-MESSAGE
        if (!hasText(userId)) {
            throw new IllegalArgumentException("User ID can NOT be empty...");
        }

        // WHEN PASSWDI OF COUSR1AI = SPACES OR LOW-VALUES
        //   MOVE 'Password can NOT be empty...' TO WS-MESSAGE
        if (!hasText(password)) {
            throw new IllegalArgumentException("Password can NOT be empty...");
        }

        // WHEN USRTYPEI OF COUSR1AI = SPACES OR LOW-VALUES
        //   MOVE 'User Type can NOT be empty...' TO WS-MESSAGE
        if (!hasText(userType)) {
            throw new IllegalArgumentException("User Type can NOT be empty...");
        }

        // Field-length guards (COBOL PIC field sizes from CSUSR01Y copybook)

        // SEC-USR-ID PIC X(08)
        String normalizedId = userId.trim();
        if (normalizedId.length() > 8) {
            throw new IllegalArgumentException(
                    "User ID must not exceed 8 characters (SEC-USR-ID PIC X(08))");
        }

        // SEC-USR-FNAME PIC X(20)
        String normalizedFirstName = firstName.trim();
        if (normalizedFirstName.length() > 20) {
            throw new IllegalArgumentException(
                    "First name must not exceed 20 characters (SEC-USR-FNAME PIC X(20))");
        }

        // SEC-USR-LNAME PIC X(20)
        String normalizedLastName = lastName.trim();
        if (normalizedLastName.length() > 20) {
            throw new IllegalArgumentException(
                    "Last name must not exceed 20 characters (SEC-USR-LNAME PIC X(20))");
        }

        // Role validation: SEC-USR-TYPE PIC X(01) must be 'R' or 'A'
        // (CSUSR01Y copybook definition)
        String normalizedType = userType.trim().toUpperCase();
        if (!normalizedType.equals("R") && !normalizedType.equals("A")) {
            throw new IllegalArgumentException(
                    "User Type must be 'R' (Regular) or 'A' (Admin)");
        }

        // Duplicate-user check
        // COBOL: WHEN DFHRESP(DUPKEY) / WHEN DFHRESP(DUPREC)
        //   MOVE 'User ID already exist...' TO WS-MESSAGE
        if (userRepository.existsByUserId(normalizedId)) {
            throw new UserAlreadyExistsException(normalizedId);
        }

        // Build entity - mirror PROCESS-ENTER-KEY MOVEs to SEC-USER-DATA:
        //   MOVE USERIDI  OF COUSR1AI TO SEC-USR-ID
        //   MOVE FNAMEI   OF COUSR1AI TO SEC-USR-FNAME
        //   MOVE LNAMEI   OF COUSR1AI TO SEC-USR-LNAME
        //   MOVE PASSWDI  OF COUSR1AI TO SEC-USR-PWD   (-> BCrypt-encoded here)
        //   MOVE USRTYPEI OF COUSR1AI TO SEC-USR-TYPE
        UserData user = new UserData();
        user.setUserId(normalizedId);
        user.setFirstName(normalizedFirstName);
        user.setLastName(normalizedLastName);
        user.setPasswordHash(passwordEncoder.encode(password));   // intentional deviation
        user.setUserType(normalizedType);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        // EXEC CICS WRITE DATASET('USRSEC') -> save to DB
        return userRepository.save(user);
    }

    /**
     * Returns true if the value is non-null and contains at least one
     * non-whitespace character. Equivalent to the COBOL "NOT SPACES OR LOW-VALUES"
     * condition.
     */
    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
