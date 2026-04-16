package com.carddemo.repository;

import com.carddemo.model.CardXref;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link CardXref}.
 *
 * Provides sequential read (findAll) for CBACT03C reporting
 * and alternate-key lookup by account ID for CBACT04C interest posting.
 *
 * COBOL: READ XREF-FILE KEY IS FD-XREF-ACCT-ID (alternate key lookup)
 */
@Repository
public interface CardXrefRepository extends JpaRepository<CardXref, String> {

    /** Alternate-key lookup: mirrors VSAM alternate-key READ by FD-XREF-ACCT-ID. */
    Optional<CardXref> findByXrefAcctId(Long xrefAcctId);
}
