package com.example.worklink.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.worklink.DBHelper;
import com.example.worklink.R;
import java.io.File;
import java.util.Locale;

public class ResumeViewerActivity extends AppCompatActivity {

    TextView tvPreview;
    Button btnDownload;
    DBHelper dbHelper;
    int workerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_resume_viewer);

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        workerId = sharedPreferences.getInt("userId", -1);

        tvPreview = findViewById(R.id.tvResumePreview);
        btnDownload = findViewById(R.id.btnDownloadPdf);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        dbHelper = new DBHelper(this);

        loadResumePreview();

        btnDownload.setOnClickListener(v -> {
            ResumeBuilder builder = new ResumeBuilder(this);
            File file = builder.generateResume(workerId);
            if (file != null) {
                Toast.makeText(this, "Resume Downloaded: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadResumePreview() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        // 1. Load basic info
        Cursor cursor = db.rawQuery(
                "SELECT u.name, u.phone, w.skills, w.rating " +
                "FROM users u JOIN worker_profile w ON u.id = w.worker_id " +
                "WHERE u.id = ?", new String[]{String.valueOf(workerId)});

        StringBuilder preview = new StringBuilder();
        if (cursor.moveToFirst()) {
            String name = cursor.getString(0);
            String phone = cursor.getString(1);
            String skills = cursor.getString(2);
            double rating = cursor.getDouble(3);

            preview.append("NAME: ").append(name).append("\n");
            preview.append("CONTACT: ").append(phone).append("\n");
            preview.append("OVERALL RATING: ").append(String.format(Locale.US, "%.1f", rating)).append(" / 5.0\n\n");
            
            preview.append("SKILLS:\n");
            String[] skillArray = skills.split(",");
            for (String skill : skillArray) {
                preview.append(" • ").append(skill.trim()).append("\n");
            }
            preview.append("\n");
        }
        cursor.close();

        // 2. Load Detailed Experience (Completed/Terminated Jobs)
        preview.append("EXPERIENCE:\n");
        // Use CASE to handle 0 or NULL actual_days for legacy data
        String expQuery = "SELECT j.title, j.description, j.location, j.job_date, " +
                "CASE WHEN b.actual_days IS NULL OR b.actual_days = 0 THEN j.duration_days ELSE b.actual_days END as duration, " +
                "b.status, j.required_skills " +
                "FROM bookings b " +
                "JOIN jobs j ON b.job_id = j.job_id " +
                "WHERE b.worker_id = ? AND (b.status = 'COMPLETED' OR b.status = 'TERMINATED') " +
                "ORDER BY j.job_date DESC";
        
        Cursor expCursor = db.rawQuery(expQuery, new String[]{String.valueOf(workerId)});
        
        if (expCursor.getCount() == 0) {
            preview.append(" No verified work experience yet.\n");
        } else {
            while (expCursor.moveToNext()) {
                String title = expCursor.getString(0);
                String desc = expCursor.getString(1);
                String loc = expCursor.getString(2);
                String date = expCursor.getString(3);
                int duration = expCursor.getInt(4);
                String status = expCursor.getString(5);
                String jobSkills = expCursor.getString(6);

                preview.append("■ ").append(title).append("\n");
                preview.append("  Skills Used: ").append(jobSkills).append("\n");
                preview.append("  Location: ").append(loc).append("\n");
                preview.append("  Start Date: ").append(date).append("\n");
                preview.append("  Duration: ").append(duration).append(" days\n");
                preview.append("  Description: ").append(desc).append("\n\n");
            }
        }
        expCursor.close();

        tvPreview.setText(preview.toString());
    }
}
