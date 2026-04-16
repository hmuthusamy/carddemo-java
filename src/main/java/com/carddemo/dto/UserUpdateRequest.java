package com.carddemo.dto;

/**
 * DTO carrying the updatable fields from the COUSR2A BMS map.
 *
 * Maps screen fields from COUSR02C.CBL:
 *   FNAMEI    → firstName
 *   LNAMEI    → lastName
 *   PASSWDI   → password
 *   USRTYPEI  → userType
 *
 * All fields are optional strings; null/blank triggers the same
 * validation error messages as the original EVALUATE branches.
 */
public class UserUpdateRequest {

    private String firstName;
    private String lastName;
    private String password;
    private String userType;

    public UserUpdateRequest() {}

    public UserUpdateRequest(String firstName, String lastName,
                             String password, String userType) {
        this.firstName = firstName;
        this.lastName  = lastName;
        this.password  = password;
        this.userType  = userType;
    }

    public String getFirstName() { return firstName; }
    public void   setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName()  { return lastName; }
    public void   setLastName(String lastName)   { this.lastName = lastName; }

    public String getPassword()  { return password; }
    public void   setPassword(String password)   { this.password = password; }

    public String getUserType()  { return userType; }
    public void   setUserType(String userType)   { this.userType = userType; }
}
