package com.carddemo.repository;

import com.carddemo.model.CardData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link CardData}.
 *
 * Replaces CICS VSAM file operations from COCRDUPC:
 *   EXEC CICS READ  FILE(CARDDAT) → {@link #findById(String)}
 *   EXEC CICS READ  FILE(CARDDAT) UPDATE → {@link #findById(String)} + optimistic lock
 *   EXEC CICS REWRITE FILE(CARDDAT) → {@link #save(CardData)}
 *
 * The JPA {@code @Version} column on {@link CardData} provides the same
 * stale-data protection as COBOL paragraph 9300-CHECK-CHANGE-IN-REC.
 */
@Repository
public interface CardDataRepository extends JpaRepository<CardData, String> {
}
