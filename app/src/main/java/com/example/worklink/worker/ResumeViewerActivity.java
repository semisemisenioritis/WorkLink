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
        Cursor cursor = db.rawQuery(
                "SELECT u.name, u.phone, w.skills, w.experience, w.rating " +
                "FROM users u JOIN worker_profile w ON u.id = w.worker_id " +
                "WHERE u.id = ?", new String[]{String.valueOf(workerId)});

        if (cursor.moveToFirst()) {
            String name = cursor.getString(0);
            String phone = cursor.getString(1);
            String skills = cursor.getString(2);
            int experience = cursor.getInt(3);
            double rating = cursor.getDouble(4);

            StringBuilder preview = new StringBuilder();
            preview.append("NAME: ").append(name).append("\n\n");
            preview.append("CONTACT: ").append(phone).append("\n\n");
            preview.append("EXPERIENCE: ").append(experience).append(" years\n\n");
            preview.append("OVERALL RATING: ").append(rating).append(" / 5.0\n\n");
            preview.append("SKILLS:\n");
            
            String[] skillArray = skills.split(",");
            for (String skill : skillArray) {
                preview.append("• ").append(skill.trim()).append("\n");
            }

            tvPreview.setText(preview.toString());
        }
        cursor.close();
    }
}
