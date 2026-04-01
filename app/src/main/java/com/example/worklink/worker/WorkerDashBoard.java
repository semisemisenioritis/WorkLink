package com.example.worklink.worker;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.example.worklink.DBHelper;
import com.example.worklink.LoginActivity;
import com.example.worklink.R;

public class WorkerDashBoard extends AppCompatActivity {

    SwitchCompat availabilitySwitch;
    Button jobFeed, profile, earnings, btnResume, btnApplications, logout;
    DBHelper dbHelper;
    int workerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_dashboard);

        // Retrieve user session
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        workerId = sharedPreferences.getInt("userId", -1);

        availabilitySwitch = findViewById(R.id.switchAvailability);
        jobFeed = findViewById(R.id.btnJobFeed);
        btnApplications = findViewById(R.id.btnApplications);
        profile = findViewById(R.id.btnProfile);
        earnings = findViewById(R.id.btnEarnings);
        btnResume = findViewById(R.id.btnViewResume);
        logout = findViewById(R.id.btnLogout);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        dbHelper = new DBHelper(this);

        // Load initial state
        loadAvailabilityState();

        // Toggle availability
        availabilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put("availability", isChecked ? 1 : 0);

            db.update("worker_profile", values, "worker_id=?",
                    new String[]{String.valueOf(workerId)});

            if (isChecked) {
                // Logic: Once turned ON, if the job post is no longer 'OPEN', mark the application as 'withdrawn'
                // This cleans up stale applications while keeping active ones visible.
                String cleanupQuery = "UPDATE applications SET status = 'withdrawn' " +
                        "WHERE worker_id = ? AND status = 'pending' AND job_id IN " +
                        "(SELECT job_id FROM jobs WHERE status != 'OPEN')";
                db.execSQL(cleanupQuery, new Object[]{workerId});
            }

            Toast.makeText(this, isChecked ? "You are now visible to employers" : "Applications Hidden", Toast.LENGTH_SHORT).show();
        });

        // Open Job Feed
        jobFeed.setOnClickListener(v ->
                startActivity(new Intent(this, JobFeedActivity.class))
        );

        // Open Applications Status
        btnApplications.setOnClickListener(v ->
                startActivity(new Intent(this, ApplicationsActivity.class))
        );

        // Open Profile
        profile.setOnClickListener(v ->
                startActivity(new Intent(this, WorkerProfileActivity.class))
        );

        // Open Earnings
        earnings.setOnClickListener(v ->
                startActivity(new Intent(this, EarningsActivity.class))
        );

        // Open Resume Viewer
        btnResume.setOnClickListener(v -> {
            startActivity(new Intent(this, ResumeViewerActivity.class));
        });

        // Logout
        logout.setOnClickListener(v -> {
            // Clear session
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            // Redirect to Login
            Intent intent = new Intent(WorkerDashBoard.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadAvailabilityState() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT availability FROM worker_profile WHERE worker_id=?",
                new String[]{String.valueOf(workerId)});
        if (cursor.moveToFirst()) {
            int availability = cursor.getInt(0);
            availabilitySwitch.setChecked(availability == 1);
        }
        cursor.close();
    }
}
