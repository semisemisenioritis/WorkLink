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

        btnBack = findViewById(R.id.btnBack);
        listView = findViewById(R.id.jobListView);
        
        jobsDisplayList = new ArrayList<>();
        fullJobsList = new ArrayList<>();
        
        adapter = new ArrayAdapter<>(this, R.layout.list_item_white_text, jobsDisplayList);
        listView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        // First listen to apps, then jobs
        startRealtimeAppListener();
    }

    private void startRealtimeAppListener() {
        appListener = db.collection("applications")
                .whereEqualTo("workerId", workerId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        appliedJobIds.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            String jId = doc.getString("jobId");
                            if (jId != null) appliedJobIds.add(jId);
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
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
            if (!appliedJobIds.contains(job.getJobId())) {
                fullJobsList.add(job);
                jobsDisplayList.add(job.getTitle() + " - ₹" + job.getWage() + "\n" + job.getLocation());
            }
        }
        adapter.notifyDataSetChanged();
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
