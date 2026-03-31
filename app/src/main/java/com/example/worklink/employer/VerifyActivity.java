package com.example.worklink.employer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.worklink.R;
import com.example.worklink.api.ApiClient;
import com.example.worklink.api.SessionManager;
import com.example.worklink.api.models.Booking;
import com.example.worklink.api.models.Job;
import com.example.worklink.api.models.WorkUser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VerifyActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> bookingDisplayList;
    List<Booking> activeBookings;
    Map<Integer, Job> jobsMap;
    Map<Integer, WorkUser> usersMap;
    SessionManager sessionManager;
    int employerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_job_feed); // Reusing layout for simple list

        sessionManager = new SessionManager(this);
        employerId = sessionManager.getUserId();

        listView = findViewById(R.id.jobListView);
        bookingDisplayList = new ArrayList<>();
        activeBookings = new ArrayList<>();
        jobsMap = new HashMap<>();
        usersMap = new HashMap<>();
        
        loadData();

        listView.setOnItemClickListener((p, v, pos, id) -> {
            Booking booking = activeBookings.get(pos);
            Job job = jobsMap.get(booking.getJobId());

            // ADVANCED QUERY/ENDPOINT MISSING: Update specific booking status
            // Placeholder: Marking it locally and proceeding
            booking.setStatus("COMPLETED");

            Toast.makeText(this, "Marked Completed. Proceeding to Payment.", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, PaymentRatingActivity.class);
            intent.putExtra("bookingId", booking.getBookingId());
            intent.putExtra("workerId", booking.getWorkerId());
            intent.putExtra("amount", job != null ? job.getWage() : 0.0);
            startActivity(intent);
            finish();
        });
    }

    private void loadData() {
        // This is complex as we need to fetch bookings, then filter by jobs that belong to this employer.
        // We'll fetch jobs and bookings in parallel or sequence.
        ApiClient.getService(this).getJobs().enqueue(new Callback<List<Job>>() {
            @Override
            public void onResponse(Call<List<Job>> call, Response<List<Job>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Job job : response.body()) {
                        if (job.getEmployerId() == employerId) {
                            jobsMap.put(job.getJobId(), job);
                        }
                    }
                    fetchUsersAndBookings();
                }
            }

            @Override
            public void onFailure(Call<List<Job>> call, Throwable t) {
                Toast.makeText(VerifyActivity.this, "Failed to load jobs", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUsersAndBookings() {
        ApiClient.getService(this).getWorkUsers().enqueue(new Callback<List<WorkUser>>() {
            @Override
            public void onResponse(Call<List<WorkUser>> call, Response<List<WorkUser>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (WorkUser user : response.body()) {
                        usersMap.put(user.getId(), user);
                    }
                    fetchBookings();
                }
            }

            @Override
            public void onFailure(Call<List<WorkUser>> call, Throwable t) {
                Toast.makeText(VerifyActivity.this, "Failed to load users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchBookings() {
        ApiClient.getService(this).getBookings().enqueue(new Callback<List<Booking>>() {
            @Override
            public void onResponse(Call<List<Booking>> call, Response<List<Booking>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    activeBookings.clear();
                    bookingDisplayList.clear();
                    for (Booking booking : response.body()) {
                        if (jobsMap.containsKey(booking.getJobId()) && "ACCEPTED".equalsIgnoreCase(booking.getStatus())) {
                            activeBookings.add(booking);

                            WorkUser worker = usersMap.get(booking.getWorkerId());
                            Job job = jobsMap.get(booking.getJobId());

                            String display = "Worker: " + (worker != null ? worker.getName() : "Unknown") +
                                             "\nJob: " + (job != null ? job.getTitle() : "Unknown") +
                                             "\nWage: ₹" + (job != null ? job.getWage() : 0.0);
                            bookingDisplayList.add(display);
                        }
                    }
                    listView.setAdapter(new ArrayAdapter<>(VerifyActivity.this,
                            android.R.layout.simple_list_item_1, bookingDisplayList));

                    if (bookingDisplayList.isEmpty()) {
                        Toast.makeText(VerifyActivity.this, "No active bookings to verify.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Booking>> call, Throwable t) {
                Toast.makeText(VerifyActivity.this, "Failed to load bookings", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
