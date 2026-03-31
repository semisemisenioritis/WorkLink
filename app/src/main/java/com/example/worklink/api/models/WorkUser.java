package com.example.worklink.api.models;

public class WorkUser {
    private int id;
    private String name;
    private String email;
    private String username;
    private String role;
    private String created_at;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getCreatedAt() { return created_at; }
}
