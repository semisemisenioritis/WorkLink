package com.example.worklink.models;

import com.google.firebase.Timestamp;

public class Booking {
    private String bookingId;
    private String jobId;
    private String workerId;
    private String status; // "ACCEPTED", "COMPLETED", "TERMINATED"
    private Integer actualDays;
    private Timestamp checkInTime;
    private Timestamp checkOutTime;

    public Booking() {}

    public Booking(String jobId, String workerId) {
        this.jobId = jobId;
        this.workerId = workerId;
        this.status = "ACCEPTED";
    }

    // Getters and Setters
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getActualDays() { return actualDays; }
    public void setActualDays(Integer actualDays) { this.actualDays = actualDays; }
    public Timestamp getCheckInTime() { return checkInTime; }
    public void setCheckInTime(Timestamp checkInTime) { this.checkInTime = checkInTime; }
    public Timestamp getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(Timestamp checkOutTime) { this.checkOutTime = checkOutTime; }
}
