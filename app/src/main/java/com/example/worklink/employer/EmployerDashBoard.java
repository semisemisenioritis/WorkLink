package com.example.worklink.employer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.worklink.LoginActivity;
import com.example.worklink.R;

public class EmployerDashBoard extends AppCompatActivity {

    Button searchWorkers, postJob, verify, logout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.employer_activity_dashboard);

        searchWorkers = findViewById(R.id.btnSearchWorkers);
        postJob = findViewById(R.id.btnPostJob);
        verify = findViewById(R.id.btnVerify);
        logout = findViewById(R.id.btnLogout);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        searchWorkers.setOnClickListener(v ->
                startActivity(new Intent(this, SearchWorkerActivity.class)));

        postJob.setOnClickListener(v ->
                startActivity(new Intent(this, PostJobActivity.class)));

        verify.setOnClickListener(v ->
                startActivity(new Intent(this, VerifyActivity.class)));

        logout.setOnClickListener(v -> {
            // Clear session
            SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            // Redirect to Login
            Intent intent = new Intent(EmployerDashBoard.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
