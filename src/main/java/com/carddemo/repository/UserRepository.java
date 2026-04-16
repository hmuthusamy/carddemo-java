package com.carddemo.repository;

import com.carddemo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User}.
 *
 * <p>Replaces direct VSAM READ operations (EXEC CICS READ DATASET('USRSEC') ...)
 * performed in COSGN00C.cbl and COUSR00C.cbl.  The {@code findByUserId} method
 * mirrors the keyed READ using SEC-USR-ID as the primary key.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find a user by their login ID.
     *
     * <p>Equivalent to the COBOL:
     * <pre>
     *   EXEC CICS READ
     *        DATASET   (WS-USRSEC-FILE)
     *        INTO      (SEC-USER-DATA)
     *        RIDFLD    (SEC-USR-ID)
     *        ...
     *   END-EXEC
     * </pre>
     *
     * @param userId the 8-character user ID (SEC-USR-ID)
     * @return an {@link Optional} containing the user if found
     */
    Optional<User> findByUserId(String userId);
}
