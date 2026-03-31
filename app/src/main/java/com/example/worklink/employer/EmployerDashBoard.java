package com.example.worklink.employer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.worklink.R;

public class EmployerDashBoard extends AppCompatActivity {

    Button searchWorkers, postJob, verify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.employer_activity_dashboard);

        searchWorkers = findViewById(R.id.btnSearchWorkers);
        postJob = findViewById(R.id.btnPostJob);
        verify = findViewById(R.id.btnVerify);

        searchWorkers.setOnClickListener(v ->
                startActivity(new Intent(this, SearchWorkerActivity.class)));

        postJob.setOnClickListener(v ->
                startActivity(new Intent(this, PostJobActivity.class)));

        verify.setOnClickListener(v ->
                startActivity(new Intent(this, VerifyActivity.class)));
    }
}
