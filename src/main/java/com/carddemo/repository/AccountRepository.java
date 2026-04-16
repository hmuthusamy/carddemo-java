package com.carddemo.repository;

import com.carddemo.model.AccountData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * AccountRepository – Spring Data JPA repository for {@link AccountData}.
 *
 * Replaces the COBOL EXEC CICS READ statement in paragraph 9300-GETACCTDATA-BYACCT:
 *
 * <pre>
 *   EXEC CICS READ
 *        DATASET   (LIT-ACCTFILENAME)     -- 'ACCTDAT '
 *        RIDFLD    (WS-CARD-RID-ACCT-ID-X)
 *        INTO      (ACCOUNT-RECORD)
 *        RESP      (WS-RESP-CD)
 *   END-EXEC
 * </pre>
 *
 * {@code findById(accountId)} replaces the CICS READ with DFHRESP(NORMAL) / DFHRESP(NOTFND) logic.
 */
@Repository
public interface AccountRepository extends JpaRepository<AccountData, Long> {
}
