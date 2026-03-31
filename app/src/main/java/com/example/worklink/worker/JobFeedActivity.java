package com.example.worklink.worker;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.worklink.R;
import com.example.worklink.api.ApiClient;
import com.example.worklink.api.SessionManager;
import com.example.worklink.api.models.Booking;
import com.example.worklink.api.models.Job;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class JobFeedActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> jobsDisplayList;
    List<Job> jobsList;
    ArrayAdapter<String> adapter;
    SessionManager sessionManager;
    int workerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_job_feed);

        listView = findViewById(R.id.jobListView);
        sessionManager = new SessionManager(this);
        workerId = sessionManager.getUserId();
        
        jobsDisplayList = new ArrayList<>();
        jobsList = new ArrayList<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, jobsDisplayList);
        listView.setAdapter(adapter);

        fetchJobs();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < jobsList.size()) {
                Job selectedJob = jobsList.get(position);
                applyForJob(selectedJob.getJobId());
            }
        });
    }

    private void fetchJobs() {
        ApiClient.getService(this).getJobs().enqueue(new Callback<List<Job>>() {
            @Override
            public void onResponse(Call<List<Job>> call, Response<List<Job>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    jobsList.clear();
                    jobsDisplayList.clear();
                    for (Job job : response.body()) {
                        if ("OPEN".equalsIgnoreCase(job.getStatus())) {
                            jobsList.add(job);
                            jobsDisplayList.add(job.getTitle() + " - ₹" + job.getWage());
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Job>> call, Throwable t) {
                Toast.makeText(JobFeedActivity.this, "Failed to fetch jobs", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyForJob(int jobId) {
        Booking booking = new Booking();
        booking.setJobId(jobId);
        booking.setWorkerId(workerId);
        booking.setStatus("PENDING");

        ApiClient.getService(this).createBooking(booking).enqueue(new Callback<Booking>() {
            @Override
            public void onResponse(Call<Booking> call, Response<Booking> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(JobFeedActivity.this, "Applied for Job", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(JobFeedActivity.this, "Already applied or Job full", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Booking> call, Throwable t) {
                Toast.makeText(JobFeedActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
