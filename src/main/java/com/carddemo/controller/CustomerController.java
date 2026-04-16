package com.carddemo.controller;

import com.carddemo.model.CustomerData;
import com.carddemo.service.CustomerService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * CustomerController – REST API migrated from COBOL/CICS COUCUS00.
 *
 * <p>Original COUCUS00 operations mapped to HTTP endpoints:
 * <pre>
 *  COUCUSIN  (READ CUSTFILE by CUST-ID)          → GET  /api/customers/{customerId}
 *  STARTBR / search screen                       → GET  /api/customers?search={term}
 *  COUCUS00 new-customer add (WRITE CUSTFILE)    → POST /api/customers
 *  COUCUSUPD (REWRITE CUSTFILE)                  → PUT  /api/customers/{customerId}
 * </pre>
 *
 * <p>All customer fields are mapped from the {@link CustomerData} entity, which
 * mirrors the CVCUS01Y COBOL copybook (CUST-ID through CUST-FICO-CREDIT-SCORE).
 */
@Slf4j
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // =========================================================================
    // GET /api/customers/{customerId}
    // COBOL equivalent: COUCUSIN – EXEC CICS READ CUSTFILE RIDFLD(CUST-ID)
    // =========================================================================

    /**
     * Retrieve a single customer by their 9-digit customer ID.
     *
     * @param customerId 9-digit customer identifier (CUST-ID PIC 9(09))
     * @return 200 OK with customer data, or 404 Not Found
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerData> getCustomerById(
            @PathVariable Long customerId) {

        log.info("GET /api/customers/{} – COUCUSIN customer inquiry", customerId);
        return customerService.findById(customerId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer not found: id=" + customerId));
    }

    // =========================================================================
    // GET /api/customers?search={term}
    // COBOL equivalent: COUCUS00 STARTBR / browse with optional name filter
    // =========================================================================

    /**
     * List customers, optionally filtered by a search term.
     *
     * <p>When {@code search} is omitted, all customers are returned (sequential
     * browse in COBOL terms).  When provided, the search is applied across
     * CUST-FIRST-NAME, CUST-MIDDLE-NAME, CUST-LAST-NAME, and
     * CUST-GOVT-ISSUED-ID.
     *
     * @param search optional free-text search term
     * @return 200 OK with list of matching customers
     */
    @GetMapping
    public ResponseEntity<List<CustomerData>> searchCustomers(
            @RequestParam(name = "search", required = false) String search) {

        log.info("GET /api/customers?search='{}' – COUCUS00 customer search", search);
        List<CustomerData> results = (search != null && !search.isBlank())
                ? customerService.searchCustomers(search)
                : customerService.findAll();
        return ResponseEntity.ok(results);
    }

    // =========================================================================
    // POST /api/customers
    // COBOL equivalent: COUCUS00 – EXEC CICS WRITE CUSTFILE FROM(CUSTOMER-RECORD)
    // =========================================================================

    /**
     * Create a new customer record.
     *
     * <p>Validates that a customer with the provided ID does not already exist –
     * mirroring the COUCUS00 DUPKEY check before WRITE.
     *
     * @param customer customer payload; all CVCUS01Y fields accepted
     * @return 201 Created with the persisted customer, or 409 Conflict if duplicate
     */
    @PostMapping
    @Transactional
    public ResponseEntity<CustomerData> createCustomer(
            @Valid @RequestBody CustomerData customer) {

        log.info("POST /api/customers – COUCUS00 create customer id={}",
                customer.getCustomerId());
        try {
            CustomerData created = customerService.createCustomer(customer);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }
    }

    // =========================================================================
    // PUT /api/customers/{customerId}
    // COBOL equivalent: COUCUSUPD – EXEC CICS REWRITE CUSTFILE FROM(CUSTOMER-RECORD)
    // =========================================================================

    /**
     * Update an existing customer record (full replacement).
     *
     * <p>Mirrors COUCUSUPD: reads the existing record, overlays all mutable
     * fields from the request body, then rewrites the record.
     *
     * @param customerId path variable matching CUST-ID PIC 9(09)
     * @param customer   updated customer data
     * @return 200 OK with updated customer, or 404 Not Found
     */
    @PutMapping("/{customerId}")
    @Transactional
    public ResponseEntity<CustomerData> updateCustomer(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerData customer) {

        log.info("PUT /api/customers/{} – COUCUSUPD update customer", customerId);
        try {
            CustomerData updated = customerService.updateCustomer(customerId, customer);
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }
}
