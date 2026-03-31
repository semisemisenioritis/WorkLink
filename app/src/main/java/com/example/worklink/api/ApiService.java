package com.example.worklink.api;

import com.example.worklink.api.models.*;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @POST("auth/send-otp")
    Call<MessageResponse> sendOtp(@Body OtpRequest request);

    @POST("auth/verify-otp-register")
    Call<MessageResponse> verifyOtpRegister(@Body VerifyOtpRequest request);

    @POST("auth/login")
    Call<AuthResponse> login(@Body AuthRequest request);

    @GET("user/profile/")
    Call<UserProfile> getUserProfile();

    @POST("user/change-password/")
    Call<MessageResponse> changePassword(@Body ChangePasswordRequest request);

    @POST("work/users/")
    Call<WorkUser> createWorkUser(@Body WorkUserRequest request);

    @GET("work/users/")
    Call<List<WorkUser>> getWorkUsers();

    @DELETE("work/users/{user_id}/")
    Call<MessageResponse> deleteWorkUser(@Path("user_id") int userId);

    @POST("work/worker-profile/")
    Call<WorkerProfile> createWorkerProfile(@Body WorkerProfile request);

    @GET("work/worker-profile/")
    Call<List<WorkerProfile>> getWorkerProfiles();

    @GET("work/workers/search/")
    Call<List<WorkerSearchResponse>> searchWorkers(@Query("skill") String skill);

    @GET("work/workers/{worker_id}/stats/")
    Call<WorkerStatsResponse> getWorkerStats(@Path("worker_id") int workerId);

    @POST("work/employer-profile/")
    Call<EmployerProfile> createEmployerProfile(@Body EmployerProfile request);

    @GET("work/employer-profile/")
    Call<List<EmployerProfile>> getEmployerProfiles();

    @POST("work/jobs/")
    Call<Job> createJob(@Body Job request);

    @GET("work/jobs/")
    Call<List<Job>> getJobs();

    @DELETE("work/jobs/{job_id}/")
    Call<MessageResponse> deleteJob(@Path("job_id") int jobId);

    @POST("work/bookings/")
    Call<Booking> createBooking(@Body Booking request);

    @GET("work/bookings/")
    Call<List<Booking>> getBookings();

    @DELETE("work/bookings/{booking_id}/")
    Call<MessageResponse> deleteBooking(@Path("booking_id") int bookingId);

    @POST("work/ratings/")
    Call<Rating> createRating(@Body Rating request);

    @GET("work/ratings/")
    Call<List<Rating>> getRatings();

    @POST("work/payments/")
    Call<Payment> createPayment(@Body Payment request);

    @GET("work/payments/")
    Call<List<Payment>> getPayments();
}
