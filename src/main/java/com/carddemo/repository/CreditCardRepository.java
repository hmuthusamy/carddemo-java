package com.carddemo.repository;

import com.carddemo.model.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for CreditCard entities.
 *
 * Implements JpaSpecificationExecutor to support the dynamic query-building
 * required by the COBOL search paragraphs:
 *
 *   9100-GETCARD-BYACCTCARD  – primary-key READ on CARDDAT by card number
 *   9150-GETCARD-BYACCT      – alternate-index READ on CARDAIX by account ID
 *
 * Both COBOL access paths are expressed as JPA Specifications in
 * {@link com.carddemo.service.CardSearchService}, enabling arbitrary
 * combinations of cardNumber, accountId, and status filters.
 */
@Repository
public interface CreditCardRepository
        extends JpaRepository<CreditCard, String>,
                JpaSpecificationExecutor<CreditCard> {

    /**
     * Look up a single card by its 16-digit card number.
     * Replaces CICS READ FILE(CARDDAT) RIDFLD(WS-CARD-RID-CARDNUM).
     */
    Optional<CreditCard> findByCardNumber(String cardNumber);

    /**
     * Look up cards by the 11-digit account identifier (alternate index path).
     * Replaces CICS READ FILE(CARDAIX) RIDFLD(WS-CARD-RID-ACCT-ID).
     */
    java.util.List<CreditCard> findByAccountId(Long accountId);
}
