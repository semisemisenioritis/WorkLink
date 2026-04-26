package com.example.worklink;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.models.User;
import com.example.worklink.worker.WorkerDashBoard;
import com.example.worklink.employer.EmployerDashBoard;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    EditText etPhone, etOTP;
    Button btnSendOTP, btnLogin;
    TextView tvRegister;
    LinearLayout layoutPhoneInput, layoutOTP;
    ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private String mVerificationId;
    private String phoneText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etPhone = findViewById(R.id.etPhone);
        etOTP = findViewById(R.id.etOTP);
        btnSendOTP = findViewById(R.id.btnSendOTP);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        layoutPhoneInput = findViewById(R.id.layoutPhoneInput);
        layoutOTP = findViewById(R.id.layoutOTP);
        progressBar = findViewById(R.id.progressBar);

        btnSendOTP.setOnClickListener(v -> startPhoneNumberVerification());

        btnLogin.setOnClickListener(v -> {
            String code = etOTP.getText().toString().trim();
            if (TextUtils.isEmpty(code)) {
                etOTP.setError("Enter code");
                return;
            }
            verifyPhoneNumberWithCode(mVerificationId, code);
        });

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void startPhoneNumberVerification() {
        phoneText = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(phoneText)) {
            etPhone.setError("Phone number is required");
            return;
        }

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
            signInWithPhoneAuthCredential(credential);
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            progressBar.setVisibility(View.GONE);
            btnSendOTP.setEnabled(true);
            Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCodeSent(@NonNull String verificationId,
                @NonNull PhoneAuthProvider.ForceResendingToken token) {
            progressBar.setVisibility(View.GONE);
            mVerificationId = verificationId;

            layoutPhoneInput.setVisibility(View.GONE);
            layoutOTP.setVisibility(View.VISIBLE);
            Toast.makeText(LoginActivity.this, "OTP Sent", Toast.LENGTH_SHORT).show();
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
                        checkUserExistsAndRedirect(uid);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Verification Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserExistsAndRedirect(String uid) {
        FirestoreManager.getInstance().getUser(uid).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful() && task.getResult().exists()) {
                User user = task.getResult().toObject(User.class);
                if (user != null) {
                    // Save user session
                    SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("userId", uid); // Now using String UID
                    editor.putString("role", user.getRole());
                    editor.apply();

                    Toast.makeText(this, "Welcome back, " + user.getName(), Toast.LENGTH_SHORT).show();

                    if ("Worker".equals(user.getRole())) {
                        startActivity(new Intent(this, WorkerDashBoard.class));
                    } else {
                        startActivity(new Intent(this, EmployerDashBoard.class));
                    }
                    finish();
                    return;
                }
            }
            // If user doesn't exist in Firestore, they must register
            Toast.makeText(this, "No profile found. Please register.", Toast.LENGTH_LONG).show();
            mAuth.signOut();
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }
}
