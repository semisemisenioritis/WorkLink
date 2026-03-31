package com.example.worklink.employer;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

public class SearchWorkerActivity extends AppCompatActivity {

    Spinner spJobs;
    ListView listView;
    DBHelper dbHelper;
    ArrayList<String> workerDisplayList;
    ArrayList<Integer> workerIds;
    ArrayList<Integer> applicationIds;
    
    ArrayList<String> jobTitles;
    ArrayList<Integer> jobIds;
    
    int employerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.employer_activity_search_worker);

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        employerId = sharedPreferences.getInt("userId", -1);

        spJobs = findViewById(R.id.spJobs);
        listView = findViewById(R.id.listWorkers);
        dbHelper = new DBHelper(this);

        loadEmployerJobs();

        spJobs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!jobIds.isEmpty()) {
                    loadApplicants(jobIds.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            int selectedWorkerId = workerIds.get(position);
            int selectedAppId = applicationIds.get(position);
            String selectedWorkerName = workerDisplayList.get(position).split(" \\| ")[0];

            new AlertDialog.Builder(this)
                    .setTitle("Review Applicant")
                    .setMessage("Accept application from " + selectedWorkerName + "?")
                    .setPositiveButton("Accept", (dialog, which) -> acceptApplication(selectedAppId, selectedWorkerId))
                    .setNegativeButton("Reject", (dialog, which) -> rejectApplication(selectedAppId))
                    .setNeutralButton("Cancel", null)
                    .show();
        });
    }

    private void loadEmployerJobs() {
        jobTitles = new ArrayList<>();
        jobIds = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT job_id, title, workers_needed FROM jobs WHERE employer_id=? AND status='OPEN'", 
                new String[]{String.valueOf(employerId)});

        while (cursor.moveToNext()) {
            int jId = cursor.getInt(0);
            String title = cursor.getString(1);
            int needed = cursor.getInt(2);
            
            // Show how many slots are filled
            Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM bookings WHERE job_id=?", new String[]{String.valueOf(jId)});
            int filled = 0;
            if (countCursor.moveToFirst()) filled = countCursor.getInt(0);
            countCursor.close();

            jobIds.add(jId);
            jobTitles.add(title + " (" + filled + "/" + needed + ")");
        }
        cursor.close();

        if (jobTitles.isEmpty()) {
            jobTitles.add("No Open Jobs Found");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_white_text, jobTitles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spJobs.setAdapter(adapter);
    }

    private void loadApplicants(int jobId) {
        workerDisplayList = new ArrayList<>();
        workerIds = new ArrayList<>();
        applicationIds = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT a.application_id, u.id, u.name, w.skills, w.rating FROM applications a " +
                "JOIN users u ON a.worker_id = u.id " +
                "JOIN worker_profile w ON u.id = w.worker_id " +
                "WHERE a.job_id = ? AND a.status = 'pending'";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(jobId)});

        while (cursor.moveToNext()) {
            applicationIds.add(cursor.getInt(0));
            workerIds.add(cursor.getInt(1));
            String data = cursor.getString(2) + " | " +
                    cursor.getString(3) + " | ⭐" +
                    cursor.getDouble(4);
            workerDisplayList.add(data);
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.list_item_white_text, workerDisplayList);
        listView.setAdapter(adapter);
        
        if (workerDisplayList.isEmpty() && !jobIds.isEmpty()) {
            Toast.makeText(this, "No pending applicants for this job", Toast.LENGTH_SHORT).show();
        }
    }

    private void acceptApplication(int appId, int workerId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        Cursor cursor = db.rawQuery("SELECT job_id FROM applications WHERE application_id=?", 
                new String[]{String.valueOf(appId)});
        
        if (cursor.moveToFirst()) {
            int jobId = cursor.getInt(0);

            // 1. Create Booking
            ContentValues bookingValues = new ContentValues();
            bookingValues.put("job_id", jobId);
            bookingValues.put("worker_id", workerId);
            bookingValues.put("status", "ACCEPTED");
            db.insert("bookings", null, bookingValues);

            // 2. Update Application Status
            db.execSQL("UPDATE applications SET status='accepted' WHERE application_id=?", new Object[]{appId});

            // 3. Check if all slots are filled
            Cursor jobCursor = db.rawQuery("SELECT workers_needed FROM jobs WHERE job_id=?", new String[]{String.valueOf(jobId)});
            if (jobCursor.moveToFirst()) {
                int needed = jobCursor.getInt(0);
                
                Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM bookings WHERE job_id=?", new String[]{String.valueOf(jobId)});
                int filled = 0;
                if (countCursor.moveToFirst()) filled = countCursor.getInt(0);
                countCursor.close();

                if (filled >= needed) {
                    db.execSQL("UPDATE jobs SET status='FILLED' WHERE job_id=?", new Object[]{jobId});
                    Toast.makeText(this, "Job Fully Staffed and Closed!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Applicant Accepted! " + (needed - filled) + " more needed.", Toast.LENGTH_SHORT).show();
                }
            }
            jobCursor.close();
            
            // Refresh
            loadEmployerJobs(); 
        }
        cursor.close();
    }

    private void rejectApplication(int appId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE applications SET status='rejected' WHERE application_id=?", new Object[]{appId});
        Toast.makeText(this, "Application Rejected", Toast.LENGTH_SHORT).show();
        
        if (spJobs.getSelectedItemPosition() != AdapterView.INVALID_POSITION) {
            loadApplicants(jobIds.get(spJobs.getSelectedItemPosition()));
        }
    }
}
