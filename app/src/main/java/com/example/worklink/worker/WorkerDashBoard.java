package com.example.worklink.worker;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

        dbHelper = new DBHelper(this);

        // Toggle availability
        availabilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put("availability", isChecked ? 1 : 0);

            db.update("worker_profile", values, "worker_id=?",
                    new String[]{String.valueOf(workerId)});

            Toast.makeText(this, "Availability Updated", Toast.LENGTH_SHORT).show();
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
}
