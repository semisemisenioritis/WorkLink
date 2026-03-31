package com.example.worklink.api.models;

public class WorkUserRequest {
    private String name;
    private String email;
    private String password;
    private String role;

    public WorkUserRequest(String name, String email, String password, String role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // Getters and Setters
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
}
