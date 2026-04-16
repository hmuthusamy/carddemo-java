package com.carddemo.controller;

import com.carddemo.model.Statement;
import com.carddemo.service.StatementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statements")
public class StatementController {

    private final StatementService statementService;

    public StatementController(StatementService statementService) {
        this.statementService = statementService;
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateStatements() {
        int count = statementService.generateStatementsForAllAccounts();
        return ResponseEntity.ok(Map.of(
                "message", "Statements generated successfully",
                "count", count
        ));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<Statement>> getStatements(@PathVariable Long accountId) {
        return ResponseEntity.ok(statementService.getStatementsForAccount(accountId));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCount() {
        long count = statementService.countGeneratedStatements();
        return ResponseEntity.ok(Map.of("generatedStatements", count));
    }
}
