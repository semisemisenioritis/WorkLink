package com.example.worklink;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.worklink.api.ApiClient;
import com.example.worklink.api.SessionManager;
import com.example.worklink.api.models.AuthRequest;
import com.example.worklink.api.models.AuthResponse;
import com.example.worklink.api.models.WorkUser;
import com.example.worklink.worker.WorkerDashBoard;
import com.example.worklink.employer.EmployerDashBoard;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin, btnRegister;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        sessionManager = new SessionManager(this);

        btnLogin.setOnClickListener(v -> {
            String emailText = etEmail.getText().toString().trim();
            String passText = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(emailText) || TextUtils.isEmpty(passText)) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            performAuthLogin(emailText, passText);
        });

        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void performAuthLogin(String email, String password) {
        AuthRequest authRequest = new AuthRequest(email, password);
        ApiClient.getService(this).login(authRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessionManager.saveAuthToken(response.body().getAccessToken(), response.body().getRefreshToken());
                    fetchUserDetailsAndNavigate(email);
                } else {
                    Log.e("LOGIN_ERROR", "Login failed with code: " + response.code());
                    Toast.makeText(LoginActivity.this, "Invalid credentials or Server Error (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.e("LOGIN_ERROR", "Network failure", t);
                Toast.makeText(LoginActivity.this, "Login failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUserDetailsAndNavigate(String email) {
        ApiClient.getService(this).getWorkUsers().enqueue(new Callback<List<WorkUser>>() {
            @Override
            public void onResponse(Call<List<WorkUser>> call, Response<List<WorkUser>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WorkUser foundUser = null;
                    for (WorkUser user : response.body()) {
                        if (email.equalsIgnoreCase(user.getEmail())) {
                            foundUser = user;
                            break;
                        }
                    }

                    if (foundUser != null) {
                        sessionManager.saveUserDetail(foundUser.getId(), foundUser.getRole());
                        navigateToDashboard(foundUser.getRole());
                    } else {
                        Toast.makeText(LoginActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("LOGIN_ERROR", "Failed to fetch user list after login: " + response.code());
                    Toast.makeText(LoginActivity.this, "Login successful, but failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<WorkUser>> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Network error fetching profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToDashboard(String role) {
        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
        if ("Worker".equalsIgnoreCase(role)) {
            startActivity(new Intent(this, WorkerDashBoard.class));
        } else {
            startActivity(new Intent(this, EmployerDashBoard.class));
        }
        finish();
    }
}
