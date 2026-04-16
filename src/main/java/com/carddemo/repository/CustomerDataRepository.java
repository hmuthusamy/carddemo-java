package com.carddemo.repository;

import com.carddemo.model.CustomerData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for CustomerData entity (VSAM CUST KSDS).
 * Mapped from COBOL copybook CVCUS01Y.cpy
 */
@Repository
public interface CustomerDataRepository extends JpaRepository<CustomerData, Long> {
}
