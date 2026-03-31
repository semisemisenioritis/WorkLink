package com.example.worklink.worker;

import android.os.Bundle;
import android.text.TextUtils;
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
import java.util.Arrays;
import java.util.List;

public class WorkerProfileActivity extends AppCompatActivity {

    EditText etSkills, etExperience;
    TextView tvCurrentSkills, tvCurrentExperience;
    Button btnSave;
    SessionManager sessionManager;
    int workerId;
    WorkerProfile currentProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_profile);

        sessionManager = new SessionManager(this);
        workerId = sessionManager.getUserId();

        etSkills = findViewById(R.id.etSkills);
        etExperience = findViewById(R.id.etExperience);
        tvCurrentSkills = findViewById(R.id.tvCurrentSkills);
        tvCurrentExperience = findViewById(R.id.tvCurrentExperience);
        btnSave = findViewById(R.id.btnSave);

        loadProfileData();

        btnSave.setOnClickListener(v -> {
            String newSkillsStr = etSkills.getText().toString().trim();
            String newExp = etExperience.getText().toString().trim();

            if (TextUtils.isEmpty(newSkillsStr) || TextUtils.isEmpty(newExp)) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> updatedSkills = new ArrayList<>();
            if (currentProfile != null && currentProfile.getSkills() != null) {
                updatedSkills.addAll(currentProfile.getSkills());
            }
            updatedSkills.addAll(Arrays.asList(newSkillsStr.split("\\s*,\\s*")));

            WorkerProfile updateRequest = new WorkerProfile();
            updateRequest.setWorkerId(workerId);
            updateRequest.setSkills(updatedSkills);
            updateRequest.setExperience(Integer.parseInt(newExp));

            // ADVANCED QUERY/ENDPOINT MISSING: Update specific fields in worker profile
            // Placeholder: Using POST /work/worker-profile as a fallback if it supports updates
            ApiClient.getService(this).createWorkerProfile(updateRequest).enqueue(new Callback<WorkerProfile>() {
                @Override
                public void onResponse(Call<WorkerProfile> call, Response<WorkerProfile> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(WorkerProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                        etSkills.setText("");
                        etExperience.setText("");
                        loadProfileData();
                    } else {
                        Toast.makeText(WorkerProfileActivity.this, "Update Failed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<WorkerProfile> call, Throwable t) {
                    Toast.makeText(WorkerProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void loadProfileData() {
        ApiClient.getService(this).getWorkerProfiles().enqueue(new Callback<List<WorkerProfile>>() {
            @Override
            public void onResponse(Call<List<WorkerProfile>> call, Response<List<WorkerProfile>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (WorkerProfile profile : response.body()) {
                        if (profile.getWorkerId() == workerId) {
                            currentProfile = profile;
                            String skills = (profile.getSkills() == null || profile.getSkills().isEmpty()) 
                                    ? "None" : String.join(", ", profile.getSkills());
                            tvCurrentSkills.setText("Skills: " + skills);
                            tvCurrentExperience.setText("Experience: " + profile.getExperience() + " years");
                            break;
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<WorkerProfile>> call, Throwable t) {
                Toast.makeText(WorkerProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
