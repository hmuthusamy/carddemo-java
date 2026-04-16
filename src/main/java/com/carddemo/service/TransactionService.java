package com.carddemo.service;

import com.carddemo.dto.TransactionRequest;
import com.carddemo.model.Account;
import com.carddemo.model.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository,
                               AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    public Transaction createTransaction(TransactionRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found: " + request.getAccountId()));

        // Update account balance
        BigDecimal newBalance;
        if ("PURCHASE".equalsIgnoreCase(request.getTransactionType())) {
            newBalance = account.getCurrentBalance().add(request.getAmount());
        } else if ("PAYMENT".equalsIgnoreCase(request.getTransactionType())) {
            newBalance = account.getCurrentBalance().subtract(request.getAmount());
        } else {
            newBalance = account.getCurrentBalance().add(request.getAmount());
        }

        account.setCurrentBalance(newBalance);
        if (account.getCreditLimit() != null) {
            account.setAvailableCredit(account.getCreditLimit().subtract(newBalance));
        }
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .account(account)
                .amount(request.getAmount())
                .transactionType(request.getTransactionType().toUpperCase())
                .description(request.getDescription())
                .merchantName(request.getMerchantName())
                .merchantCategory(request.getMerchantCategory())
                .status("COMPLETED")
                .build();

        return transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        return transactionRepository.findByAccountOrderByTransactionDateDesc(account);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        return account.getCurrentBalance();
    }
}
