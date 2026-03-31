package com.example.worklink.api.models;

public class Rating {
    private int rating_id;
    private int booking_id;
    private int given_by;
    private int given_to;
    private int rating;
    private String review;
    private String created_at;

    // Getters and Setters
    public int getRatingId() { return rating_id; }
    public void setRatingId(int ratingId) { this.rating_id = ratingId; }
    public int getBookingId() { return booking_id; }
    public void setBookingId(int bookingId) { this.booking_id = bookingId; }
    public int getGivenBy() { return given_by; }
    public void setGivenBy(int givenBy) { this.given_by = givenBy; }
    public int getGivenTo() { return given_to; }
    public void setGivenTo(int givenTo) { this.given_to = givenTo; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }
    public String getCreatedAt() { return created_at; }
}
