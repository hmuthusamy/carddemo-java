package com.carddemo.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * AccountUpdateResponse – REST response envelope for PUT /api/accounts/{accountId}.
 *
 * Mirrors the COBOL DFHCOMMAREA WS-RETURN-MSG / WS-ERRMSG pattern:
 *   - success=true  →  account updated, return full account snapshot
 *   - success=false →  validation / not-found errors, return messages list
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountUpdateResponse {

    /** true = COBOL "UPDATE SUCCESSFUL", false = COBOL "WS-ERRMSG populated". */
    private boolean success;

    /** Human-readable result message (mirrors WS-RETURN-MSG). */
    private String message;

    /** Validation / error details (mirrors COBOL error flag list). */
    private List<String> errors;

    /* ── Snapshot of the persisted account (only when success=true) ─────── */

    private String accountId;
    private String activeStatus;
    private BigDecimal currentBalance;
    private BigDecimal creditLimit;
    private BigDecimal cashCreditLimit;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate openDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reissueDate;

    private BigDecimal currCycCredit;
    private BigDecimal currCycDebit;
    private String addrZip;
    private String groupId;

    /* ── Factory helpers ─────────────────────────────────────────────────── */

    public static AccountUpdateResponse fromEntity(AccountData account) {
        return AccountUpdateResponse.builder()
                .success(true)
                .message("Account updated successfully")
                .accountId(account.getAccountId())
                .activeStatus(account.getActiveStatus())
                .currentBalance(account.getCurrentBalance())
                .creditLimit(account.getCreditLimit())
                .cashCreditLimit(account.getCashCreditLimit())
                .openDate(account.getOpenDate())
                .expirationDate(account.getExpirationDate())
                .reissueDate(account.getReissueDate())
                .currCycCredit(account.getCurrCycCredit())
                .currCycDebit(account.getCurrCycDebit())
                .addrZip(account.getAddrZip())
                .groupId(account.getGroupId())
                .build();
    }

    public static AccountUpdateResponse error(String message, List<String> errors) {
        return AccountUpdateResponse.builder()
                .success(false)
                .message(message)
                .errors(errors)
                .build();
    }

    public static AccountUpdateResponse notFound(String accountId) {
        return AccountUpdateResponse.builder()
                .success(false)
                .message("Account not found: " + accountId)
                .build();
    }
}
