package com.carddemo.service;

import com.carddemo.model.CustomerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Cbcus01cService – business-logic service migrated from COBOL program CBCUS01C.
 *
 * <p>The original COBOL program (CBCUS01C.CBL) performed the following steps:
 * <ol>
 *   <li>Open the KSDS VSAM customer file (CUSTFILE) for sequential read.</li>
 *   <li>Loop: read next record → DISPLAY CUSTOMER-RECORD to SYSOUT.</li>
 *   <li>On file status '10' (EOF) set END-OF-FILE='Y' and exit loop.</li>
 *   <li>On any other non-zero status, display an I/O error message and ABEND.</li>
 *   <li>Close the file and return.</li>
 * </ol>
 *
 * <p>This service encapsulates the per-record business rules extracted from the COBOL
 * PROCEDURE DIVISION so that {@link com.carddemo.batch.Cbcus01cJobConfig} can delegate
 * to it from the Spring Batch {@code ItemProcessor}.
 */
@Service
public class Cbcus01cService {

    private static final Logger log = LoggerFactory.getLogger(Cbcus01cService.class);

    /** Minimum FICO score considered creditworthy (business rule derived from CardDemo). */
    static final int MIN_CREDITWORTHY_FICO = 300;

    /** Maximum valid FICO score. */
    static final int MAX_FICO_SCORE = 850;

    /**
     * Process a single {@link CustomerData} record.
     *
     * <p>Mirrors the {@code DISPLAY CUSTOMER-RECORD} logic in {@code 1000-CUSTFILE-GET-NEXT}
     * and adds the enrichment / validation rules that a batch processor would apply:
     * <ul>
     *   <li>Log the full customer record (replaces COBOL {@code DISPLAY}).</li>
     *   <li>Validate that CUST-ID is positive.</li>
     *   <li>Normalise name fields (trim trailing spaces – COBOL fixed-width strings).</li>
     *   <li>Validate FICO score range [300–850]; clamp / warn if out of range.</li>
     *   <li>Mark the record as processed ({@code processedFlag = "Y"}).</li>
     * </ul>
     *
     * @param customer the raw {@link CustomerData} record read from the database
     * @return the enriched {@link CustomerData} record, or {@code null} to skip invalid records
     */
    public CustomerData process(CustomerData customer) {
        if (customer == null) {
            log.warn("Received null CustomerData record – skipping");
            return null;
        }

        // -----------------------------------------------------------------------
        // Replicate:  DISPLAY CUSTOMER-RECORD  (COBOL line ~57 & ~69)
        // -----------------------------------------------------------------------
        log.info("Processing CUSTOMER-RECORD: custId={} name={} {} {} ficoScore={}",
                customer.getCustId(),
                customer.getCustFirstName(),
                customer.getCustMiddleName(),
                customer.getCustLastName(),
                customer.getCustFicoCreditScore());

        // -----------------------------------------------------------------------
        // Business Rule 1 – CUST-ID must be positive (mirrors KSDS primary key)
        // -----------------------------------------------------------------------
        if (customer.getCustId() == null || customer.getCustId() <= 0) {
            log.error("ERROR READING CUSTOMER FILE – invalid custId={}", customer.getCustId());
            return null; // skip record (COBOL would ABEND; Spring Batch skips with skip policy)
        }

        // -----------------------------------------------------------------------
        // Business Rule 2 – Normalise fixed-width COBOL strings (trim spaces)
        // -----------------------------------------------------------------------
        customer.setCustFirstName(trimCobol(customer.getCustFirstName()));
        customer.setCustMiddleName(trimCobol(customer.getCustMiddleName()));
        customer.setCustLastName(trimCobol(customer.getCustLastName()));
        customer.setCustAddrLine1(trimCobol(customer.getCustAddrLine1()));
        customer.setCustAddrLine2(trimCobol(customer.getCustAddrLine2()));
        customer.setCustAddrLine3(trimCobol(customer.getCustAddrLine3()));
        customer.setCustAddrStateCd(trimCobol(customer.getCustAddrStateCd()));
        customer.setCustAddrCountryCd(trimCobol(customer.getCustAddrCountryCd()));
        customer.setCustAddrZip(trimCobol(customer.getCustAddrZip()));
        customer.setCustPhoneNum1(trimCobol(customer.getCustPhoneNum1()));
        customer.setCustPhoneNum2(trimCobol(customer.getCustPhoneNum2()));
        customer.setCustGovtIssuedId(trimCobol(customer.getCustGovtIssuedId()));
        customer.setCustEftAccountId(trimCobol(customer.getCustEftAccountId()));

        // -----------------------------------------------------------------------
        // Business Rule 3 – Validate & clamp FICO credit score  PIC 9(03)
        // -----------------------------------------------------------------------
        if (customer.getCustFicoCreditScore() != null) {
            int fico = customer.getCustFicoCreditScore();
            if (fico < MIN_CREDITWORTHY_FICO || fico > MAX_FICO_SCORE) {
                log.warn("custId={} has out-of-range FICO score {}; clamping to [{},{}]",
                        customer.getCustId(), fico, MIN_CREDITWORTHY_FICO, MAX_FICO_SCORE);
                customer.setCustFicoCreditScore(
                        Math.max(MIN_CREDITWORTHY_FICO, Math.min(MAX_FICO_SCORE, fico)));
            }
        }

        // -----------------------------------------------------------------------
        // Business Rule 4 – Validate primary card holder indicator  PIC X(01)
        // -----------------------------------------------------------------------
        String ind = customer.getCustPriCardHolderInd();
        if (ind != null && !ind.isBlank() && !"Y".equalsIgnoreCase(ind) && !"N".equalsIgnoreCase(ind)) {
            log.warn("custId={} has unexpected priCardHolderInd='{}'; defaulting to 'N'",
                    customer.getCustId(), ind);
            customer.setCustPriCardHolderInd("N");
        }

        // -----------------------------------------------------------------------
        // Mark record as processed (new enrichment field – not in original COBOL)
        // -----------------------------------------------------------------------
        customer.setProcessedFlag("Y");

        return customer;
    }

    /**
     * Replicates COBOL fixed-width string trimming: removes trailing (and leading) spaces.
     *
     * @param value raw COBOL string value (may be null)
     * @return trimmed string, or {@code null} if input was null
     */
    String trimCobol(String value) {
        return value == null ? null : value.strip();
    }
}
