package com.example.worklink.api.models;

public class Job {
    private int job_id;
    private int employer_id;
    private String title;
    private String description;
    private String location;
    private Double wage;
    private String required_skills;
    private String job_date;
    private String status;
    private Integer num_workers;
    private Integer workers_assigned;
    private String created_at;

    // Getters and Setters
    public int getJobId() { return job_id; }
    public void setJobId(int jobId) { this.job_id = jobId; }
    public int getEmployerId() { return employer_id; }
    public void setEmployerId(int employerId) { this.employer_id = employerId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Double getWage() { return wage; }
    public void setWage(Double wage) { this.wage = wage; }
    public String getRequiredSkills() { return required_skills; }
    public void setRequiredSkills(String requiredSkills) { this.required_skills = requiredSkills; }
    public String getJobDate() { return job_date; }
    public void setJobDate(String jobDate) { this.job_date = jobDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getNumWorkers() { return num_workers; }
    public void setNumWorkers(Integer numWorkers) { this.num_workers = numWorkers; }
    public Integer getWorkersAssigned() { return workers_assigned; }
    public void setWorkersAssigned(Integer workersAssigned) { this.workers_assigned = workersAssigned; }
    public String getCreatedAt() { return created_at; }
}
