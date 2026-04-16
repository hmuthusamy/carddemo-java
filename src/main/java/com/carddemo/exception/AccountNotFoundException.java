package com.carddemo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * AccountNotFoundException – thrown when an account cannot be located.
 *
 * Mirrors the COBOL error message conditions:
 *   DID-NOT-FIND-ACCT-IN-CARDXREF  → account not in cross-reference file
 *   DID-NOT-FIND-ACCT-IN-ACCTDAT   → account not in account master file
 *
 * The {@code @ResponseStatus(HttpStatus.NOT_FOUND)} annotation causes Spring MVC
 * to return HTTP 404 automatically when this exception propagates from a controller
 * (and is also handled explicitly in AccountViewController for structured JSON).
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class AccountNotFoundException extends RuntimeException {

    private final Long accountId;

    public AccountNotFoundException(Long accountId) {
        super("Account not found: " + accountId);
        this.accountId = accountId;
    }

    public AccountNotFoundException(Long accountId, String detail) {
        super("Account not found: " + accountId + " – " + detail);
        this.accountId = accountId;
    }

    public Long getAccountId() {
        return accountId;
    }
}
