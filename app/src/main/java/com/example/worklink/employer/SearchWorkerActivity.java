package com.example.worklink.employer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.worklink.R;
import com.example.worklink.api.ApiClient;
import com.example.worklink.api.SessionManager;
import com.example.worklink.api.models.Booking;
import com.example.worklink.api.models.Job;
import com.example.worklink.api.models.WorkerSearchResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class SearchWorkerActivity extends AppCompatActivity {

    EditText skill;
    Button search;
    ListView listView;
    ArrayList<String> workerDisplayList;
    List<WorkerSearchResponse> searchResults;
    SessionManager sessionManager;
    int employerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.employer_activity_search_worker);

        sessionManager = new SessionManager(this);
        employerId = sessionManager.getUserId();

        skill = findViewById(R.id.etSkill);
        search = findViewById(R.id.btnSearch);
        listView = findViewById(R.id.listWorkers);

        workerDisplayList = new ArrayList<>();
        searchResults = new ArrayList<>();

        search.setOnClickListener(v -> {
            String skillText = skill.getText().toString().trim();
            if (skillText.isEmpty()) {
                Toast.makeText(this, "Enter a skill to search", Toast.LENGTH_SHORT).show();
                return;
            }

            ApiClient.getService(this).searchWorkers(skillText).enqueue(new Callback<List<WorkerSearchResponse>>() {
                @Override
                public void onResponse(Call<List<WorkerSearchResponse>> call, Response<List<WorkerSearchResponse>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        searchResults = response.body();
                        workerDisplayList.clear();
                        for (WorkerSearchResponse res : searchResults) {
                            String data = res.getUser().getName() + " | " +
                                    String.join(", ", res.getProfile().getSkills()) + " | ⭐" +
                                    res.getProfile().getRating();
                            workerDisplayList.add(data);
                        }
                        listView.setAdapter(new ArrayAdapter<>(SearchWorkerActivity.this,
                                android.R.layout.simple_list_item_1, workerDisplayList));
                    }
                }

                @Override
                public void onFailure(Call<List<WorkerSearchResponse>> call, Throwable t) {
                    Toast.makeText(SearchWorkerActivity.this, "Search failed", Toast.LENGTH_SHORT).show();
                }
            });
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            WorkerSearchResponse selected = searchResults.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Employ Worker")
                    .setMessage("Do you want to employ " + selected.getUser().getName() + "?")
                    .setPositiveButton("Yes", (dialog, which) -> findJobAndEmploy(selected.getUser().getId()))
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void findJobAndEmploy(int workerId) {
        // Fetch all jobs and find the latest open one for this employer
        ApiClient.getService(this).getJobs().enqueue(new Callback<List<Job>>() {
            @Override
            public void onResponse(Call<List<Job>> call, Response<List<Job>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Job latestJob = null;
                    for (Job job : response.body()) {
                        if (job.getEmployerId() == employerId && "OPEN".equalsIgnoreCase(job.getStatus())) {
                            latestJob = job;
                            // In a real app we'd sort by date, here we just take the last matching one
                        }
                    }

                    if (latestJob != null) {
                        createBooking(latestJob.getJobId(), workerId);
                    } else {
                        Toast.makeText(SearchWorkerActivity.this, "Please post a job first!", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Job>> call, Throwable t) {
                Toast.makeText(SearchWorkerActivity.this, "Error finding job", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createBooking(int jobId, int workerId) {
        Booking booking = new Booking();
        booking.setJobId(jobId);
        booking.setWorkerId(workerId);
        booking.setStatus("ACCEPTED");

        ApiClient.getService(this).createBooking(booking).enqueue(new Callback<Booking>() {
            @Override
            public void onResponse(Call<Booking> call, Response<Booking> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SearchWorkerActivity.this, "Worker Employed!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SearchWorkerActivity.this, "Employment failed: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Booking> call, Throwable t) {
                Toast.makeText(SearchWorkerActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
