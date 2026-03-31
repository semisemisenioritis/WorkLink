package com.example.worklink.api.models;

public class EmployerProfile {
    private int employer_id;
    private String company_name;
    private String location;
    private Double rating;
    private Integer total_jobs_posted;

    public int getEmployerId() { return employer_id; }
    public void setEmployerId(int employerId) { this.employer_id = employerId; }
    public String getCompanyName() { return company_name; }
    public void setCompanyName(String companyName) { this.company_name = companyName; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Integer getTotalJobsPosted() { return total_jobs_posted; }
    public void setTotalJobsPosted(Integer totalJobsPosted) { this.total_jobs_posted = totalJobsPosted; }
}
