package com.example.worklink.api.models;

import java.util.List;

public class WorkerStatsResponse {
    private int worker_id;
    private WorkUser user;
    private WorkerProfile profile;
    private Stats stats;
    private List<Rating> recent_ratings;

    public static class Stats {
        private int total_ratings_received;
        private double average_rating;
        private int total_bookings;
        private int completed_jobs;
        private double total_earnings_calculated;
        private double total_earnings_from_profile;

        // Getters
        public int getTotalRatingsReceived() { return total_ratings_received; }
        public double getAverageRating() { return average_rating; }
        public int getTotalBookings() { return total_bookings; }
        public int getCompletedJobs() { return completed_jobs; }
        public double getTotalEarningsCalculated() { return total_earnings_calculated; }
        public double getTotalEarningsFromProfile() { return total_earnings_from_profile; }
    }

    // Getters
    public int getWorkerId() { return worker_id; }
    public WorkUser getUser() { return user; }
    public WorkerProfile getProfile() { return profile; }
    public Stats getStats() { return stats; }
    public List<Rating> getRecentRatings() { return recent_ratings; }
}
