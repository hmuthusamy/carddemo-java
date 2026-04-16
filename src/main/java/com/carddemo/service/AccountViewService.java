package com.carddemo.service;

import com.carddemo.exception.AccountNotFoundException;
import com.carddemo.model.AccountData;
import com.carddemo.model.AccountViewResponse;
import com.carddemo.model.CardXref;
import com.carddemo.model.CustomerData;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AccountViewService – business logic migrated from COBOL program COACTVWC.
 *
 * <p>This service implements the account-view read chain originally expressed in
 * COBOL paragraphs 9000-READ-ACCT, 9200-GETCARDXREF-BYACCT, 9300-GETACCTDATA-BYACCT
 * and 9400-GETCUSTDATA-BYCUST, replacing EXEC CICS READ statements with Spring Data
 * JPA repository calls.
 *
 * <pre>
 * COBOL paragraph → Java method
 * ─────────────────────────────────────────────────────────────────────────────
 * 9000-READ-ACCT                 →  getAccountView(accountId)
 * 9200-GETCARDXREF-BYACCT        →  lookupCardXref(accountId)
 * 9300-GETACCTDATA-BYACCT        →  lookupAccountData(accountId)
 * 9400-GETCUSTDATA-BYCUST        →  lookupCustomerData(customerId)
 * 1200-SETUP-SCREEN-VARS (part)  →  buildResponse(account, customer)
 * SSN STRING formatting          →  formatSsn(rawSsn)
 * </pre>
 */
@Service
@Transactional(readOnly = true)
public class AccountViewService {

    private static final Logger log = LoggerFactory.getLogger(AccountViewService.class);

    private final AccountRepository  accountRepository;
    private final CustomerRepository customerRepository;
    private final CardXrefRepository cardXrefRepository;

    public AccountViewService(AccountRepository  accountRepository,
                              CustomerRepository customerRepository,
                              CardXrefRepository cardXrefRepository) {
        this.accountRepository  = accountRepository;
        this.customerRepository = customerRepository;
        this.cardXrefRepository = cardXrefRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieve a full account view for the given account id.
     *
     * <p>Mirrors COBOL paragraph {@code 9000-READ-ACCT}:
     * <ol>
     *   <li>Look up card cross-reference by account id (was CICS READ on CXACAIX).</li>
     *   <li>Look up account master record (was CICS READ on ACCTDAT).</li>
     *   <li>Look up customer master record (was CICS READ on CUSTDAT).</li>
     *   <li>Build and return the response DTO (was MOVE statements in 1200-SETUP-SCREEN-VARS).</li>
     * </ol>
     *
     * @param accountId 11-digit account identifier (CDEMO-ACCT-ID)
     * @return populated {@link AccountViewResponse}
     * @throws AccountNotFoundException if the account or associated customer is not found
     * @throws IllegalArgumentException if the account id fails validation
     */
    public AccountViewResponse getAccountView(Long accountId) {
        log.debug("getAccountView: accountId={}", accountId);

        // ── 2210-EDIT-ACCOUNT equivalent ─────────────────────────────────────
        validateAccountId(accountId);

        // ── 9200-GETCARDXREF-BYACCT ──────────────────────────────────────────
        // EXEC CICS READ DATASET('CXACAIX') RIDFLD(WS-CARD-RID-ACCT-ID-X) ...
        CardXref xref = lookupCardXref(accountId);

        // ── 9300-GETACCTDATA-BYACCT ──────────────────────────────────────────
        // EXEC CICS READ DATASET('ACCTDAT') RIDFLD(WS-CARD-RID-ACCT-ID-X) ...
        AccountData account = lookupAccountData(accountId);

        // ── 9400-GETCUSTDATA-BYCUST ──────────────────────────────────────────
        // EXEC CICS READ DATASET('CUSTDAT') RIDFLD(WS-CARD-RID-CUST-ID-X) ...
        CustomerData customer = lookupCustomerData(xref.getXrefCustId());

        // ── 1200-SETUP-SCREEN-VARS ────────────────────────────────────────────
        return buildResponse(account, customer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers  (package-private for unit testing)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates the account id before any file reads.
     *
     * Mirrors COBOL paragraph 2210-EDIT-ACCOUNT:
     * <pre>
     *   IF CC-ACCT-ID IS NOT NUMERIC OR CC-ACCT-ID EQUAL ZEROES
     *      SET INPUT-ERROR ...
     * </pre>
     */
    void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            // SEARCHED-ACCT-ZEROES / SEARCHED-ACCT-NOT-NUMERIC
            throw new IllegalArgumentException(
                    "Account number must be a non zero 11 digit number");
        }
    }

    /**
     * Look up the card cross-reference record by account id.
     *
     * Replaces COBOL paragraph 9200-GETCARDXREF-BYACCT.
     * CICS DFHRESP(NOTFND) → AccountNotFoundException.
     */
    CardXref lookupCardXref(Long accountId) {
        return cardXrefRepository.findByXrefAcctId(accountId)
                .orElseThrow(() -> {
                    // DID-NOT-FIND-ACCT-IN-CARDXREF
                    log.warn("Account {} not found in card cross-reference file", accountId);
                    return new AccountNotFoundException(accountId,
                            "Did not find this account in account card xref file");
                });
    }

    /**
     * Look up the account master record by account id.
     *
     * Replaces COBOL paragraph 9300-GETACCTDATA-BYACCT.
     * CICS DFHRESP(NOTFND) → AccountNotFoundException.
     */
    AccountData lookupAccountData(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    // DID-NOT-FIND-ACCT-IN-ACCTDAT
                    log.warn("Account {} not found in account master file", accountId);
                    return new AccountNotFoundException(accountId,
                            "Did not find this account in account master file");
                });
    }

    /**
     * Look up the customer master record by customer id.
     *
     * Replaces COBOL paragraph 9400-GETCUSTDATA-BYCUST.
     * CICS DFHRESP(NOTFND) → AccountNotFoundException.
     */
    CustomerData lookupCustomerData(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    // DID-NOT-FIND-CUST-IN-CUSTDAT
                    log.warn("Customer {} not found in customer master file", customerId);
                    return new AccountNotFoundException(customerId,
                            "Did not find associated customer in master file");
                });
    }

    /**
     * Build the response DTO from account and customer records.
     *
     * Mirrors COBOL paragraph 1200-SETUP-SCREEN-VARS:
     * <pre>
     *   MOVE ACCT-ACTIVE-STATUS  TO ACSTTUSO  OF CACTVWAO
     *   MOVE ACCT-CURR-BAL       TO ACURBALO  OF CACTVWAO
     *   ...
     *   MOVE CUST-FIRST-NAME     TO ACSFNAMO  OF CACTVWAO
     *   STRING CUST-SSN(1:3) '-' CUST-SSN(4:2) '-' CUST-SSN(6:4)
     *          DELIMITED BY SIZE INTO ACSTSSNO OF CACTVWAO
     * </pre>
     */
    AccountViewResponse buildResponse(AccountData account, CustomerData customer) {
        return AccountViewResponse.builder()
                // ── Account fields (FOUND-ACCT-IN-MASTER branch) ──────────────
                .accountId(account.getAcctId())
                .activeStatus(account.getAcctActiveStatus())
                .currentBalance(account.getAcctCurrBal())
                .creditLimit(account.getAcctCreditLimit())
                .cashCreditLimit(account.getAcctCashCreditLimit())
                .currCycCredit(account.getAcctCurrCycCredit())
                .currCycDebit(account.getAcctCurrCycDebit())
                .openDate(account.getAcctOpenDate())
                .expirationDate(account.getAcctExpiraionDate())
                .reissueDate(account.getAcctReissueDate())
                .groupId(account.getAcctGroupId())
                // ── Customer fields (FOUND-CUST-IN-MASTER branch) ─────────────
                .customerId(customer.getCustId())
                .ssn(formatSsn(customer.getCustSsn()))
                .ficoCreditScore(customer.getCustFicoCreditScore())
                .dateOfBirth(customer.getCustDobYyyyMmDd())
                .firstName(customer.getCustFirstName())
                .middleName(customer.getCustMiddleName())
                .lastName(customer.getCustLastName())
                .addressLine1(customer.getCustAddrLine1())
                .addressLine2(customer.getCustAddrLine2())
                .city(customer.getCustAddrLine3())
                .state(customer.getCustAddrStateCd())
                .zipCode(customer.getCustAddrZip())
                .country(customer.getCustAddrCountryCd())
                .phoneNumber1(customer.getCustPhoneNum1())
                .phoneNumber2(customer.getCustPhoneNum2())
                .govtIssuedId(customer.getCustGovtIssuedId())
                .eftAccountId(customer.getCustEftAccountId())
                .primaryCardHolderFlag(customer.getCustPriCardHolderInd())
                .build();
    }

    /**
     * Format a raw 9-character SSN string into NNN-NN-NNNN.
     *
     * <p>Preserves the COBOL STRING logic in paragraph 1200-SETUP-SCREEN-VARS:
     * <pre>
     *   STRING
     *       CUST-SSN(1:3) '-'
     *       CUST-SSN(4:2) '-'
     *       CUST-SSN(6:4)
     *       DELIMITED BY SIZE INTO ACSTSSNO
     * </pre>
     *
     * @param rawSsn 9-digit SSN (may be null or shorter than expected)
     * @return formatted SSN or the original value if it cannot be formatted
     */
    String formatSsn(String rawSsn) {
        if (rawSsn == null) {
            return null;
        }
        // Remove any existing dashes/spaces before applying the COBOL format
        String digits = rawSsn.replaceAll("[^0-9]", "");
        if (digits.length() != 9) {
            // Cannot format – return as-is (mirrors graceful COBOL fallback)
            return rawSsn.trim();
        }
        // CUST-SSN(1:3) + '-' + CUST-SSN(4:2) + '-' + CUST-SSN(6:4)
        return digits.substring(0, 3) + "-" + digits.substring(3, 5) + "-" + digits.substring(5, 9);
    }
}
