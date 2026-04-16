package com.carddemo.repository;

import com.carddemo.model.CardXref;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * CardXrefRepository – Spring Data JPA repository for CardXref.
 *
 * Replaces CICS VSAM READ operations against CCXREF / CXACAIX datasets:
 *   EXEC CICS READ DATASET('CCXREF')  RIDFLD(XREF-CARD-NUM) → findByXrefCardNum()
 *   EXEC CICS READ DATASET('CXACAIX') RIDFLD(XREF-ACCT-ID)  → findByXrefAcctId()
 */
@Repository
public interface CardXrefRepository extends JpaRepository<CardXref, String> {

    /** Look up a card-xref record by card number (CCXREF primary key). */
    Optional<CardXref> findByXrefCardNum(String xrefCardNum);

    /** Look up a card-xref record by account ID (CXACAIX alternate index). */
    Optional<CardXref> findByXrefAcctId(String xrefAcctId);
}
