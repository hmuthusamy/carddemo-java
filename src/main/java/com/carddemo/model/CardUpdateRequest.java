package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input DTO for the CBACT02C batch job.
 *
 * <p>Represents one update transaction record read by the flat-file
 * {@link org.springframework.batch.item.file.FlatFileItemReader}.
 * Each field maps to a fixed-column position in the input file,
 * mirroring the COBOL record layout derived from CVACT02Y.
 *
 * <p>Field layout (total width 78 characters):
 * <pre>
 *  Cols  1-16 : cardNum           (CARD-NUM        PIC X(16))
 *  Cols 17-27 : cardAcctId        (CARD-ACCT-ID    PIC 9(11))
 *  Cols 28-30 : cardCvvCd         (CARD-CVV-CD     PIC 9(03))
 *  Cols 31-80 : cardEmbossedName  (CARD-EMBOSSED-NAME PIC X(50))
 *  Cols 81-90 : cardExpirationDate(CARD-EXPIRAION-DATE PIC X(10))
 *  Col  91    : cardActiveStatus  (CARD-ACTIVE-STATUS  PIC X(01))
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardUpdateRequest {

    private String cardNum;
    private Long   cardAcctId;
    private Integer cardCvvCd;
    private String cardEmbossedName;
    private String cardExpirationDate;
    private String cardActiveStatus;
}
