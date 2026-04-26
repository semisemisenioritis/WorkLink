package com.example.worklink.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.FirestoreManager;
import com.example.worklink.R;
import com.example.worklink.models.Booking;
import com.example.worklink.models.Job;
import com.example.worklink.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class ResumeViewerActivity extends AppCompatActivity {

    TextView tvPreview;
    Button btnDownload;
    ImageButton btnBack;
    String workerId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_resume_viewer);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        workerId = sharedPreferences.getString("userId", "");

        if (workerId.isEmpty() && FirebaseAuth.getInstance().getCurrentUser() != null) {
            workerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        btnBack = findViewById(R.id.btnBack);
        tvPreview = findViewById(R.id.tvResumePreview);
        btnDownload = findViewById(R.id.btnDownloadPdf);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        loadResumePreview();

        btnDownload.setOnClickListener(v -> {
            ResumeBuilder builder = new ResumeBuilder(this);
            builder.generateResume(workerId, new ResumeBuilder.ResumeCallback() {
                @Override
                public void onSuccess(File file) {
                    Toast.makeText(ResumeViewerActivity.this, "Resume Downloaded!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(ResumeViewerActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void loadResumePreview() {
        FirestoreManager.getInstance().getUser(workerId).addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            User user = doc.toObject(User.class);
            
            StringBuilder preview = new StringBuilder();
            preview.append("NAME: ").append(user.getName()).append("\n");
            preview.append("CONTACT: ").append(user.getPhone()).append("\n");
            preview.append("RATING: ⭐ ").append(String.format(Locale.US, "%.1f", user.getWorkerRating() != null ? user.getWorkerRating() : 0.0)).append("\n\n");
            
            preview.append("SKILLS:\n");
            String skills = user.getSkills() != null ? user.getSkills() : "None";
            for (String skill : skills.split(",")) {
                preview.append(" • ").append(skill.trim()).append("\n");
            }
            preview.append("\nEXPERIENCE:\n");

            db.collection("bookings")
                .whereEqualTo("workerId", workerId)
                .whereIn("status", List.of("COMPLETED", "TERMINATED"))
                .get()
                .addOnSuccessListener(snaps -> {
                    if (snaps.isEmpty()) {
                        preview.append(" No verified work history yet.");
                    } else {
                        for (QueryDocumentSnapshot bDoc : snaps) {
                            Booking b = bDoc.toObject(Booking.class);
                            preview.append("■ Job ID: ").append(b.getJobId()).append("\n");
                            preview.append("  Status: ").append(b.getStatus()).append("\n");
                            preview.append("  Days Worked: ").append(b.getActualDays() != null ? b.getActualDays() : "N/A").append("\n\n");
                        }
                    }
                    tvPreview.setText(preview.toString());
                });
        });
    }
}
