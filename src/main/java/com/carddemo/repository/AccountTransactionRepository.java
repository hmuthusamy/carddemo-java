package com.carddemo.repository;

import com.carddemo.model.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for AccountTransaction (card cross-reference) entity (VSAM XREF KSDS).
 * Mapped from COBOL copybook CVACT03Y.cpy
 */
@Repository
public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, String> {
}
