package com.carddemo.repository;

import com.carddemo.model.AccountData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link AccountData}.
 *
 * Migrated from CICS VSAM file ACCTDAT accessed via EXEC CICS READ / REWRITE
 * in the original COBOL program COACTUPD.cbl.
 *
 * CICS EXEC REWRITE  →  repository.save()
 * CICS EXEC READ     →  repository.findById()
 */
@Repository
public interface AccountDataRepository extends JpaRepository<AccountData, String> {
}
