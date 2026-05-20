package com.example.worklink.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.FirestoreManager;
import com.example.worklink.R;
import com.example.worklink.models.Application;
import com.example.worklink.models.Job;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class JobFeedActivity extends AppCompatActivity {

    private static final String TAG = "JobFeedActivity";
    ListView listView;
    ImageButton btnBack;
    ArrayList<String> jobsDisplayList;
    ArrayList<Job> fullJobsList;
    ArrayAdapter<String> adapter;
    String workerId;
    private ListenerRegistration jobListener;
    private ListenerRegistration appListener;
    private Set<String> appliedJobIds = new HashSet<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_job_feed);

        db = FirebaseFirestore.getInstance();
        
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        workerId = sharedPreferences.getString("userId", "");
        if (workerId.isEmpty() && FirebaseAuth.getInstance().getCurrentUser() != null) {
            workerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        if (workerId.isEmpty()) {
            Toast.makeText(this, "Error: User session not found. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnBack = findViewById(R.id.btnBack);
        listView = findViewById(R.id.jobListView);
        
        jobsDisplayList = new ArrayList<>();
        fullJobsList = new ArrayList<>();
        
        adapter = new ArrayAdapter<>(this, R.layout.list_item_white_text, jobsDisplayList);
        listView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        // Handle job clicks
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < fullJobsList.size()) {
                showApplyDialog(fullJobsList.get(position));
            }
        });

        // First listen to apps, then jobs
        startRealtimeAppListener();
    }

    private void startRealtimeAppListener() {
        appListener = db.collection("applications")
                .whereEqualTo("workerId", workerId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to applications", error);
                        return;
                    }
                    if (value != null) {
                        appliedJobIds.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            String status = doc.getString("status");
                            String jId = doc.getString("jobId");
                            // Only count as "applied" if the application is not withdrawn
                            if (jId != null && !"withdrawn".equals(status)) {
                                appliedJobIds.add(jId);
                            }
                        }
                        if (jobListener == null) startRealtimeJobListener();
                        else filterAndRefreshUI();
                    }
                });
    }

    private com.google.firebase.firestore.QuerySnapshot lastJobSnapshot;

    private void startRealtimeJobListener() {
        jobListener = FirestoreManager.getInstance().getAvailableJobsQuery()
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to jobs", error);
                        Toast.makeText(this, "Error loading jobs: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value != null) {
                        lastJobSnapshot = value;
                        filterAndRefreshUI();
                    }
                });
    }

    private void filterAndRefreshUI() {
        if (lastJobSnapshot == null) return;
        jobsDisplayList.clear();
        fullJobsList.clear();
        for (QueryDocumentSnapshot doc : lastJobSnapshot) {
            Job job = doc.toObject(Job.class);
            // Ensure jobId is set from document ID if missing
            if (job.getJobId() == null) {
                job.setJobId(doc.getId());
            }
            
            if (!appliedJobIds.contains(job.getJobId())) {
                fullJobsList.add(job);
                jobsDisplayList.add(job.getTitle() + " - ₹" + job.getWage() + "\n" + job.getLocation());
            }
        }
        adapter.notifyDataSetChanged();
        
        if (fullJobsList.isEmpty()) {
            Toast.makeText(this, "No new jobs available at the moment.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showApplyDialog(Job job) {
        new AlertDialog.Builder(this)
                .setTitle("Apply for Job")
                .setMessage("Do you want to send an application for \"" + job.getTitle() + "\"?")
                .setPositiveButton("Yes", (dialog, which) -> applyForJob(job))
                .setNegativeButton("No", null)
                .show();
    }

    private void applyForJob(Job job) {
        if (job.getJobId() == null) {
            Toast.makeText(this, "Error: Invalid Job ID", Toast.LENGTH_SHORT).show();
            return;
        }
        Application newApp = new Application(job.getJobId(), workerId);
        FirestoreManager.getInstance().applyForJob(newApp)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Applied Successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (jobListener != null) jobListener.remove();
        if (appListener != null) appListener.remove();
    }
}
