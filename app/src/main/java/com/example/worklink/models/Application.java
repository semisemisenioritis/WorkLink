package com.example.worklink.models;

import com.google.firebase.Timestamp;

public class Application {
    private String applicationId;
    private String jobId;
    private String workerId;
    private String status; // "pending", "accepted", "rejected"
    private Timestamp appliedAt;

    public Application() {}

    public Application(String jobId, String workerId) {
        this.jobId = jobId;
        this.workerId = workerId;
        this.status = "pending";
        this.appliedAt = Timestamp.now();
    }

    // Getters and Setters
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Timestamp appliedAt) { this.appliedAt = appliedAt; }
}
