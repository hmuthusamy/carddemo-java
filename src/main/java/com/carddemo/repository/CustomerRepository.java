package com.carddemo.repository;

import com.carddemo.model.CustomerData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for CustomerData.
 *
 * Replaces VSAM KSDS file access from CVCUS01Y copybook / CBCUS01C batch
 * and COUCUS00 CICS customer management operations.
 */
@Repository
public interface CustomerRepository extends JpaRepository<CustomerData, Long> {

    /**
     * Search customers by first name, middle name, last name, or government ID.
     * Mirrors the COUCUS00 customer-search screen logic.
     *
     * @param term search term (case-insensitive substring)
     * @return list of matching customers
     */
    @Query("""
            SELECT c FROM CustomerData c
            WHERE LOWER(c.firstName)    LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(c.middleName)   LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(c.lastName)     LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(c.govtIssuedId) LIKE LOWER(CONCAT('%', :term, '%'))
            ORDER BY c.lastName, c.firstName
            """)
    List<CustomerData> searchByTerm(@Param("term") String term);

    /**
     * Find all customers whose last name starts with the given prefix.
     *
     * @param prefix last-name prefix
     * @return list of customers
     */
    List<CustomerData> findByLastNameStartingWithIgnoreCaseOrderByLastNameAscFirstNameAsc(String prefix);
}
