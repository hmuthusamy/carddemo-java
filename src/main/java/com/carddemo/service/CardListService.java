package com.carddemo.service;

import com.carddemo.model.CardData;
import com.carddemo.repository.CardDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * CardListService – business-logic layer for listing credit cards.
 *
 * <p>Migrated from COBOL CICS program {@code COCRDLIC.CBL}.
 *
 * <h2>COBOL → Java mapping</h2>
 * <table border="1">
 *   <tr><th>COBOL paragraph / operation</th><th>Java equivalent</th></tr>
 *   <tr>
 *     <td>9000-READ-FORWARD: STARTBR → READNEXT (max 7 rows) → ENDBR</td>
 *     <td>{@link #listCards(Long, String, int, int)}</td>
 *   </tr>
 *   <tr>
 *     <td>9100-READ-BACKWARDS: STARTBR → READPREV → ENDBR</td>
 *     <td>Same method – Spring Data handles backward paging via page number</td>
 *   </tr>
 *   <tr>
 *     <td>9500-FILTER-RECORDS: filter by CARD-ACCT-ID / CARD-NUM</td>
 *     <td>Repository query methods with accountId / cardNum predicates</td>
 *   </tr>
 *   <tr>
 *     <td>WS-MAX-SCREEN-LINES VALUE 7</td>
 *     <td>{@link #DEFAULT_PAGE_SIZE} = 7</td>
 *   </tr>
 *   <tr>
 *     <td>WS-CA-SCREEN-NUM (page counter)</td>
 *     <td>{@code pageNumber} parameter (0-based)</td>
 *   </tr>
 *   <tr>
 *     <td>WS-CA-LAST-CARD-NUM (cursor for page-down)</td>
 *     <td>{@code cursorCardNum} parameter for cursor-based paging</td>
 *   </tr>
 * </table>
 *
 * <h2>Admin vs regular-user logic</h2>
 * <ul>
 *   <li>Admin (accountId == null): list ALL cards – mirrors "all cards if no context
 *       passed and admin user" comment in COCRDLIC.</li>
 *   <li>Regular user (accountId supplied): list only cards for that account –
 *       mirrors "only the ones associated with ACCT in COMMAREA".</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class CardListService {

    private static final Logger log = LoggerFactory.getLogger(CardListService.class);

    /**
     * COBOL: {@code WS-MAX-SCREEN-LINES PIC S9(4) COMP VALUE 7}.
     * Default page size = 7 cards per page (one screen worth).
     */
    public static final int DEFAULT_PAGE_SIZE = 7;

    private final CardDataRepository cardDataRepository;

    public CardListService(CardDataRepository cardDataRepository) {
        this.cardDataRepository = cardDataRepository;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Returns a paginated list of credit cards.
     *
     * <p>This method consolidates the COBOL 9000-READ-FORWARD and 9100-READ-BACKWARDS
     * paragraphs.  The CICS STARTBR/READNEXT/ENDBR browse is replaced by a single
     * paginated JPA query; the {@code cursorCardNum} parameter optionally anchors the
     * page at a specific card number (GTEQ equivalent).
     *
     * @param accountId     11-digit account id filter; {@code null} means admin – return all
     * @param cursorCardNum starting card number for cursor-based forward paging;
     *                      {@code null} or blank starts from the beginning
     * @param pageNumber    0-based page number (WS-CA-SCREEN-NUM - 1)
     * @param pageSize      records per page; use {@link #DEFAULT_PAGE_SIZE} for 7
     * @return {@link Page} of {@link CardData} – never null
     */
    public Page<CardData> listCards(Long accountId, String cursorCardNum,
                                    int pageNumber, int pageSize) {

        // Guard: enforce sensible page size bounds
        int size = (pageSize <= 0 || pageSize > 100) ? DEFAULT_PAGE_SIZE : pageSize;

        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by("cardNum").ascending());

        boolean hasCursor  = (cursorCardNum != null && !cursorCardNum.isBlank());
        boolean hasAccount = (accountId != null && accountId > 0L);

        log.debug("listCards accountId={} cursor={} page={} size={}",
                accountId, cursorCardNum, pageNumber, size);

        // ------------------------------------------------------------------
        // Mirrors 9500-FILTER-RECORDS logic:
        //   FLG-ACCTFILTER-ISVALID → filter by account
        //   FLG-CARDFILTER-ISVALID → cursor-based start (GTEQ)
        // ------------------------------------------------------------------
        if (hasAccount && hasCursor) {
            // CICS: STARTBR RIDFLD(cardNum) GTEQ + filter by CARD-ACCT-ID
            return cardDataRepository
                    .findByCardNumGreaterThanEqualAndAccountId(cursorCardNum, accountId, pageable);
        }
        if (hasAccount) {
            // CICS: STARTBR from top + filter by CARD-ACCT-ID
            return cardDataRepository.findByAccountId(accountId, pageable);
        }
        if (hasCursor) {
            // Admin, GTEQ from cursor
            return cardDataRepository.findByCardNumGreaterThanEqual(cursorCardNum, pageable);
        }
        // Admin, no cursor – list everything from start
        return cardDataRepository.findAll(pageable);
    }

    /**
     * Convenience overload using the default page size (7 – mirrors WS-MAX-SCREEN-LINES).
     *
     * @param accountId     account id filter; {@code null} for admin
     * @param cursorCardNum starting card number cursor; may be {@code null}
     * @param pageNumber    0-based page number
     * @return page of matching {@link CardData}
     */
    public Page<CardData> listCards(Long accountId, String cursorCardNum, int pageNumber) {
        return listCards(accountId, cursorCardNum, pageNumber, DEFAULT_PAGE_SIZE);
    }

    /**
     * Looks up a single card by its 16-digit card number.
     *
     * <p>Used when the controller needs to resolve a card before navigating to
     * the detail or update screen (mirrors the XCTL to COCRDSLC / COCRDUPC).
     *
     * @param cardNum 16-digit card number
     * @return {@link Optional} containing the card, or empty if not found
     */
    public Optional<CardData> findByCardNum(String cardNum) {
        log.debug("findByCardNum cardNum={}", cardNum);
        return cardDataRepository.findByCardNum(cardNum);
    }
}
