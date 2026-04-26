package com.example.worklink.models;

import com.google.firebase.Timestamp;

public class User {
    private String id; // This will be the Firebase Auth UID
    private String name;
    private String phone;
    private String username;
    private String role; // "Worker" or "Employer"
    private Timestamp createdAt;

    // Worker Profile Fields
    private String skills;
    private Integer experience;
    private Integer availability;
    private Double workerRating;
    private Integer totalJobs;

    // Employer Profile Fields
    private String companyName;
    private String location;
    private Double employerRating;
    private Integer totalJobsPosted;

    // Required empty constructor for Firestore
    public User() {}

    // Basic constructor for registration
    public User(String id, String name, String phone, String username, String role) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.username = username;
        this.role = role;
        this.createdAt = Timestamp.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    // Worker Getters/Setters
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public Integer getExperience() { return experience; }
    public void setExperience(Integer experience) { this.experience = experience; }
    public Integer getAvailability() { return availability; }
    public void setAvailability(Integer availability) { this.availability = availability; }
    public Double getWorkerRating() { return workerRating; }
    public void setWorkerRating(Double workerRating) { this.workerRating = workerRating; }
    public Integer getTotalJobs() { return totalJobs; }
    public void setTotalJobs(Integer totalJobs) { this.totalJobs = totalJobs; }

    // Employer Getters/Setters
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Double getEmployerRating() { return employerRating; }
    public void setEmployerRating(Double employerRating) { this.employerRating = employerRating; }
    public Integer getTotalJobsPosted() { return totalJobsPosted; }
    public void setTotalJobsPosted(Integer totalJobsPosted) { this.totalJobsPosted = totalJobsPosted; }
}
