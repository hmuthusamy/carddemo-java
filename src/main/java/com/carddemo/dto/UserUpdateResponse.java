package com.carddemo.dto;

/**
 * Response DTO returned after a successful or failed update.
 * Mirrors the WS-MESSAGE / ERRMSGO pattern in COUSR02C.
 */
public class UserUpdateResponse {

    private String userId;
    private String message;
    private boolean success;

    public UserUpdateResponse() {}

    public UserUpdateResponse(String userId, String message, boolean success) {
        this.userId  = userId;
        this.message = message;
        this.success = success;
    }

    public String  getUserId()  { return userId; }
    public void    setUserId(String userId) { this.userId = userId; }

    public String  getMessage() { return message; }
    public void    setMessage(String message) { this.message = message; }

    public boolean isSuccess()  { return success; }
    public void    setSuccess(boolean success) { this.success = success; }
}
