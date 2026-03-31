package com.example.worklink.worker;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.worklink.R;
import com.example.worklink.api.ApiClient;
import com.example.worklink.api.SessionManager;
import com.example.worklink.api.models.WorkerStatsResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.File;

public class ResumeViewerActivity extends AppCompatActivity {

    TextView tvPreview;
    Button btnDownload;
    SessionManager sessionManager;
    int workerId;
    WorkerStatsResponse currentStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_resume_viewer);

        sessionManager = new SessionManager(this);
        workerId = sessionManager.getUserId();

        tvPreview = findViewById(R.id.tvResumePreview);
        btnDownload = findViewById(R.id.btnDownloadPdf);

        loadResumePreview();

        btnDownload.setOnClickListener(v -> {
            if (currentStats != null) {
                ResumeBuilder builder = new ResumeBuilder(this);
                File file = builder.generateResume(currentStats);
                if (file != null) {
                    Toast.makeText(this, "Resume Downloaded: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Data still loading...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadResumePreview() {
        ApiClient.getService(this).getWorkerStats(workerId).enqueue(new Callback<WorkerStatsResponse>() {
            @Override
            public void onResponse(Call<WorkerStatsResponse> call, Response<WorkerStatsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentStats = response.body();
                    updatePreview(currentStats);
                } else {
                    Toast.makeText(ResumeViewerActivity.this, "Failed to load resume data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WorkerStatsResponse> call, Throwable t) {
                Toast.makeText(ResumeViewerActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePreview(WorkerStatsResponse stats) {
        StringBuilder preview = new StringBuilder();
        preview.append("NAME: ").append(stats.getUser().getName()).append("\n\n");
        preview.append("EMAIL: ").append(stats.getUser().getEmail()).append("\n\n");
        preview.append("EXPERIENCE: ").append(stats.getProfile().getExperience()).append(" years\n\n");
        preview.append("OVERALL RATING: ").append(stats.getProfile().getRating()).append(" / 5.0\n\n");
        preview.append("SKILLS:\n");
        
        if (stats.getProfile().getSkills() != null) {
            for (String skill : stats.getProfile().getSkills()) {
                preview.append("• ").append(skill.trim()).append("\n");
            }
        } else {
            preview.append("• None\n");
        }

        tvPreview.setText(preview.toString());
    }
}
