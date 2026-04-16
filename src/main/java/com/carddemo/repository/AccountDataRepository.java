package com.carddemo.repository;

import com.carddemo.model.AccountData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Spring Data JPA repository for {@link AccountData} entities.
 *
 * <p>Corresponds to the account VSAM KSDS (keyed by ACCT-ID) used
 * in CBACT02C account file processing.
 */
@Repository
public interface AccountDataRepository extends JpaRepository<AccountData, Long> {

    /**
     * Find all accounts by active status ('Y' = active, 'N' = inactive).
     * Mirrors COBOL logic that checks ACCT-ACTIVE-STATUS.
     */
    List<AccountData> findByAcctActiveStatus(String acctActiveStatus);

    /**
     * Find all accounts belonging to a specific group.
     * Mirrors COBOL lookup by ACCT-GROUP-ID.
     */
    List<AccountData> findByAcctGroupId(String acctGroupId);

    /**
     * Find accounts whose current balance exceeds a given threshold.
     * Supports COBOL-style range comparisons on ACCT-CURR-BAL.
     */
    List<AccountData> findByAcctCurrBalGreaterThan(BigDecimal threshold);
}
