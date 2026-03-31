package com.example.worklink.api.models;

public class Booking {
    private int booking_id;
    private int job_id;
    private int worker_id;
    private String status;
    private String check_in_time;
    private String check_out_time;

    // Getters and Setters
    public int getBookingId() { return booking_id; }
    public void setBookingId(int bookingId) { this.booking_id = bookingId; }
    public int getJobId() { return job_id; }
    public void setJobId(int jobId) { this.job_id = jobId; }
    public int getWorkerId() { return worker_id; }
    public void setWorkerId(int workerId) { this.worker_id = workerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCheckInTime() { return check_in_time; }
    public void setCheckInTime(String checkInTime) { this.check_in_time = checkInTime; }
    public String getCheckOutTime() { return check_out_time; }
    public void setCheckOutTime(String checkOutTime) { this.check_out_time = checkOutTime; }
}
