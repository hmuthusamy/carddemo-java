package com.carddemo.repository;

import com.carddemo.model.CustomerData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * CustomerRepository – Spring Data JPA repository for {@link CustomerData}.
 *
 * Replaces the COBOL EXEC CICS READ statement in paragraph 9400-GETCUSTDATA-BYCUST:
 *
 * <pre>
 *   EXEC CICS READ
 *        DATASET   (LIT-CUSTFILENAME)     -- 'CUSTDAT '
 *        RIDFLD    (WS-CARD-RID-CUST-ID-X)
 *        INTO      (CUSTOMER-RECORD)
 *        RESP      (WS-RESP-CD)
 *   END-EXEC
 * </pre>
 */
@Repository
public interface CustomerRepository extends JpaRepository<CustomerData, Long> {
}
