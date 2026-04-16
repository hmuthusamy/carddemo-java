package com.carddemo.controller;

import com.carddemo.dto.TransactionRequest;
import com.carddemo.model.Transaction;
import com.carddemo.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@RequestBody TransactionRequest request) {
        Transaction transaction = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<Transaction>> getTransactionsByAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccount(accountId));
    }

    @GetMapping("/account/{accountId}/balance")
    public ResponseEntity<Map<String, BigDecimal>> getBalance(@PathVariable Long accountId) {
        BigDecimal balance = transactionService.getAccountBalance(accountId);
        return ResponseEntity.ok(Map.of("balance", balance));
    }
}
