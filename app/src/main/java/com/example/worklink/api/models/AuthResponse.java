package com.example.worklink.api.models;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName(value = "access_token", alternate = {"access"})
    private String access_token;
    
    @SerializedName(value = "refresh_token", alternate = {"refresh"})
    private String refresh_token;

    public String getAccessToken() { return access_token; }
    public String getRefreshToken() { return refresh_token; }
}
