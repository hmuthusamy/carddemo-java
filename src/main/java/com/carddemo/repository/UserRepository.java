package com.carddemo.repository;

import com.carddemo.model.User;
import com.carddemo.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the USRSEC user table.
 *
 * <p>Replaces direct CICS VSAM READ/DELETE commands from COUSR03C.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Count active users of a given type.
     * Used by {@link com.carddemo.service.UserDeleteService} to guard
     * against deleting the last active admin (equivalent to the implicit
     * RACF/CICS protection in the original mainframe environment).
     *
     * @param userType the single-character user-type code ('A' = admin)
     * @param status   the status filter (ACTIVE)
     * @return number of matching users
     */
    long countByUserTypeAndStatus(String userType, UserStatus status);
}
