package com.carddemo.repository;

import com.carddemo.model.AccountData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link AccountData}.
 *
 * Replaces sequential VSAM ACCOUNT-FILE reads in CBACT04C.
 * The JpaPagingItemReader used in Cbact03cJobConfig also reads
 * directly through JPA without calling this interface,
 * but it is provided here for service-layer and controller use.
 */
@Repository
public interface AccountDataRepository extends JpaRepository<AccountData, Long> {

    /** Find all active accounts — mirrors "ACCT-ACTIVE-STATUS = 'Y'" filter. */
    List<AccountData> findByAcctActiveStatus(String acctActiveStatus);
}
