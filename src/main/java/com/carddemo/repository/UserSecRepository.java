package com.carddemo.repository;

import com.carddemo.model.UserSec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the UserSec entity.
 *
 * Replaces the VSAM EXEC CICS READ / REWRITE operations from
 * COUSR02C paragraphs READ-USER-SEC-FILE and UPDATE-USER-SEC-FILE.
 */
@Repository
public interface UserSecRepository extends JpaRepository<UserSec, String> {
    // findById(String id) – covers READ-USER-SEC-FILE (DFHRESP NOTFND → empty Optional)
    // save(UserSec)       – covers UPDATE-USER-SEC-FILE (CICS REWRITE)
}
