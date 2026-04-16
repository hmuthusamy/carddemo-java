package com.carddemo.controller;

import com.carddemo.dto.CardRequest;
import com.carddemo.model.CreditCard;
import com.carddemo.service.CardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    public ResponseEntity<CreditCard> createCard(@RequestBody CardRequest request) {
        CreditCard card = cardService.createCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditCard> getCard(@PathVariable Long id) {
        CreditCard card = cardService.getCard(id);
        return ResponseEntity.ok(card);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<CreditCard>> getCardsByAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(cardService.getCardsByAccount(accountId));
    }
}
