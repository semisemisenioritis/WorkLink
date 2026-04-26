package com.example.worklink.worker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.worklink.FirestoreManager;
import com.example.worklink.LoginActivity;
import com.example.worklink.R;
import com.example.worklink.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class WorkerDashBoard extends AppCompatActivity {

    SwitchCompat availabilitySwitch;
    Button jobFeed, profile, earnings, btnResume, btnApplications, logout;
    ImageButton btnBack;
    String workerId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_dashboard);

        db = FirebaseFirestore.getInstance();

        // Retrieve user session
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        workerId = sharedPreferences.getString("userId", "");

        if (TextUtils.isEmpty(workerId)) {
            // Fallback if session is lost
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                workerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            } else {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
        }

        btnBack = findViewById(R.id.btnBack);
        availabilitySwitch = findViewById(R.id.switchAvailability);
        jobFeed = findViewById(R.id.btnJobFeed);
        btnApplications = findViewById(R.id.btnApplications);
        profile = findViewById(R.id.btnProfile);
        earnings = findViewById(R.id.btnEarnings);
        btnResume = findViewById(R.id.btnViewResume);
        logout = findViewById(R.id.btnLogout);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Load initial state from Firestore
        loadAvailabilityState();

        // Toggle availability
        availabilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("availability", isChecked ? 1 : 0);

            db.collection("users").document(workerId).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, isChecked ? "You are now visible to employers" : "Applications Hidden", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update availability", Toast.LENGTH_SHORT).show();
                    });
        });

        // Navigation
        jobFeed.setOnClickListener(v -> startActivity(new Intent(this, JobFeedActivity.class)));
        btnApplications.setOnClickListener(v -> startActivity(new Intent(this, ApplicationsActivity.class)));
        profile.setOnClickListener(v -> startActivity(new Intent(this, WorkerProfileActivity.class)));
        earnings.setOnClickListener(v -> startActivity(new Intent(this, EarningsActivity.class)));
        btnResume.setOnClickListener(v -> startActivity(new Intent(this, ResumeViewerActivity.class)));

        // Logout
        logout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            Intent intent = new Intent(WorkerDashBoard.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadAvailabilityState() {
        FirestoreManager.getInstance().getUser(workerId).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null && user.getAvailability() != null) {
                    availabilitySwitch.setChecked(user.getAvailability() == 1);
                }
            }
        });
    }
}
