package com.example.worklink.worker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.worklink.R;
import com.example.worklink.api.ApiClient;
import com.example.worklink.api.SessionManager;
import com.example.worklink.api.models.WorkerProfile;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;

public class WorkerDashBoard extends AppCompatActivity {

    Switch availabilitySwitch;
    Button jobFeed, profile, earnings, btnResume;
    SessionManager sessionManager;
    int workerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_dashboard);

        sessionManager = new SessionManager(this);
        workerId = sessionManager.getUserId();

        availabilitySwitch = findViewById(R.id.switchAvailability);
        jobFeed = findViewById(R.id.btnJobFeed);
        profile = findViewById(R.id.btnProfile);
        earnings = findViewById(R.id.btnEarnings);
        btnResume = findViewById(R.id.btnViewResume);

        // Toggle availability
        availabilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // ADVANCED QUERY/ENDPOINT MISSING: Update specific field in worker profile
            // Placeholder: Using POST /work/worker-profile as a fallback if it supports updates
            WorkerProfile updateRequest = new WorkerProfile();
            updateRequest.setWorkerId(workerId);
            updateRequest.setAvailability(isChecked ? 1 : 0);
            
            ApiClient.getService(this).createWorkerProfile(updateRequest).enqueue(new Callback<WorkerProfile>() {
                @Override
                public void onResponse(Call<WorkerProfile> call, Response<WorkerProfile> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(WorkerDashBoard.this, "Availability Updated", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<WorkerProfile> call, Throwable t) {
                    Toast.makeText(WorkerDashBoard.this, "Failed to update availability", Toast.LENGTH_SHORT).show();
                }
            });
        });

        jobFeed.setOnClickListener(v ->
                startActivity(new Intent(this, JobFeedActivity.class))
        );

        profile.setOnClickListener(v ->
                startActivity(new Intent(this, WorkerProfileActivity.class))
        );

        earnings.setOnClickListener(v ->
                startActivity(new Intent(this, EarningsActivity.class))
        );

        btnResume.setOnClickListener(v -> {
            startActivity(new Intent(this, ResumeViewerActivity.class));
        });
    }
}
