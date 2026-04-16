package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CSMSG01Y.cpy
 * CCDA-COMMON-MESSAGES - Common message constants.
 * Not a VSAM record - UI message data, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageData {

    /** CCDA-MSG-THANK-YOU PIC X(50) */
    private String ccdaMsgThankYou;

    /** CCDA-MSG-INVALID-KEY PIC X(50) */
    private String ccdaMsgInvalidKey;
}
