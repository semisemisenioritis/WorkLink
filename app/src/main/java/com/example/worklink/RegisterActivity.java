package com.example.worklink;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.worklink.api.ApiClient;
import com.example.worklink.api.SessionManager;
import com.example.worklink.api.models.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;

public class RegisterActivity extends AppCompatActivity {

    EditText etName, etEmail, etPassword, etOtp;
    Spinner spRole;
    Button btnSubmit;
    boolean isOtpSent = false;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etOtp = findViewById(R.id.etOtp);
        spRole = findViewById(R.id.spRole);
        btnSubmit = findViewById(R.id.btnSubmit);

        sessionManager = new SessionManager(this);

        String[] roles = {"Worker", "Employer"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, roles);
        spRole.setAdapter(adapter);

        btnSubmit.setOnClickListener(v -> {
            if (!isOtpSent) {
                sendOtpFlow();
            } else {
                verifyAndRegisterFlow();
            }
        });
    }

    private void sendOtpFlow() {
        String emailText = etEmail.getText().toString().trim();
        String nameText = etName.getText().toString().trim();
        String passText = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(nameText) || TextUtils.isEmpty(emailText) || TextUtils.isEmpty(passText)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            etEmail.setError("Invalid email format");
            return;
        }

        btnSubmit.setEnabled(false);
        OtpRequest request = new OtpRequest(emailText);
        ApiClient.getService(this).sendOtp(request).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                btnSubmit.setEnabled(true);
                if (response.isSuccessful()) {
                    isOtpSent = true;
                    etOtp.setVisibility(View.VISIBLE);
                    btnSubmit.setText("Verify & Register");
                    Toast.makeText(RegisterActivity.this, "OTP sent to your email", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("API_ERROR", "Send OTP failed: " + response.code());
                    Toast.makeText(RegisterActivity.this, "Failed to send OTP. Maybe email already exists.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                btnSubmit.setEnabled(true);
                Log.e("API_ERROR", "Network error", t);
                Toast.makeText(RegisterActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyAndRegisterFlow() {
        String emailText = etEmail.getText().toString().trim();
        String passText = etPassword.getText().toString().trim();
        String otpText = etOtp.getText().toString().trim();

        if (TextUtils.isEmpty(otpText)) {
            etOtp.setError("Enter OTP");
            return;
        }

        btnSubmit.setEnabled(false);
        VerifyOtpRequest verifyRequest = new VerifyOtpRequest(emailText, otpText, passText);
        ApiClient.getService(this).verifyOtpRegister(verifyRequest).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (response.isSuccessful()) {
                    // Auth Registration successful. Now LOGIN to get token for subsequent calls.
                    loginAfterRegistration(emailText, passText);
                } else {
                    btnSubmit.setEnabled(true);
                    Log.e("API_ERROR", "OTP Verification failed: " + response.code());
                    Toast.makeText(RegisterActivity.this, "OTP Verification failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                btnSubmit.setEnabled(true);
                Log.e("API_ERROR", "Network error", t);
                Toast.makeText(RegisterActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginAfterRegistration(String email, String password) {
        AuthRequest loginRequest = new AuthRequest(email, password);
        ApiClient.getService(this).login(loginRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Save tokens so next API calls have the Authorization header
                    sessionManager.saveAuthToken(response.body().getAccessToken(), response.body().getRefreshToken());
                    
                    // Now proceed to create WorkUser and Profile
                    String nameText = etName.getText().toString().trim();
                    String selectedRole = spRole.getSelectedItem().toString();
                    createWorkUser(nameText, email, password, selectedRole);
                } else {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "Login failed after registration", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                btnSubmit.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Login network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createWorkUser(String name, String email, String password, String role) {
        WorkUserRequest request = new WorkUserRequest(name, email, password, role);
        // ApiClient interceptor will now automatically add the token we just saved
        ApiClient.getService(this).createWorkUser(request).enqueue(new Callback<WorkUser>() {
            @Override
            public void onResponse(Call<WorkUser> call, Response<WorkUser> response) {
                if (response.isSuccessful() && response.body() != null) {
                    createProfile(response.body().getId(), role);
                } else {
                    btnSubmit.setEnabled(true);
                    Log.e("API_ERROR", "Create WorkUser failed: " + response.code());
                    Toast.makeText(RegisterActivity.this, "Failed to create user record", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WorkUser> call, Throwable t) {
                btnSubmit.setEnabled(true);
                Log.e("API_ERROR", "Network error", t);
                Toast.makeText(RegisterActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createProfile(int userId, String selectedRole) {
        if ("Worker".equals(selectedRole)) {
            WorkerProfile profile = new WorkerProfile();
            profile.setWorkerId(userId);
            profile.setSkills(new ArrayList<>());
            profile.setExperience(0);
            
            ApiClient.getService(this).createWorkerProfile(profile).enqueue(new Callback<WorkerProfile>() {
                @Override
                public void onResponse(Call<WorkerProfile> call, Response<WorkerProfile> response) {
                    finishRegistration();
                }

                @Override
                public void onFailure(Call<WorkerProfile> call, Throwable t) {
                    finishRegistration();
                }
            });
        } else {
            EmployerProfile profile = new EmployerProfile();
            profile.setEmployerId(userId);
            profile.setCompanyName("New Company");
            profile.setLocation("Default Location");
            
            ApiClient.getService(this).createEmployerProfile(profile).enqueue(new Callback<EmployerProfile>() {
                @Override
                public void onResponse(Call<EmployerProfile> call, Response<EmployerProfile> response) {
                    finishRegistration();
                }

                @Override
                public void onFailure(Call<EmployerProfile> call, Throwable t) {
                    finishRegistration();
                }
            });
        }
    }

    private void finishRegistration() {
        Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}
