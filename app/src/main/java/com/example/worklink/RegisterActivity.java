package com.example.worklink;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    EditText name, email, phone, username, password, etOTP;
    Spinner role;
    Button register, btnSendOTP;
    DBHelper dbHelper;
    boolean isEmailVerified = false;
    
    private static final String TAG = "WorkLink_Auth";
    // 10.0.2.2 is localhost for the Android Emulator
    private static final String BASE_URL = "http://10.0.2.2:8000";
    
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        name = findViewById(R.id.etName);
        email = findViewById(R.id.etEmail);
        phone = findViewById(R.id.etPhone);
        username = findViewById(R.id.etUsername);
        password = findViewById(R.id.etPassword);
        etOTP = findViewById(R.id.etOTP);
        role = findViewById(R.id.spRole);
        register = findViewById(R.id.btnSubmit);
        btnSendOTP = findViewById(R.id.btnSendOTP);

        dbHelper = new DBHelper(this);

        String[] roles = {"Worker", "Employer"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_white_text, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        role.setAdapter(adapter);

        btnSendOTP.setOnClickListener(v -> {
            String emailText = email.getText().toString().trim();
            if (TextUtils.isEmpty(emailText) || !Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                email.setError("Enter a valid email address");
                return;
            }
            Toast.makeText(this, "Requesting OTP...", Toast.LENGTH_SHORT).show();
            sendOTP(emailText);
        });

        register.setOnClickListener(v -> {
            if (!isEmailVerified) {
                String otpVal = etOTP.getText().toString().trim();
                if (TextUtils.isEmpty(otpVal)) {
                    Toast.makeText(this, "Please verify your email with OTP first", Toast.LENGTH_SHORT).show();
                    if (etOTP.getVisibility() != View.VISIBLE) {
                        email.setError("Click Send OTP first");
                    }
                    return;
                }
                verifyOTPAndRegister();
            } else {
                performRegistration();
            }
        });
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void sendOTP(String emailStr) {
        HttpUrl url = HttpUrl.parse(BASE_URL + "/send-otp");
        if (url == null) return;
        
        url = url.newBuilder().addQueryParameter("email", emailStr).build();

        // Some servers require a body for POST, even if empty
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create("", MediaType.parse("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                showToast("Connection Failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseData = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Code: " + response.code() + " Body: " + responseData);

                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        etOTP.setVisibility(View.VISIBLE);
                        btnSendOTP.setText("Resend");
                        Toast.makeText(RegisterActivity.this, "OTP Sent!", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    showToast("Server Error " + response.code() + ": " + responseData);
                }
            }
        });
    }

    private void verifyOTPAndRegister() {
        String emailText = email.getText().toString().trim();
        String otpText = etOTP.getText().toString().trim();

        HttpUrl url = HttpUrl.parse(BASE_URL + "/verify-otp").newBuilder()
                .addQueryParameter("email", emailText)
                .addQueryParameter("otp", otpText)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create("", MediaType.parse("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                showToast("Verification failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject json = new JSONObject(responseData);
                        if ("verified".equals(json.getString("status"))) {
                            isEmailVerified = true;
                            runOnUiThread(() -> performRegistration());
                        } else {
                            showToast("Invalid OTP code");
                        }
                    } catch (Exception e) {
                        showToast("Error reading server response");
                    }
                } else {
                    showToast("Server Error: " + response.code());
                }
            }
        });
    }

    private void performRegistration() {
        String nameText = name.getText().toString().trim();
        String emailText = email.getText().toString().trim();
        String phoneText = phone.getText().toString().trim();
        String userText = username.getText().toString().trim();
        String passText = password.getText().toString().trim();
        String selectedRole = role.getSelectedItem().toString();

        if (TextUtils.isEmpty(nameText) || TextUtils.isEmpty(phoneText) ||
            TextUtils.isEmpty(userText) || TextUtils.isEmpty(passText)) {
            runOnUiThread(() -> Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show());
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", nameText);
        values.put("email", emailText);
        values.put("phone", phoneText);
        values.put("username", userText);
        values.put("password", passText);
        values.put("role", selectedRole);

        try {
            long userId = db.insertOrThrow("users", null, values);
            if (userId != -1) {
                if ("Worker".equals(selectedRole)) {
                    ContentValues v = new ContentValues();
                    v.put("worker_id", userId);
                    v.put("email", emailText);
                    v.put("skills", "None");
                    v.put("experience", 0);
                    db.insert("worker_profile", null, v);
                } else {
                    ContentValues v = new ContentValues();
                    v.put("employer_id", userId);
                    v.put("email", emailText);
                    db.insert("employer_profile", null, v);
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        } catch (Exception e) {
            showToast("User already exists with this phone or username");
        }
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> 
            Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show()
        );
    }
}
