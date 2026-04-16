package com.carddemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Property-binding configuration that maps every JCL DD NAME= statement to a
 * typed Spring property, injected from {@code application.yml}.
 *
 * <p><b>Mapping table (JCL DD → Spring property → default path):</b>
 * <pre>
 * JCL Job         Step      DD NAME     DSN (mainframe)                          Spring property
 * -------------- --------- ----------- ---------------------------------------- -------------------------------------------------
 * POSTTRAN        STEP15    TRANFILE    AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS       spring.batch.tranfile.path
 * POSTTRAN        STEP15    DALYTRAN    AWS.M2.CARDDEMO.DALYTRAN.PS              spring.batch.dalytran.path
 * POSTTRAN        STEP15    XREFFILE    AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS       spring.batch.xreffile.path
 * POSTTRAN        STEP15    DALYREJS    AWS.M2.CARDDEMO.DALYREJS(+1)             spring.batch.dalyrejs.path
 * POSTTRAN        STEP15    ACCTFILE    AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS       spring.batch.acctfile.path
 * POSTTRAN        STEP15    TCATBALF    AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS       spring.batch.tcatbalf.path
 * INTCALC         STEP15    TCATBALF    AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS       spring.batch.tcatbalf.path
 * INTCALC         STEP15    XREFFILE    AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS       spring.batch.xreffile.path
 * INTCALC         STEP15    XREFFIL1    AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX.PATH   spring.batch.xreffil1.path
 * INTCALC         STEP15    ACCTFILE    AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS       spring.batch.acctfile.path
 * INTCALC         STEP15    DISCGRP     AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS        spring.batch.discgrp.path
 * INTCALC         STEP15    TRANSACT    AWS.M2.CARDDEMO.SYSTRAN(+1)              spring.batch.transact.path
 * TRANREPT        STEP10R   TRANFILE    AWS.M2.CARDDEMO.TRANSACT.DALY(+1)        spring.batch.tranfile.path
 * TRANREPT        STEP10R   CARDXREF    AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS       spring.batch.cardxref.path
 * TRANREPT        STEP10R   TRANTYPE    AWS.M2.CARDDEMO.TRANTYPE.VSAM.KSDS       spring.batch.trantype.path
 * TRANREPT        STEP10R   TRANCATG    AWS.M2.CARDDEMO.TRANCATG.VSAM.KSDS       spring.batch.trancatg.path
 * TRANREPT        STEP10R   DATEPARM    AWS.M2.CARDDEMO.DATEPARM                 spring.batch.dateparm.path
 * TRANREPT        STEP10R   TRANREPT    AWS.M2.CARDDEMO.TRANREPT(+1)             spring.batch.tranrept.path
 * CREASTMT        STEP040   TRNXFILE    AWS.M2.CARDDEMO.TRXFL.VSAM.KSDS          spring.batch.trnxfile.path
 * CREASTMT        STEP040   XREFFILE    AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS       spring.batch.xreffile.path
 * CREASTMT        STEP040   ACCTFILE    AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS       spring.batch.acctfile.path
 * CREASTMT        STEP040   CUSTFILE    AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS       spring.batch.custfile.path
 * CREASTMT        STEP040   STMTFILE    AWS.M2.CARDDEMO.STATEMNT.PS              spring.batch.stmtfile.path
 * CREASTMT        STEP040   HTMLFILE    AWS.M2.CARDDEMO.STATEMNT.HTML            spring.batch.htmlfile.path
 * CBEXPORT        STEP02    CUSTFILE    AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS       spring.batch.custfile.path
 * CBEXPORT        STEP02    ACCTFILE    AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS       spring.batch.acctfile.path
 * CBEXPORT        STEP02    XREFFILE    AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS       spring.batch.xreffile.path
 * CBEXPORT        STEP02    TRANSACT    AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS       spring.batch.transact.path
 * CBEXPORT        STEP02    CARDFILE    AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS       spring.batch.cardfile.path
 * CBEXPORT        STEP02    EXPFILE     AWS.M2.CARDDEMO.EXPORT.DATA              spring.batch.expfile.path
 * CBIMPORT        STEP01    EXPFILE     AWS.M2.CARDDEMO.EXPORT.DATA              spring.batch.expfile.path
 * CBIMPORT        STEP01    CUSTOUT     AWS.M2.CARDDEMO.CUSTDATA.IMPORT          spring.batch.custout.path
 * CBIMPORT        STEP01    ACCTOUT     AWS.M2.CARDDEMO.ACCTDATA.IMPORT          spring.batch.acctout.path
 * CBIMPORT        STEP01    XREFOUT     AWS.M2.CARDDEMO.CARDXREF.IMPORT          spring.batch.xrefout.path
 * CBIMPORT        STEP01    TRNXOUT     AWS.M2.CARDDEMO.TRANSACT.IMPORT          spring.batch.trnxout.path
 * CBIMPORT        STEP01    ERROUT      AWS.M2.CARDDEMO.IMPORT.ERRORS            spring.batch.errout.path
 * </pre>
 *
 * <p>All properties have sensible local-filesystem defaults so the application
 * starts without external configuration.  Override them in application.yml or
 * via environment variables for real environments.
 */
@Configuration
public class BatchConfig {

    // -----------------------------------------------------------------------
    // POSTTRAN.jcl  – STEP15 EXEC PGM=CBTRN02C
    // -----------------------------------------------------------------------

    /**
     * JCL DD NAME=TRANFILE → DSN=AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS
     * (also reused by TRANREPT STEP10R EXEC PGM=CBTRN03C)
     */
    @Value("${spring.batch.tranfile.path:/data/carddemo/transact.dat}")
    private String tranfilePath;

    /**
     * JCL DD NAME=DALYTRAN → DSN=AWS.M2.CARDDEMO.DALYTRAN.PS
     */
    @Value("${spring.batch.dalytran.path:/data/carddemo/dalytran.dat}")
    private String dalytranPath;

    /**
     * JCL DD NAME=DALYREJS → DSN=AWS.M2.CARDDEMO.DALYREJS(+1)   (output GDG)
     */
    @Value("${spring.batch.dalyrejs.path:/data/carddemo/output/dalyrejs.dat}")
    private String dalyrjsPath;

    // -----------------------------------------------------------------------
    // INTCALC.jcl  – STEP15 EXEC PGM=CBACT04C
    // -----------------------------------------------------------------------

    /**
     * JCL DD NAME=TCATBALF → DSN=AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS
     * (shared with POSTTRAN STEP15)
     */
    @Value("${spring.batch.tcatbalf.path:/data/carddemo/tcatbalf.dat}")
    private String tcatbalfPath;

    /**
     * JCL DD NAME=XREFFILE → DSN=AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS
     * (shared across POSTTRAN / INTCALC / CREASTMT / CBEXPORT)
     */
    @Value("${spring.batch.xreffile.path:/data/carddemo/cardxref.dat}")
    private String xreffilePath;

    /**
     * JCL DD NAME=XREFFIL1 → DSN=AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX.PATH
     * (alternate-index path, INTCALC only)
     */
    @Value("${spring.batch.xreffil1.path:/data/carddemo/cardxref-aix.dat}")
    private String xreffil1Path;

    /**
     * JCL DD NAME=ACCTFILE → DSN=AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS
     * (shared across POSTTRAN / INTCALC / CREASTMT / CBEXPORT)
     */
    @Value("${spring.batch.acctfile.path:/data/carddemo/acctdata.dat}")
    private String acctfilePath;

    /**
     * JCL DD NAME=DISCGRP → DSN=AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS
     * (INTCALC only)
     */
    @Value("${spring.batch.discgrp.path:/data/carddemo/discgrp.dat}")
    private String discgrpPath;

    /**
     * JCL DD NAME=TRANSACT → DSN=AWS.M2.CARDDEMO.SYSTRAN(+1)  (output GDG, INTCALC)
     * Also input for CBEXPORT STEP02
     */
    @Value("${spring.batch.transact.path:/data/carddemo/transact-vsam.dat}")
    private String transactPath;

    // -----------------------------------------------------------------------
    // TRANREPT.jcl  – STEP10R EXEC PGM=CBTRN03C
    // -----------------------------------------------------------------------

    /**
     * JCL DD NAME=CARDXREF → DSN=AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS
     */
    @Value("${spring.batch.cardxref.path:/data/carddemo/cardxref.dat}")
    private String cardxrefPath;

    /**
     * JCL DD NAME=TRANTYPE → DSN=AWS.M2.CARDDEMO.TRANTYPE.VSAM.KSDS
     */
    @Value("${spring.batch.trantype.path:/data/carddemo/trantype.dat}")
    private String trantypePath;

    /**
     * JCL DD NAME=TRANCATG → DSN=AWS.M2.CARDDEMO.TRANCATG.VSAM.KSDS
     */
    @Value("${spring.batch.trancatg.path:/data/carddemo/trancatg.dat}")
    private String trancatgPath;

    /**
     * JCL DD NAME=DATEPARM → DSN=AWS.M2.CARDDEMO.DATEPARM
     */
    @Value("${spring.batch.dateparm.path:/data/carddemo/dateparm.dat}")
    private String dateparmPath;

    /**
     * JCL DD NAME=TRANREPT → DSN=AWS.M2.CARDDEMO.TRANREPT(+1)  (output GDG)
     */
    @Value("${spring.batch.tranrept.path:/data/carddemo/output/tranrept.dat}")
    private String tranreptPath;

    // -----------------------------------------------------------------------
    // CREASTMT.JCL  – STEP040 EXEC PGM=CBSTM03A
    // -----------------------------------------------------------------------

    /**
     * JCL DD NAME=TRNXFILE → DSN=AWS.M2.CARDDEMO.TRXFL.VSAM.KSDS
     */
    @Value("${spring.batch.trnxfile.path:/data/carddemo/trxfl.dat}")
    private String trnxfilePath;

    /**
     * JCL DD NAME=CUSTFILE → DSN=AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS
     * (shared with CBEXPORT)
     */
    @Value("${spring.batch.custfile.path:/data/carddemo/custdata.dat}")
    private String custfilePath;

    /**
     * JCL DD NAME=STMTFILE → DSN=AWS.M2.CARDDEMO.STATEMNT.PS  (output)
     */
    @Value("${spring.batch.stmtfile.path:/data/carddemo/output/statemnt.txt}")
    private String stmtfilePath;

    /**
     * JCL DD NAME=HTMLFILE → DSN=AWS.M2.CARDDEMO.STATEMNT.HTML  (output)
     */
    @Value("${spring.batch.htmlfile.path:/data/carddemo/output/statemnt.html}")
    private String htmlfilePath;

    // -----------------------------------------------------------------------
    // CBEXPORT.jcl  – STEP02 EXEC PGM=CBEXPORT
    // -----------------------------------------------------------------------

    /**
     * JCL DD NAME=CARDFILE → DSN=AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS
     */
    @Value("${spring.batch.cardfile.path:/data/carddemo/carddata.dat}")
    private String cardfilePath;

    /**
     * JCL DD NAME=EXPFILE → DSN=AWS.M2.CARDDEMO.EXPORT.DATA
     * (output for CBEXPORT, input for CBIMPORT)
     */
    @Value("${spring.batch.expfile.path:/data/carddemo/export.dat}")
    private String expfilePath;

    // -----------------------------------------------------------------------
    // CBIMPORT.jcl  – STEP01 EXEC PGM=CBIMPORT
    // -----------------------------------------------------------------------

    /**
     * JCL DD NAME=CUSTOUT → DSN=AWS.M2.CARDDEMO.CUSTDATA.IMPORT  (output)
     */
    @Value("${spring.batch.custout.path:/data/carddemo/output/custdata-import.dat}")
    private String custoutPath;

    /**
     * JCL DD NAME=ACCTOUT → DSN=AWS.M2.CARDDEMO.ACCTDATA.IMPORT  (output)
     */
    @Value("${spring.batch.acctout.path:/data/carddemo/output/acctdata-import.dat}")
    private String acctoutPath;

    /**
     * JCL DD NAME=XREFOUT → DSN=AWS.M2.CARDDEMO.CARDXREF.IMPORT  (output)
     */
    @Value("${spring.batch.xrefout.path:/data/carddemo/output/cardxref-import.dat}")
    private String xrefoutPath;

    /**
     * JCL DD NAME=TRNXOUT → DSN=AWS.M2.CARDDEMO.TRANSACT.IMPORT  (output)
     */
    @Value("${spring.batch.trnxout.path:/data/carddemo/output/transact-import.dat}")
    private String trnxoutPath;

    /**
     * JCL DD NAME=ERROUT → DSN=AWS.M2.CARDDEMO.IMPORT.ERRORS  (output)
     */
    @Value("${spring.batch.errout.path:/data/carddemo/output/import-errors.dat}")
    private String erroutPath;

    // -----------------------------------------------------------------------
    // Getters – used by service layer and unit tests
    // -----------------------------------------------------------------------

    public String getTranfilePath()   { return tranfilePath; }
    public String getDalytranPath()   { return dalytranPath; }
    public String getDalyrjsPath()    { return dalyrjsPath; }
    public String getTcatbalfPath()   { return tcatbalfPath; }
    public String getXreffilePath()   { return xreffilePath; }
    public String getXreffil1Path()   { return xreffil1Path; }
    public String getAcctfilePath()   { return acctfilePath; }
    public String getDiscgrpPath()    { return discgrpPath; }
    public String getTransactPath()   { return transactPath; }
    public String getCardxrefPath()   { return cardxrefPath; }
    public String getTrantypePath()   { return trantypePath; }
    public String getTrancatgPath()   { return trancatgPath; }
    public String getDateparmPath()   { return dateparmPath; }
    public String getTranreptPath()   { return tranreptPath; }
    public String getTrnxfilePath()   { return trnxfilePath; }
    public String getCustfilePath()   { return custfilePath; }
    public String getStmtfilePath()   { return stmtfilePath; }
    public String getHtmlfilePath()   { return htmlfilePath; }
    public String getCardfilePath()   { return cardfilePath; }
    public String getExpfilePath()    { return expfilePath; }
    public String getCustoutPath()    { return custoutPath; }
    public String getAcctoutPath()    { return acctoutPath; }
    public String getXrefoutPath()    { return xrefoutPath; }
    public String getTrnxoutPath()    { return trnxoutPath; }
    public String getErroutPath()     { return erroutPath; }
}
