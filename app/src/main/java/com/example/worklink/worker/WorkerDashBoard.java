package com.example.worklink.worker;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import com.example.worklink.DBHelper;
import com.example.worklink.R;
import com.example.worklink.worker.JobFeedActivity;
import com.example.worklink.worker.WorkerProfileActivity;
import com.example.worklink.worker.EarningsActivity;

public class WorkerDashBoard extends AppCompatActivity {

    Switch availabilitySwitch;
    Button jobFeed, profile, earnings;
    DBHelper dbHelper;
    int workerId = 1; // later get from login session

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_dashboard);

        availabilitySwitch = findViewById(R.id.switchAvailability);
        jobFeed = findViewById(R.id.btnJobFeed);
        profile = findViewById(R.id.btnProfile);
        earnings = findViewById(R.id.btnEarnings);

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

        // Open Profile
        profile.setOnClickListener(v ->
                startActivity(new Intent(this, WorkerProfileActivity.class))
        );

        // Open Earnings
        earnings.setOnClickListener(v ->
                startActivity(new Intent(this, EarningsActivity.class))
        );
    }
}
