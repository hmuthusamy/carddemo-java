package com.carddemo.controller;

import com.carddemo.model.CardData;
import com.carddemo.service.CardListService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CardListController – REST controller migrated from COBOL CICS program COCRDLIC.CBL.
 *
 * <h2>Program description (from COBOL header)</h2>
 * <pre>
 *   Program: COCRDLIC.CBL
 *   Layer:   Business logic
 *   Function: List Credit Cards
 *             a) All cards if no context passed and admin user
 *             b) Only the ones associated with ACCT in COMMAREA
 *                if user is not admin
 * </pre>
 *
 * <h2>COBOL → REST mapping</h2>
 * <table border="1">
 *   <tr><th>COBOL event / key</th><th>REST equivalent</th></tr>
 *   <tr>
 *     <td>ENTER / initial display (9000-READ-FORWARD, first page)</td>
 *     <td>GET /api/cards?accountId=&amp;page=0&amp;size=7</td>
 *   </tr>
 *   <tr>
 *     <td>PF8 page-down (READNEXT from WS-CA-LAST-CARD-NUM)</td>
 *     <td>GET /api/cards?accountId=&amp;page=N  or  ?cursor=LAST_CARD_NUM</td>
 *   </tr>
 *   <tr>
 *     <td>PF7 page-up (READPREV / 9100-READ-BACKWARDS)</td>
 *     <td>GET /api/cards?accountId=&amp;page=N-1</td>
 *   </tr>
 *   <tr>
 *     <td>S (view) → XCTL to COCRDSLC</td>
 *     <td>GET /api/cards/{cardNum} (returns card detail for client-side routing)</td>
 *   </tr>
 *   <tr>
 *     <td>U (update) → XCTL to COCRDUPC</td>
 *     <td>Consumer calls PATCH /api/cards/{cardNum} (separate update controller)</td>
 *   </tr>
 * </table>
 *
 * <h2>Pagination response envelope</h2>
 * The JSON response wraps the Spring {@link Page} fields so that callers
 * can replicate the COBOL flags:
 * <ul>
 *   <li>{@code hasNextPage}  → {@code CA-NEXT-PAGE-EXISTS}</li>
 *   <li>{@code hasPrevPage}  → NOT {@code CA-FIRST-PAGE}</li>
 *   <li>{@code pageNumber}   → {@code WS-CA-SCREEN-NUM}</li>
 *   <li>{@code totalPages}   → derived from total elements</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/cards")
public class CardListController {

    private static final Logger log = LoggerFactory.getLogger(CardListController.class);

    /** Mirrors COBOL constant WS-MAX-SCREEN-LINES VALUE 7. */
    private static final int DEFAULT_PAGE_SIZE = CardListService.DEFAULT_PAGE_SIZE;

    private final CardListService cardListService;

    public CardListController(CardListService cardListService) {
        this.cardListService = cardListService;
    }

    // -----------------------------------------------------------------------
    // GET /api/cards
    // -----------------------------------------------------------------------

    /**
     * Lists credit cards with optional account filter and pagination.
     *
     * <p>Replaces the COBOL 9000-READ-FORWARD / 9100-READ-BACKWARDS browse loops.
     *
     * <h3>COBOL logic preserved</h3>
     * <ul>
     *   <li>When {@code accountId} is absent: admin view – all cards returned.</li>
     *   <li>When {@code accountId} is present: only cards for that account
     *       (mirrors COBOL comment "only the ones associated with ACCT in COMMAREA").</li>
     *   <li>{@code cursor} provides GTEQ start-browse semantics (WS-CA-LAST-CARD-NUM
     *       for page-down, WS-CA-FIRST-CARD-NUM for page-up).</li>
     *   <li>{@code page} and {@code size} provide standard Spring pagination –
     *       {@code size} defaults to 7 (WS-MAX-SCREEN-LINES).</li>
     * </ul>
     *
     * <h3>Response body</h3>
     * <pre>
     * {
     *   "content"      : [ { "cardNum": "…", "accountId": …, … }, … ],
     *   "pageNumber"   : 0,          // WS-CA-SCREEN-NUM (1-based in COBOL, 0-based here)
     *   "pageSize"     : 7,          // WS-MAX-SCREEN-LINES
     *   "totalElements": 42,
     *   "totalPages"   : 6,
     *   "hasNextPage"  : true,       // CA-NEXT-PAGE-EXISTS
     *   "hasPrevPage"  : false       // NOT CA-FIRST-PAGE
     * }
     * </pre>
     *
     * @param accountId optional 11-digit account id filter
     * @param cursor    optional starting card number for GTEQ browse (WS-CA-LAST-CARD-NUM)
     * @param page      0-based page number; defaults to 0 (first screen)
     * @param size      page size; defaults to 7 (WS-MAX-SCREEN-LINES)
     * @return 200 OK with paginated envelope, or 400 if accountId is non-numeric
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listCards(
            @RequestParam(value = "accountId", required = false) Long accountId,
            @RequestParam(value = "cursor",    required = false) String cursor,
            @RequestParam(value = "page",      defaultValue = "0") int page,
            @RequestParam(value = "size",      defaultValue = "7") int size) {

        log.info("GET /api/cards accountId={} cursor={} page={} size={}",
                accountId, cursor, page, size);

        // Delegate to service (mirrors PERFORM 9000-READ-FORWARD)
        Page<CardData> resultPage = cardListService.listCards(accountId, cursor, page, size);

        // Build response envelope matching COBOL screen / commarea flags
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content",       resultPage.getContent());
        body.put("pageNumber",    resultPage.getNumber());
        body.put("pageSize",      resultPage.getSize());
        body.put("totalElements", resultPage.getTotalElements());
        body.put("totalPages",    resultPage.getTotalPages());
        // CA-NEXT-PAGE-EXISTS (COBOL flag for PF8 availability)
        body.put("hasNextPage",   resultPage.hasNext());
        // NOT CA-FIRST-PAGE (COBOL flag for PF7 availability)
        body.put("hasPrevPage",   resultPage.hasPrevious());

        return ResponseEntity.ok(body);
    }

    // -----------------------------------------------------------------------
    // GET /api/cards/{cardNum}
    // -----------------------------------------------------------------------

    /**
     * Retrieves a single card by its 16-digit card number.
     *
     * <p>Mirrors the COBOL SELECT (S) row action that transferred control to
     * COCRDSLC via {@code EXEC CICS XCTL PROGRAM(LIT-CARDDTLPGM)}.  The card
     * detail is returned as JSON so the REST client can render or route to the
     * detail view.
     *
     * @param cardNum 16-digit card number from the list
     * @return 200 OK with {@link CardData}, or 404 if not found
     */
    @GetMapping("/{cardNum}")
    public ResponseEntity<CardData> getCard(@PathVariable("cardNum") String cardNum) {
        log.info("GET /api/cards/{}", cardNum);

        return cardListService.findByCardNum(cardNum)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Card not found: {}", cardNum);
                    return ResponseEntity.notFound().build();
                });
    }
}
