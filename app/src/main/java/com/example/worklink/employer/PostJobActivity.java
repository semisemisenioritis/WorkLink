package com.example.worklink.employer;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.worklink.R;
import com.example.worklink.api.ApiClient;
import com.example.worklink.api.SessionManager;
import com.example.worklink.api.models.Job;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostJobActivity extends AppCompatActivity {

    EditText title, desc, location, wage, skills, count;
    Button post;
    SessionManager sessionManager;
    int employerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.employer_activity_post_job);

        sessionManager = new SessionManager(this);
        employerId = sessionManager.getUserId();

        title = findViewById(R.id.etTitle);
        desc = findViewById(R.id.etDesc);
        location = findViewById(R.id.etLocation);
        wage = findViewById(R.id.etWage);
        skills = findViewById(R.id.etSkills);
        count = findViewById(R.id.etCount);
        post = findViewById(R.id.btnPost);

        post.setOnClickListener(v -> {
            String titleText = title.getText().toString();
            String descText = desc.getText().toString();
            String locationText = location.getText().toString();
            String wageText = wage.getText().toString();
            String skillsText = skills.getText().toString();
            String countText = count.getText().toString();

            if (titleText.isEmpty() || wageText.isEmpty() || countText.isEmpty()) {
                Toast.makeText(this, "Title, Wage and Count are required", Toast.LENGTH_SHORT).show();
                return;
            }

            Job job = new Job();
            job.setEmployerId(employerId);
            job.setTitle(titleText);
            job.setDescription(descText);
            job.setLocation(locationText);
            job.setWage(Double.parseDouble(wageText));
            job.setRequiredSkills(skillsText);
            job.setNumWorkers(Integer.parseInt(countText));
            job.setStatus("OPEN");

            ApiClient.getService(this).createJob(job).enqueue(new Callback<Job>() {
                @Override
                public void onResponse(Call<Job> call, Response<Job> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(PostJobActivity.this, "Job Posted", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(PostJobActivity.this, "Failed to post job", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Job> call, Throwable t) {
                    Toast.makeText(PostJobActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
