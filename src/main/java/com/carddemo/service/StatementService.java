package com.carddemo.service;

import com.carddemo.model.Account;
import com.carddemo.model.Statement;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.StatementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class StatementService {

    private final StatementRepository statementRepository;
    private final AccountRepository accountRepository;

    public StatementService(StatementRepository statementRepository,
                            AccountRepository accountRepository) {
        this.statementRepository = statementRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Generate statements for all active accounts.
     * This simulates what the batch job would do.
     * Returns the number of statements generated.
     */
    public int generateStatementsForAllAccounts() {
        List<Account> accounts = accountRepository.findAll();
        List<Statement> statements = new ArrayList<>();

        for (Account account : accounts) {
            if ("ACTIVE".equals(account.getStatus())) {
                BigDecimal balance = account.getCurrentBalance() != null
                        ? account.getCurrentBalance() : BigDecimal.ZERO;

                Statement statement = Statement.builder()
                        .account(account)
                        .statementDate(LocalDate.now())
                        .openingBalance(BigDecimal.ZERO)
                        .closingBalance(balance)
                        .totalCharges(balance.compareTo(BigDecimal.ZERO) > 0 ? balance : BigDecimal.ZERO)
                        .totalCredits(BigDecimal.ZERO)
                        .minimumPayment(balance.multiply(new BigDecimal("0.02")))
                        .dueDate(LocalDate.now().plusDays(25))
                        .status("GENERATED")
                        .build();

                statements.add(statement);
            }
        }

        statementRepository.saveAll(statements);
        return statements.size();
    }

    @Transactional(readOnly = true)
    public List<Statement> getStatementsForAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        return statementRepository.findByAccountOrderByStatementDateDesc(account);
    }

    @Transactional(readOnly = true)
    public long countGeneratedStatements() {
        return statementRepository.countByStatus("GENERATED");
    }
}
