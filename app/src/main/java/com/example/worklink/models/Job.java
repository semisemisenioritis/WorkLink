package com.example.worklink.models;

import com.google.firebase.Timestamp;

public class Job {
    private String jobId;
    private String employerId;
    private String title;
    private String description;
    private String location;
    private Double wage;
    private String requiredSkills;
    private Integer workersNeeded;
    private Integer durationDays;
    private String jobDate; // Added to match old schema
    private String status; // "OPEN", "CLOSED"
    private Timestamp createdAt;

    public Job() {}

    public Job(String employerId, String title, String description, String location, Double wage) {
        this.employerId = employerId;
        this.title = title;
        this.description = description;
        this.location = location;
        this.wage = wage;
        this.status = "OPEN";
        this.createdAt = Timestamp.now();
    }

    // Getters and Setters
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getEmployerId() { return employerId; }
    public void setEmployerId(String employerId) { this.employerId = employerId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Double getWage() { return wage; }
    public void setWage(Double wage) { this.wage = wage; }
    public String getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }
    public Integer getWorkersNeeded() { return workersNeeded; }
    public void setWorkersNeeded(Integer workersNeeded) { this.workersNeeded = workersNeeded; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
    public String getJobDate() { return jobDate; }
    public void setJobDate(String jobDate) { this.jobDate = jobDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
