package com.carddemo.service;

import com.carddemo.model.CustomerData;
import com.carddemo.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * CustomerService – business logic layer migrated from COUCUS00 CICS program.
 *
 * <p>Original COBOL COUCUS00 supported:
 * <ul>
 *   <li>COUCUSIN  – customer inquiry by ID</li>
 *   <li>COUCUSUPD – customer update</li>
 *   <li>Customer search by name across VSAM KSDS CUSTFILE</li>
 * </ul>
 *
 * <p>This service provides an idiomatic Java equivalent backed by JPA/PostgreSQL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    // -------------------------------------------------------------------------
    // READ operations  (COUCUSIN equivalent)
    // -------------------------------------------------------------------------

    /**
     * Retrieve a customer by their 9-digit customer ID.
     * Equivalent to COUCUS00 READ CUSTFILE RIDFLD(CUST-ID).
     *
     * @param customerId 9-digit customer identifier
     * @return Optional containing the customer if found
     */
    @Transactional(readOnly = true)
    public Optional<CustomerData> findById(Long customerId) {
        log.debug("COUCUSIN – fetching customer id={}", customerId);
        return customerRepository.findById(customerId);
    }

    /**
     * Search customers by a free-text term across name and government ID fields.
     * Mirrors the COUCUS00 browse/search screen (STARTBR CUSTFILE).
     *
     * @param term search term
     * @return list of matching customer records
     */
    @Transactional(readOnly = true)
    public List<CustomerData> searchCustomers(String term) {
        log.debug("COUCUS00 – searching customers term='{}'", term);
        if (term == null || term.isBlank()) {
            return customerRepository.findAll();
        }
        return customerRepository.searchByTerm(term.trim());
    }

    /**
     * Return all customers (STARTBR with no filter).
     *
     * @return all customer records
     */
    @Transactional(readOnly = true)
    public List<CustomerData> findAll() {
        log.debug("COUCUS00 – listing all customers");
        return customerRepository.findAll();
    }

    // -------------------------------------------------------------------------
    // CREATE operation  (COUCUS00 new customer add)
    // -------------------------------------------------------------------------

    /**
     * Create a new customer record.
     * Equivalent to COUCUS00 WRITE CUSTFILE FROM(CUSTOMER-RECORD).
     *
     * @param customer customer data to persist
     * @return the saved customer with generated/assigned ID
     * @throws IllegalArgumentException if a customer with that ID already exists
     */
    @Transactional
    public CustomerData createCustomer(CustomerData customer) {
        log.info("COUCUS00 – creating customer id={}", customer.getCustomerId());
        if (customer.getCustomerId() != null &&
                customerRepository.existsById(customer.getCustomerId())) {
            throw new IllegalArgumentException(
                    "Customer already exists with id=" + customer.getCustomerId());
        }
        CustomerData saved = customerRepository.save(customer);
        log.info("COUCUS00 – customer created id={}", saved.getCustomerId());
        return saved;
    }

    // -------------------------------------------------------------------------
    // UPDATE operation  (COUCUSUPD equivalent)
    // -------------------------------------------------------------------------

    /**
     * Update an existing customer record in full.
     * Equivalent to COUCUSUPD REWRITE CUSTFILE FROM(CUSTOMER-RECORD).
     *
     * @param customerId path variable / record key
     * @param updated    new field values
     * @return the updated customer record
     * @throws jakarta.persistence.EntityNotFoundException if customer not found
     */
    @Transactional
    public CustomerData updateCustomer(Long customerId, CustomerData updated) {
        log.info("COUCUSUPD – updating customer id={}", customerId);
        CustomerData existing = customerRepository.findById(customerId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Customer not found with id=" + customerId));

        // Map all mutable fields (COUCUSUPD copybook fields)
        existing.setFirstName(updated.getFirstName());
        existing.setMiddleName(updated.getMiddleName());
        existing.setLastName(updated.getLastName());
        existing.setAddrLine1(updated.getAddrLine1());
        existing.setAddrLine2(updated.getAddrLine2());
        existing.setAddrLine3(updated.getAddrLine3());
        existing.setAddrStateCd(updated.getAddrStateCd());
        existing.setAddrCountryCd(updated.getAddrCountryCd());
        existing.setAddrZip(updated.getAddrZip());
        existing.setPhoneNum1(updated.getPhoneNum1());
        existing.setPhoneNum2(updated.getPhoneNum2());
        existing.setSsn(updated.getSsn());
        existing.setGovtIssuedId(updated.getGovtIssuedId());
        existing.setDateOfBirth(updated.getDateOfBirth());
        existing.setEftAccountId(updated.getEftAccountId());
        existing.setPrimaryCardHolderInd(updated.getPrimaryCardHolderInd());
        existing.setFicoCreditScore(updated.getFicoCreditScore());

        CustomerData saved = customerRepository.save(existing);
        log.info("COUCUSUPD – customer updated id={}", saved.getCustomerId());
        return saved;
    }

    // -------------------------------------------------------------------------
    // Existence check helper
    // -------------------------------------------------------------------------

    /**
     * Check whether a customer with the given ID exists.
     *
     * @param customerId customer identifier
     * @return true if present
     */
    @Transactional(readOnly = true)
    public boolean exists(Long customerId) {
        return customerRepository.existsById(customerId);
    }
}
