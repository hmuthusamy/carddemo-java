package com.carddemo.repository;

import com.carddemo.model.CardXref;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link CardXref}.
 *
 * <p>Replaces the random-access READ of XREF-FILE (CVACT03Y) in CBTRN01C
 * paragraph 2000-LOOKUP-XREF. The processor calls
 * {@link #findById(Object)} to resolve a card number to its account ID,
 * mirroring the COBOL READ … KEY IS FD-XREF-CARD-NUM logic.
 */
@Repository
public interface CardXrefRepository extends JpaRepository<CardXref, String> {
}
