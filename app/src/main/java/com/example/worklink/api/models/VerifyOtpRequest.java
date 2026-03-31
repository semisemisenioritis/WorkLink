package com.example.worklink.api.models;

public class VerifyOtpRequest {
    private String email;
    private String otp;
    private String password;

    public VerifyOtpRequest(String email, String otp, String password) {
        this.email = email;
        this.otp = otp;
        this.password = password;
    }

    public String getEmail() { return email; }
    public String getOtp() { return otp; }
    public String getPassword() { return password; }
}
