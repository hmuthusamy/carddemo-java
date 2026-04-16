package com.carddemo.repository;

import com.carddemo.model.AccountData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for AccountData — mirrors ACCOUNT-FILE (CVACT01Y).
 * Supports random access by account ID used in CBACT04C paragraph 1100-GET-ACCT-DATA.
 */
@Repository
public interface AccountDataRepository extends JpaRepository<AccountData, Long> {
}
