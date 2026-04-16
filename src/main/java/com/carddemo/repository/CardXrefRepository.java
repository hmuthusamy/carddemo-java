package com.carddemo.repository;

import com.carddemo.model.CardXref;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * CardXrefRepository – Spring Data JPA repository for {@link CardXref}.
 *
 * Replaces the COBOL EXEC CICS READ in paragraph 9200-GETCARDXREF-BYACCT:
 *
 * <pre>
 *   EXEC CICS READ
 *        DATASET   (LIT-CARDXREFNAME-ACCT-PATH)   -- 'CXACAIX '
 *        RIDFLD    (WS-CARD-RID-ACCT-ID-X)         -- alternate-index by account id
 *        INTO      (CARD-XREF-RECORD)
 *        RESP      (WS-RESP-CD)
 *   END-EXEC
 * </pre>
 *
 * {@code findByXrefAcctId} replicates the alternate-index access path on account id.
 */
@Repository
public interface CardXrefRepository extends JpaRepository<CardXref, String> {

    /**
     * Find a card cross-reference record by account id.
     * Mirrors CICS alternate-index path CXACAIX (keyed on XREF-ACCT-ID).
     *
     * @param xrefAcctId the 11-digit account identifier
     * @return an Optional wrapping the matching record
     */
    Optional<CardXref> findByXrefAcctId(Long xrefAcctId);
}
