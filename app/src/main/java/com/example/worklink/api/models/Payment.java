package com.example.worklink.api.models;

public class Payment {
    private int payment_id;
    private int booking_id;
    private Double amount;
    private String payment_status;
    private String payment_method;
    private String payment_date;

    // Getters and Setters
    public int getPaymentId() { return payment_id; }
    public void setPaymentId(int paymentId) { this.payment_id = paymentId; }
    public int getBookingId() { return booking_id; }
    public void setBookingId(int bookingId) { this.booking_id = bookingId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getPaymentStatus() { return payment_status; }
    public void setPaymentStatus(String paymentStatus) { this.payment_status = paymentStatus; }
    public String getPaymentMethod() { return payment_method; }
    public void setPaymentMethod(String paymentMethod) { this.payment_method = paymentMethod; }
    public String getPaymentDate() { return payment_date; }
    public void setPaymentDate(String paymentDate) { this.payment_date = paymentDate; }
}
