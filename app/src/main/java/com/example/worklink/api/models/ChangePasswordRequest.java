package com.example.worklink.api.models;

public class ChangePasswordRequest {
    private String old_password;
    private String new_password;

    public ChangePasswordRequest(String old_password, String new_password) {
        this.old_password = old_password;
        this.new_password = new_password;
    }

    public String getOldPassword() { return old_password; }
    public String getNewPassword() { return new_password; }
}
