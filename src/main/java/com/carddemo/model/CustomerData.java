package com.carddemo.model;

/**
 * Maps to FD-CUSTFILE-REC / CUSTREC copybook.
 *
 * Key field: CUST-ID PIC X(09)
 * Notable fields used by CBSTM03A:
 *   CUST-FIRST-NAME  PIC X(25)
 *   CUST-MIDDLE-NAME PIC X(25)
 *   CUST-LAST-NAME   PIC X(25)
 *   CUST-ADDR-LINE-1 PIC X(50)
 *   CUST-ADDR-LINE-2 PIC X(50)
 *   CUST-ADDR-LINE-3 PIC X(50)
 *   CUST-ADDR-STATE-CD   PIC X(02)
 *   CUST-ADDR-COUNTRY-CD PIC X(03)
 *   CUST-ADDR-ZIP        PIC X(10)
 *   CUST-FICO-CREDIT-SCORE PIC 9(03)
 */
public class CustomerData {

    /** CUST-ID PIC X(09) */
    private String customerId;

    /** CUST-FIRST-NAME PIC X(25) */
    private String firstName;

    /** CUST-MIDDLE-NAME PIC X(25) */
    private String middleName;

    /** CUST-LAST-NAME PIC X(25) */
    private String lastName;

    /** CUST-ADDR-LINE-1 PIC X(50) */
    private String addressLine1;

    /** CUST-ADDR-LINE-2 PIC X(50) */
    private String addressLine2;

    /** CUST-ADDR-LINE-3 PIC X(50) */
    private String addressLine3;

    /** CUST-ADDR-STATE-CD PIC X(02) */
    private String stateCode;

    /** CUST-ADDR-COUNTRY-CD PIC X(03) */
    private String countryCode;

    /** CUST-ADDR-ZIP PIC X(10) */
    private String zipCode;

    /** CUST-FICO-CREDIT-SCORE PIC 9(03) */
    private int ficoScore;

    public CustomerData() {}

    public CustomerData(String customerId, String firstName, String middleName,
                        String lastName, String addressLine1, String addressLine2,
                        String addressLine3, String stateCode, String countryCode,
                        String zipCode, int ficoScore) {
        this.customerId = customerId;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressLine3 = addressLine3;
        this.stateCode = stateCode;
        this.countryCode = countryCode;
        this.zipCode = zipCode;
        this.ficoScore = ficoScore;
    }

    /** Returns full name as: firstName [middleName] lastName (mirrors COBOL STRING logic) */
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) sb.append(firstName.strip());
        if (middleName != null && !middleName.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(middleName.strip());
        }
        if (lastName != null && !lastName.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(lastName.strip());
        }
        return sb.toString();
    }

    /** Returns city/state/country/zip line as in ST-ADD3 (COBOL STRING logic) */
    public String getAddressLine3Full() {
        StringBuilder sb = new StringBuilder();
        if (addressLine3 != null && !addressLine3.isBlank()) sb.append(addressLine3.strip());
        if (stateCode != null && !stateCode.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(stateCode.strip());
        }
        if (countryCode != null && !countryCode.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(countryCode.strip());
        }
        if (zipCode != null && !zipCode.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(zipCode.strip());
        }
        return sb.toString();
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getAddressLine3() { return addressLine3; }
    public void setAddressLine3(String addressLine3) { this.addressLine3 = addressLine3; }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public int getFicoScore() { return ficoScore; }
    public void setFicoScore(int ficoScore) { this.ficoScore = ficoScore; }

    @Override
    public String toString() {
        return "CustomerData{id='" + customerId + "', name='" + getFullName() + "'}";
    }
}
