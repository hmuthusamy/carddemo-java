package com.carddemo.repository;

import com.carddemo.model.CardData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link CardData} — mirrors CARD-FILE (CVACT02Y).
 *
 * <p>Provides lookup operations for card master records by account ID,
 * supporting the CBACT04C cross-reference processing logic.
 */
@Repository
public interface CardDataRepository extends JpaRepository<CardData, String> {

    /**
     * Find all cards linked to a given account ID.
     * Mirrors the logical join: CARD-ACCT-ID = requested account.
     *
     * @param cardAcctId the account identifier (CARD-ACCT-ID PIC 9(11))
     * @return list of matching card records
     */
    List<CardData> findByCardAcctId(Long cardAcctId);

    /**
     * Find the first active card for an account.
     * Mirrors: CARD-ACTIVE-STATUS = 'Y' condition.
     *
     * @param cardAcctId     the account identifier
     * @param cardActiveStatus the status code (e.g. "Y")
     * @return optional card record
     */
    Optional<CardData> findFirstByCardAcctIdAndCardActiveStatus(Long cardAcctId, String cardActiveStatus);
}
