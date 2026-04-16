package com.carddemo.repository;

import com.carddemo.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Account}.
 *
 * <p>Replaces the random-access READ of ACCOUNT-FILE (CVACT01Y) in CBTRN01C
 * paragraph 3000-READ-ACCOUNT. The processor calls
 * {@link #findById(Object)} to confirm account existence before posting,
 * mirroring the COBOL READ … KEY IS FD-ACCT-ID logic.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
}
