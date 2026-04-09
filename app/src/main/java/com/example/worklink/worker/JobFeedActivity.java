package com.example.worklink.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

import java.util.ArrayList;

public class JobFeedActivity extends AppCompatActivity {

    ListView listView;
    ImageButton btnBack;
    DBHelper dbHelper;
    ArrayList<String> jobsList;
    ArrayList<Integer> jobIds;
    ArrayAdapter<String> adapter;
    int workerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_job_feed);

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        workerId = sharedPreferences.getInt("userId", -1);

        btnBack = findViewById(R.id.btnBack);
        listView = findViewById(R.id.jobListView);
        dbHelper = new DBHelper(this);
        jobsList = new ArrayList<>();
        jobIds = new ArrayList<>();

        btnBack.setOnClickListener(v -> finish());

        loadJobs();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            int jobId = jobIds.get(position);
            String jobTitle = jobsList.get(position).split(" - ")[0];

            new AlertDialog.Builder(this)
                    .setTitle("Apply for Job")
                    .setMessage("Do you want to send an application for \"" + jobTitle + "\"?")
                    .setPositiveButton("Yes", (dialog, which) -> applyForJob(jobId))
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void loadJobs() {
        jobsList.clear();
        jobIds.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        // Filter out jobs that the user has already applied for
        String query = "SELECT * FROM jobs WHERE status='OPEN' AND job_id NOT IN " +
                       "(SELECT job_id FROM applications WHERE worker_id = ?)";
        
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(workerId)});

        while (cursor.moveToNext()) {
            jobIds.add(cursor.getInt(0));
            String job = cursor.getString(2) + " - ₹" + cursor.getDouble(5);
            jobsList.add(job);
        }
        cursor.close();

        adapter = new ArrayAdapter<>(this,
                R.layout.list_item_white_text, jobsList);
        listView.setAdapter(adapter);
    }

    private void applyForJob(int jobId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Check if already applied (redundant now due to filtering, but good for safety)
        Cursor check = db.rawQuery("SELECT * FROM applications WHERE job_id=? AND worker_id=?",
                new String[]{String.valueOf(jobId), String.valueOf(workerId)});
        
        if (check.getCount() > 0) {
            Toast.makeText(this, "You have already applied for this job", Toast.LENGTH_SHORT).show();
            check.close();
            return;
        }
        check.close();

        try {
            db.execSQL("INSERT INTO applications (job_id, worker_id, status) VALUES (?, ?, 'pending')",
                    new Object[]{jobId, workerId});
            Toast.makeText(this, "Application Sent Successfully!", Toast.LENGTH_SHORT).show();
            
            // Refresh the feed to remove the applied job
            loadJobs();
        } catch (Exception e) {
            Toast.makeText(this, "Error sending application", Toast.LENGTH_SHORT).show();
        }
    }
}
