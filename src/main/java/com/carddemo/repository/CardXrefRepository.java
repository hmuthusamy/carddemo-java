package com.carddemo.repository;

import com.carddemo.model.CardXref;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for CardXref — mirrors XREF-FILE (CVACT03Y).
 * Supports alternate key lookup by account ID used in CBACT04C paragraph 1110-GET-XREF-DATA.
 */
@Repository
public interface CardXrefRepository extends JpaRepository<CardXref, String> {

    /**
     * Lookup by alternate key FD-XREF-ACCT-ID.
     * Mirrors: READ XREF-FILE KEY IS FD-XREF-ACCT-ID
     */
    Optional<CardXref> findFirstByXrefAcctId(Long xrefAcctId);
}
