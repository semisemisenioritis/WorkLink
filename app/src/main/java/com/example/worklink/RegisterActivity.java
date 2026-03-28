package com.example.worklink;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText name, phone, username, password;
    Spinner role;
    Button register;
    DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        name = findViewById(R.id.etName);
        phone = findViewById(R.id.etPhone);
        username = findViewById(R.id.etUsername);
        password = findViewById(R.id.etPassword);
        role = findViewById(R.id.spRole);
        register = findViewById(R.id.btnSubmit);

        // 🟢 ACTION REQUIRED: Replace the URL below with YOUR Firebase Database URL
        // It's found at the top of the Realtime Database tab in Firebase Console
        String firebaseUrl = "https://worklink-7af36-default-rtdb.firebaseio.com";

        try {
            mDatabase = FirebaseDatabase.getInstance(firebaseUrl).getReference();
        } catch (Exception e) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            Log.e("FirebaseError", "Manual URL failed, trying default: " + e.getMessage());
        }

        String[] roles = {"Worker", "Employer"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, roles);
        role.setAdapter(adapter);

        register.setOnClickListener(v -> {
            String nameText = name.getText().toString().trim();
            String phoneText = phone.getText().toString().trim();
            String userText = username.getText().toString().trim();
            String passText = password.getText().toString().trim();
            String selectedRole = role.getSelectedItem().toString();

            if (TextUtils.isEmpty(nameText) || TextUtils.isEmpty(phoneText) ||
                TextUtils.isEmpty(userText) || TextUtils.isEmpty(passText)) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create User Object for Firebase
            String userId = mDatabase.child("users").push().getKey();
            
            Map<String, Object> userValues = new HashMap<>();
            userValues.put("id", userId);
            userValues.put("name", nameText);
            userValues.put("phone", phoneText);
            userValues.put("username", userText);
            userValues.put("password", passText);
            userValues.put("role", selectedRole);

            mDatabase.child("users").child(userText).setValue(userValues)
                .addOnSuccessListener(aVoid -> {
                    // Create Profile based on role in Firebase
                    if ("Worker".equals(selectedRole)) {
                        Map<String, Object> workerProfile = new HashMap<>();
                        workerProfile.put("worker_id", userId);
                        workerProfile.put("username", userText);
                        workerProfile.put("skills", "None");
                        workerProfile.put("experience", 0);
                        workerProfile.put("rating", 0.0);
                        workerProfile.put("availability", 1);
                        mDatabase.child("worker_profile").child(userText).setValue(workerProfile);
                    } else {
                        Map<String, Object> employerProfile = new HashMap<>();
                        employerProfile.put("employer_id", userId);
                        employerProfile.put("username", userText);
                        mDatabase.child("employer_profile").child(userText).setValue(employerProfile);
                    }

                    Toast.makeText(this, "Registered Successfully on Cloud", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Registration Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("FirebaseRegister", "Error: ", e);
                });
        });
    }
}
