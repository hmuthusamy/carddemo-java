package com.carddemo.repository;

import com.carddemo.model.AccountData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link AccountData}.
 *
 * Replaces the VSAM KSDS sequential-read pattern from CBACT01C:
 *   SELECT ACCTFILE-FILE ASSIGN TO ACCTFILE
 *          ORGANIZATION IS INDEXED
 *          ACCESS MODE  IS SEQUENTIAL
 *          RECORD KEY   IS FD-ACCT-ID
 *
 * Sequential iteration is handled by the JpaPagingItemReader in the batch job.
 */
@Repository
public interface AccountRepository extends JpaRepository<AccountData, Long> {
}
