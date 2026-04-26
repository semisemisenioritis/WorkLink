package com.example.worklink;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.models.User;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    EditText etName, etPhone, etOTP;
    Spinner spRole;
    Button btnSendOTP, btnVerifyOTP;
    ImageButton btnBack;
    LinearLayout layoutUserInfo, layoutOTP;
    ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private String nameText, phoneText, selectedRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        btnBack = findViewById(R.id.btnBack);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etOTP = findViewById(R.id.etOTP);
        spRole = findViewById(R.id.spRole);
        btnSendOTP = findViewById(R.id.btnSendOTP);
        btnVerifyOTP = findViewById(R.id.btnVerifyOTP);
        layoutUserInfo = findViewById(R.id.layoutUserInfo);
        layoutOTP = findViewById(R.id.layoutOTP);
        progressBar = findViewById(R.id.progressBar);

        btnBack.setOnClickListener(v -> finish());

        String[] roles = {"Worker", "Employer"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_white_text, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRole.setAdapter(adapter);

        btnSendOTP.setOnClickListener(v -> startPhoneNumberVerification());

        btnVerifyOTP.setOnClickListener(v -> {
            String code = etOTP.getText().toString().trim();
            if (TextUtils.isEmpty(code)) {
                etOTP.setError("Enter code");
                return;
            }
            verifyPhoneNumberWithCode(mVerificationId, code);
        });
    }

    private void startPhoneNumberVerification() {
        nameText = etName.getText().toString().trim();
        phoneText = etPhone.getText().toString().trim();
        selectedRole = spRole.getSelectedItem().toString();

        if (TextUtils.isEmpty(nameText) || TextUtils.isEmpty(phoneText)) {
            Toast.makeText(this, "Name and Phone are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure phone starts with + for Firebase
        if (!phoneText.startsWith("+")) {
            Toast.makeText(this, "Please include country code (e.g., +91)", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSendOTP.setEnabled(false);

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneText)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallbacks)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
            progressBar.setVisibility(View.GONE);
            // Automatic verification or instant validation has succeeded
            signInWithPhoneAuthCredential(credential);
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            progressBar.setVisibility(View.GONE);
            btnSendOTP.setEnabled(true);
            Toast.makeText(RegisterActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCodeSent(@NonNull String verificationId,
                @NonNull PhoneAuthProvider.ForceResendingToken token) {
            progressBar.setVisibility(View.GONE);
            mVerificationId = verificationId;
            mResendToken = token;

            // Switch layouts
            layoutUserInfo.setVisibility(View.GONE);
            layoutOTP.setVisibility(View.VISIBLE);
            Toast.makeText(RegisterActivity.this, "OTP Sent", Toast.LENGTH_SHORT).show();
        }
    };

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = task.getResult().getUser().getUid();
                        saveUserToFirestore(uid);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(RegisterActivity.this, "Verification Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid) {
        User newUser = new User(uid, nameText, phoneText, "", selectedRole);
        
        // Initialize additional fields based on role
        if ("Worker".equals(selectedRole)) {
            newUser.setSkills("None");
            newUser.setExperience(0);
            newUser.setAvailability(1);
            newUser.setWorkerRating(0.0);
            newUser.setTotalJobs(0);
        } else {
            newUser.setEmployerRating(0.0);
            newUser.setTotalJobsPosted(0);
        }

        FirestoreManager.getInstance().saveUser(newUser)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                        finish(); // Go back to login or dashboard
                    } else {
                        Toast.makeText(RegisterActivity.this, "Failed to save profile: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
