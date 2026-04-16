package com.carddemo.repository;

import com.carddemo.model.UserData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for UserData entity (VSAM USRSEC KSDS).
 * Mapped from COBOL copybook CSUSR01Y.cpy
 */
@Repository
public interface UserDataRepository extends JpaRepository<UserData, String> {
}
