package com.carddemo.service;

import com.carddemo.dto.AccountRequest;
import com.carddemo.model.Account;
import com.carddemo.model.Customer;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public AccountService(AccountRepository accountRepository, CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    public Account createAccount(AccountRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found: " + request.getCustomerId()));

        String accountNumber = generateAccountNumber();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .customer(customer)
                .creditLimit(request.getCreditLimit())
                .currentBalance(BigDecimal.ZERO)
                .availableCredit(request.getCreditLimit())
                .accountType(request.getAccountType() != null ? request.getAccountType() : "CREDIT")
                .status("ACTIVE")
                .build();

        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsByCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));
        return accountRepository.findByCustomer(customer);
    }

    public Account updateBalance(Long accountId, BigDecimal newBalance) {
        Account account = getAccount(accountId);
        account.setCurrentBalance(newBalance);
        if (account.getCreditLimit() != null) {
            account.setAvailableCredit(account.getCreditLimit().subtract(newBalance));
        }
        return accountRepository.save(account);
    }

    private String generateAccountNumber() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        // Ensure uniqueness
        while (accountRepository.existsByAccountNumber(uuid)) {
            uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        }
        return uuid;
    }
}
