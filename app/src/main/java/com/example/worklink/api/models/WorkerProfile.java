package com.example.worklink.api.models;

import java.util.List;

public class WorkerProfile {
    private int worker_id;
    private List<String> skills;
    private Integer experience;
    private Integer availability;
    private Double rating;
    private Integer total_jobs;
    private Double total_earnings;

    // Getters and Setters
    public int getWorkerId() { return worker_id; }
    public void setWorkerId(int workerId) { this.worker_id = workerId; }
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
    public Integer getExperience() { return experience; }
    public void setExperience(Integer experience) { this.experience = experience; }
    public Integer getAvailability() { return availability; }
    public void setAvailability(Integer availability) { this.availability = availability; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Integer getTotalJobs() { return total_jobs; }
    public void setTotalJobs(Integer totalJobs) { this.total_jobs = totalJobs; }
    public Double getTotalEarnings() { return total_earnings; }
    public void setTotalEarnings(Double totalEarnings) { this.total_earnings = totalEarnings; }
}
