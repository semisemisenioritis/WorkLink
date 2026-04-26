package com.example.worklink.employer;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.FirestoreManager;
import com.example.worklink.R;
import com.example.worklink.models.Job;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;
import java.util.Locale;

public class PostJobActivity extends AppCompatActivity {

    EditText title, desc, location, wage, duration, skills, count, dateInput;
    Button post;
    ImageButton btnBack;
    ProgressBar progressBar;
    String employerId;
    final Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.employer_activity_post_job);

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        employerId = sharedPreferences.getString("userId", "");

        if (employerId.isEmpty() && FirebaseAuth.getInstance().getCurrentUser() != null) {
            employerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        btnBack = findViewById(R.id.btnBack);
        title = findViewById(R.id.etTitle);
        desc = findViewById(R.id.etDesc);
        location = findViewById(R.id.etLocation);
        wage = findViewById(R.id.etWage);
        duration = findViewById(R.id.etDuration);
        skills = findViewById(R.id.etSkills);
        count = findViewById(R.id.etCount);
        dateInput = findViewById(R.id.etDate);
        post = findViewById(R.id.btnPost);
        progressBar = findViewById(R.id.progressBar);

        btnBack.setOnClickListener(v -> finish());

        // Date Picker logic
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            updateLabel();
        };

        dateInput.setOnClickListener(v -> new DatePickerDialog(PostJobActivity.this, dateSetListener,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show());

        post.setOnClickListener(v -> {
            String titleStr = title.getText().toString();
            String wageStr = wage.getText().toString();
            String durationStr = duration.getText().toString();
            String countStr = count.getText().toString();
            String dateStr = dateInput.getText().toString();

            if (titleStr.isEmpty() || wageStr.isEmpty() || countStr.isEmpty() || durationStr.isEmpty() || dateStr.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            post.setEnabled(false);

            Job newJob = new Job(employerId, titleStr, desc.getText().toString(), 
                                location.getText().toString(), Double.parseDouble(wageStr));
            
            newJob.setDurationDays(Integer.parseInt(durationStr));
            newJob.setRequiredSkills(skills.getText().toString());
            newJob.setWorkersNeeded(Integer.parseInt(countStr));
            newJob.setJobDate(dateStr);

            FirestoreManager.getInstance().postJob(newJob)
                .addOnSuccessListener(aVoid -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Job Posted Successfully in Realtime!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    post.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        });
    }

    private void updateLabel() {
        String myFormat = "yyyy-MM-dd";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(myFormat, Locale.US);
        dateInput.setText(sdf.format(calendar.getTime()));
    }
}
