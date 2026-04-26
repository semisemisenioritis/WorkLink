package com.example.worklink.models;

import com.google.firebase.Timestamp;

public class Rating {
    private String ratingId;
    private String bookingId;
    private String givenBy;
    private String givenTo;
    private Integer rating;
    private String review;
    private Timestamp createdAt;

    public Rating() {}

    public String getRatingId() { return ratingId; }
    public void setRatingId(String ratingId) { this.ratingId = ratingId; }
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public String getGivenBy() { return givenBy; }
    public void setGivenBy(String givenBy) { this.givenBy = givenBy; }
    public String getGivenTo() { return givenTo; }
    public void setGivenTo(String givenTo) { this.givenTo = givenTo; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
