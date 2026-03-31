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
    Button btnWithdrawJob;
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
        btnWithdrawJob = findViewById(R.id.btnWithdrawJob);
        listView = findViewById(R.id.listWorkers);
        dbHelper = new DBHelper(this);

        loadEmployerJobs();

        spJobs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!jobIds.isEmpty()) {
                    int selectedJobId = jobIds.get(position);
                    btnWithdrawJob.setVisibility(View.VISIBLE);
                    loadApplicants(selectedJobId);
                } else {
                    btnWithdrawJob.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                btnWithdrawJob.setVisibility(View.GONE);
            }
        });

        btnWithdrawJob.setOnClickListener(v -> {
            int position = spJobs.getSelectedItemPosition();
            if (position != AdapterView.INVALID_POSITION && !jobIds.isEmpty()) {
                int jobId = jobIds.get(position);
                String jobTitle = jobTitles.get(position);
                
                new AlertDialog.Builder(this)
                        .setTitle("Withdraw Job")
                        .setMessage("Are you sure you want to withdraw the job: \"" + jobTitle + "\"? All accepted applications will be cancelled.")
                        .setPositiveButton("Yes, Withdraw", (dialog, which) -> withdrawJob(jobId))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (workerIds.isEmpty()) return;
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
            btnWithdrawJob.setVisibility(View.GONE);
        } else {
            btnWithdrawJob.setVisibility(View.VISIBLE);
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
    }

    private void acceptApplication(int appId, int workerId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        Cursor cursor = db.rawQuery("SELECT job_id FROM applications WHERE application_id=?", 
                new String[]{String.valueOf(appId)});
        
        if (cursor.moveToFirst()) {
            int jobId = cursor.getInt(0);

            ContentValues bookingValues = new ContentValues();
            bookingValues.put("job_id", jobId);
            bookingValues.put("worker_id", workerId);
            bookingValues.put("status", "ACCEPTED");
            db.insert("bookings", null, bookingValues);

            db.execSQL("UPDATE applications SET status='accepted' WHERE application_id=?", new Object[]{appId});

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
                    Toast.makeText(this, "Applicant Accepted!", Toast.LENGTH_SHORT).show();
                }
            }
            jobCursor.close();
            
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

    private void withdrawJob(int jobId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        db.beginTransaction();
        try {
            // 1. Mark Job as CLOSED/CANCELLED
            db.execSQL("UPDATE jobs SET status='CANCELLED' WHERE job_id=?", new Object[]{jobId});
            
            // 2. Change all 'accepted' and 'pending' applications to 'cancelled'
            db.execSQL("UPDATE applications SET status='cancelled' WHERE job_id=? AND (status='accepted' OR status='pending')", 
                    new Object[]{jobId});
            
            // 3. Remove bookings for this job (optional, or mark them as cancelled)
            db.execSQL("DELETE FROM bookings WHERE job_id=?", new Object[]{jobId});
            
            db.setTransactionSuccessful();
            Toast.makeText(this, "Job Withdrawn Successfully", Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }
        
        loadEmployerJobs(); // Refresh spinner and list
    }
}
