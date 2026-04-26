package com.example.worklink.worker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.FirestoreManager;
import com.example.worklink.LoginActivity;
import com.example.worklink.R;
import com.example.worklink.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class WorkerProfileActivity extends AppCompatActivity {

    EditText etSkills;
    TextView tvCurrentSkills;
    Button btnAddSkill;
    ImageButton btnBack;
    String workerId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_profile);

        db = FirebaseFirestore.getInstance();

        // Retrieve user session
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        workerId = sharedPreferences.getString("userId", "");

        if (workerId.isEmpty() && FirebaseAuth.getInstance().getCurrentUser() != null) {
            workerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else if (workerId.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        btnBack = findViewById(R.id.btnBack);
        etSkills = findViewById(R.id.etSkills);
        tvCurrentSkills = findViewById(R.id.tvCurrentSkills);
        btnAddSkill = findViewById(R.id.btnAddSkill);

        btnBack.setOnClickListener(v -> finish());
        
        loadProfileData();

        btnAddSkill.setOnClickListener(v -> {
            String newSkill = etSkills.getText().toString().trim();

            if (TextUtils.isEmpty(newSkill)) {
                Toast.makeText(this, "Enter a skill to add", Toast.LENGTH_SHORT).show();
                return;
            }

            FirestoreManager.getInstance().getUser(workerId).addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    User user = doc.toObject(User.class);
                    String currentSkills = user.getSkills();
                    String updatedSkills;

                    if (currentSkills == null || currentSkills.isEmpty() || currentSkills.equals("None")) {
                        updatedSkills = newSkill;
                    } else {
                        if (currentSkills.toLowerCase().contains(newSkill.toLowerCase())) {
                            Toast.makeText(this, "Skill already exists", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        updatedSkills = currentSkills + ", " + newSkill;
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("skills", updatedSkills);

                    db.collection("users").document(workerId).update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Skill added", Toast.LENGTH_SHORT).show();
                                etSkills.setText("");
                                loadProfileData();
                            });
                }
            });
        });
    }

    private void loadProfileData() {
        FirestoreManager.getInstance().getUser(workerId).addOnSuccessListener(doc -> {
            if (doc.exists()) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    String skills = user.getSkills();
                    tvCurrentSkills.setText("Skills: " + (TextUtils.isEmpty(skills) ? "None" : skills));
                }
            }
        });
    }
}
