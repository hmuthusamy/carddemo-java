package com.carddemo.repository;

import com.carddemo.model.CardData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link CardData} entities.
 * Corresponds to CARDFILE VSAM KSDS in CBACT02C.
 */
@Repository
public interface CardDataRepository extends JpaRepository<CardData, String> {

    /** Find all cards associated with a given account ID. */
    List<CardData> findByCardAcctId(Long cardAcctId);

    /** Find all cards by active status ('Y' or 'N'). */
    List<CardData> findByCardActiveStatus(String cardActiveStatus);
}
