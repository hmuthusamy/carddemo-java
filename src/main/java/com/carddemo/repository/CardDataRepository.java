package com.carddemo.repository;

import com.carddemo.model.CardData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * CardDataRepository – Spring Data JPA repository for {@link CardData}.
 *
 * <p>COBOL browse operation mapping (from COCRDLIC.CBL):
 * <pre>
 *   EXEC CICS STARTBR  DATASET('CARDDAT') RIDFLD(WS-CARD-RID-CARDNUM) GTEQ
 *   EXEC CICS READNEXT DATASET('CARDDAT') INTO(CARD-RECORD) …
 *   EXEC CICS ENDBR    FILE('CARDDAT')
 * </pre>
 *
 * <p>These sequential browse operations are replaced by paginated JPA queries:
 * <ul>
 *   <li>{@link #findAll(Pageable)} – admin: browse all cards (no account filter)</li>
 *   <li>{@link #findByAccountId(Long, Pageable)} – user: browse cards for one account</li>
 *   <li>{@link #findByCardNumGreaterThanEqualAndAccountId(String, Long, Pageable)}
 *       – page-forward from a known card-number cursor with an account filter</li>
 *   <li>{@link #findByCardNumGreaterThanEqual(String, Pageable)}
 *       – page-forward from a known card-number cursor, admin view</li>
 *   <li>{@link #findByCardNum(String)} – exact card-number lookup</li>
 * </ul>
 *
 * <p>Pagination maps directly to the COBOL screen paging constants:
 * <pre>
 *   WS-MAX-SCREEN-LINES  PIC S9(4) COMP VALUE 7   →  page size = 7
 *   WS-CA-SCREEN-NUM / WS-CA-LAST-CARD-NUM         →  Pageable page / cursor
 * </pre>
 */
@Repository
public interface CardDataRepository extends JpaRepository<CardData, String> {

    /**
     * Returns a page of cards belonging to the given account.
     * Replaces the CICS STARTBR/READNEXT loop filtered by CARD-ACCT-ID.
     *
     * @param accountId the 11-digit account identifier
     * @param pageable  page number and size (size = 7 mirrors COBOL screen lines)
     * @return page of matching {@link CardData} records
     */
    Page<CardData> findByAccountId(Long accountId, Pageable pageable);

    /**
     * Page-forward browse from a cursor card number, scoped to one account.
     * Mirrors the COBOL logic:
     * <pre>
     *   MOVE WS-CA-LAST-CARD-NUM TO WS-CARD-RID-CARDNUM
     *   EXEC CICS STARTBR … RIDFLD(WS-CARD-RID-CARDNUM) GTEQ
     * </pre>
     *
     * @param cardNum   cursor – first card number to include (≥ comparison = GTEQ)
     * @param accountId account filter
     * @param pageable  paging descriptor
     * @return page of {@link CardData} records starting at (or after) {@code cardNum}
     */
    Page<CardData> findByCardNumGreaterThanEqualAndAccountId(
            String cardNum, Long accountId, Pageable pageable);

    /**
     * Page-forward browse from a cursor card number for admin users (no account filter).
     *
     * @param cardNum  cursor card number
     * @param pageable paging descriptor
     * @return page of all {@link CardData} starting at (or after) {@code cardNum}
     */
    Page<CardData> findByCardNumGreaterThanEqual(String cardNum, Pageable pageable);

    /**
     * Exact card-number lookup used when navigating to card detail / update screens.
     * Replaces the COMMAREA CDEMO-CARD-NUM transfer in COCRDLIC.
     *
     * @param cardNum 16-digit card number
     * @return {@link Optional} containing the card if found
     */
    Optional<CardData> findByCardNum(String cardNum);
}
