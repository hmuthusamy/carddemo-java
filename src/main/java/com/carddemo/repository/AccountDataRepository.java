package com.carddemo.repository;

import com.carddemo.model.AccountData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for AccountData entity (VSAM ACCT KSDS).
 * Mapped from COBOL copybook CVACT01Y.cpy
 */
@Repository
public interface AccountDataRepository extends JpaRepository<AccountData, Long> {
}
