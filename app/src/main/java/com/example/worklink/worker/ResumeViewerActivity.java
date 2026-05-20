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
import com.example.worklink.models.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
                        tvPreview.setText(preview.toString());
                    } else {
                        List<Task<DocumentSnapshot>> jobTasks = new ArrayList<>();
                        List<Booking> completedBookings = new ArrayList<>();
                        
                        for (QueryDocumentSnapshot bDoc : snaps) {
                            Booking b = bDoc.toObject(Booking.class);
                            completedBookings.add(b);
                            jobTasks.add(db.collection("jobs").document(b.getJobId()).get());
                        }

                        Tasks.whenAllSuccess(jobTasks).addOnSuccessListener(jobDocs -> {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                            for (int i = 0; i < completedBookings.size(); i++) {
                                Booking b = completedBookings.get(i);
                                DocumentSnapshot jobDoc = (DocumentSnapshot) jobDocs.get(i);
                                String jobTitle = jobDoc.exists() ? jobDoc.getString("title") : "Unknown Job";
                                String description = jobDoc.exists() ? jobDoc.getString("description") : "";

                                String startDate = "N/A";
                                String endDate = "N/A";
                                if (b.getCheckInTime() != null) {
                                    startDate = sdf.format(b.getCheckInTime().toDate());
                                    if (b.getCheckOutTime() != null) {
                                        endDate = sdf.format(b.getCheckOutTime().toDate());
                                    } else if (b.getActualDays() != null) {
                                        Calendar cal = Calendar.getInstance();
                                        cal.setTime(b.getCheckInTime().toDate());
                                        cal.add(Calendar.DAY_OF_YEAR, b.getActualDays());
                                        endDate = sdf.format(cal.getTime());
                                    }
                                }
                                
                                preview.append("■ ").append(jobTitle).append("\n");
                                if (description != null && !description.isEmpty()) {
                                    preview.append("  Desc: ").append(description).append("\n");
                                }
                                preview.append("  Period: ").append(startDate).append(" - ").append(endDate).append("\n");
                                preview.append("  Status: ").append(b.getStatus()).append(" (").append(b.getActualDays() != null ? b.getActualDays() : 0).append(" days)\n\n");
                            }
                            tvPreview.setText(preview.toString());
                        });
                    }
                });
        });
    }
}
