package com.carddemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * CommAreaConfig - Java equivalent of the COBOL COMMAREA shared-state pattern.
 *
 * COBOL COMMAREA is passed between CICS program invocations via:
 *   EXEC CICS RETURN TRANSID('CARD') COMMAREA(WS-COMMAREA) END-EXEC
 * It carries session state between screen transitions.
 *
 * In Spring Boot (stateless REST), the COMMAREA is represented as:
 * 1. SessionContext - a per-request holder (populated from JWT/session token)
 * 2. Spring @Bean definitions for application-level CICS transaction constants
 */
@Configuration
public class CommAreaConfig {

    // COMMAREA layout constants (from COCOM01Y.cpy)
    public static final int    MAX_COMMAREA_LENGTH = 32767;   // PIC S9(4) COMP
    public static final int    TRAN_ID_LENGTH      = 4;       // PIC X(4)
    public static final int    PROGRAM_NAME_LENGTH = 8;       // PIC X(8)
    public static final int    USER_ID_LENGTH      = 8;       // PIC X(8)
    public static final int    ACCOUNT_ID_LENGTH   = 11;      // PIC 9(11)
    public static final int    CARD_NUMBER_LENGTH  = 16;      // PIC X(16)
    public static final int    RETURN_CODE_LENGTH  = 4;       // COBOL RETURN-CODE

    // CICS TRANSID values
    public static final String TRANSID_SIGN_ON     = "CC00";  // COSGN00C
    public static final String TRANSID_MAIN_MENU   = "CC01";  // COMEN01C
    public static final String TRANSID_ACCT_VIEW   = "CA00";  // COACTVWC
    public static final String TRANSID_ACCT_UPDATE = "CA01";  // COACTUPC
    public static final String TRANSID_CARD_LIST   = "CD00";  // COCRDSLC
    public static final String TRANSID_CARD_DETAIL = "CD01";  // COCRDLIC
    public static final String TRANSID_TXN_LIST    = "CT00";  // COTRN00C
    public static final String TRANSID_TXN_DETAIL  = "CT01";  // COTRN01C
    public static final String TRANSID_REPORT      = "CR00";  // CORPT00C
    public static final String TRANSID_ADMIN       = "CA99";  // COADM01C

    /**
     * SessionContext - per-request COMMAREA equivalent.
     * Carries the fields that COBOL stored in the COMMAREA between CICS interactions.
     * COMP-3 numeric values are represented as appropriate Java types.
     */
    public static class SessionContext {

        /** WS-USR-ID / CDEMO-FROM-USERID - PIC X(8). */
        private String userId = "";

        /** CDEMO-FROM-TRANID - PIC X(4). */
        private String fromTransId = "";

        /** CDEMO-TO-TRANID - PIC X(4). */
        private String toTransId = "";

        /** CDEMO-FROM-PROGRAM - PIC X(8). */
        private String fromProgram = "";

        /** CDEMO-TO-PROGRAM - PIC X(8). */
        private String toProgram = "";

        /** Current account ID - PIC 9(11). */
        private String accountId = "";

        /** Current card number - PIC X(16). */
        private String cardNumber = "";

        /** Last return code - mirrors COBOL RETURN-CODE. */
        private int returnCode = 0;

        /** Timestamp of last update (replaces WS-CURRENT-DATE fields). */
        private LocalDateTime lastUpdated = LocalDateTime.now();

        /** Carry-forward error message (WS-RETURN-MSG). */
        private String errorMessage = "";

        /** Application-level flags (WS-EDIT-DATE-FLGS, INPUT-ERROR etc.). */
        private final Map<String, Boolean> flags = new HashMap<>();

        public String        getUserId()       { return userId; }
        public void          setUserId(String v)         { this.userId = v; }
        public String        getFromTransId()  { return fromTransId; }
        public void          setFromTransId(String v)    { this.fromTransId = v; }
        public String        getToTransId()    { return toTransId; }
        public void          setToTransId(String v)      { this.toTransId = v; }
        public String        getFromProgram()  { return fromProgram; }
        public void          setFromProgram(String v)    { this.fromProgram = v; }
        public String        getToProgram()    { return toProgram; }
        public void          setToProgram(String v)      { this.toProgram = v; }
        public String        getAccountId()    { return accountId; }
        public void          setAccountId(String v)      { this.accountId = v; }
        public String        getCardNumber()   { return cardNumber; }
        public void          setCardNumber(String v)     { this.cardNumber = v; }
        public int           getReturnCode()   { return returnCode; }
        public void          setReturnCode(int v)        { this.returnCode = v; }
        public LocalDateTime getLastUpdated()  { return lastUpdated; }
        public void          setLastUpdated(LocalDateTime v) { this.lastUpdated = v; }
        public String        getErrorMessage() { return errorMessage; }
        public void          setErrorMessage(String v)   { this.errorMessage = v; }
        public Map<String,Boolean> getFlags()  { return flags; }

        /** Sets a named flag (88-level equivalent). */
        public void setFlag(String name, boolean value)  { flags.put(name, value); }

        /** Gets a named flag; defaults to false if not set. */
        public boolean getFlag(String name) { return flags.getOrDefault(name, false); }

        /** Resets all flags (INITIALIZE WS-EDIT-DATE-FLGS). */
        public void resetFlags() { flags.clear(); }

        /** true if returnCode == 0 (COBOL: RETURN-CODE = ZERO). */
        public boolean isSuccess() { return returnCode == 0; }

        @Override
        public String toString() {
            return String.format("SessionContext{userId='%s', from='%s', to='%s', rc=%d}",
                    userId, fromTransId, toTransId, returnCode);
        }
    }

    /** Prototype-scoped SessionContext bean - mirrors INITIALIZE COMMAREA. */
    @Bean
    public SessionContext sessionContext() {
        return new SessionContext();
    }

    /** Map of CICS TRANSID to program name (mirrors CSD definitions). */
    @Bean
    public Map<String, String> transactionProgramMap() {
        Map<String, String> map = new HashMap<>();
        map.put(TRANSID_SIGN_ON,     "COSGN00C");
        map.put(TRANSID_MAIN_MENU,   "COMEN01C");
        map.put(TRANSID_ACCT_VIEW,   "COACTVWC");
        map.put(TRANSID_ACCT_UPDATE, "COACTUPC");
        map.put(TRANSID_CARD_LIST,   "COCRDSLC");
        map.put(TRANSID_CARD_DETAIL, "COCRDLIC");
        map.put(TRANSID_TXN_LIST,    "COTRN00C");
        map.put(TRANSID_TXN_DETAIL,  "COTRN01C");
        map.put(TRANSID_REPORT,      "CORPT00C");
        map.put(TRANSID_ADMIN,       "COADM01C");
        return map;
    }
}
