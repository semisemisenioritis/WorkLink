package com.example.worklink.employer;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

import java.util.Calendar;
import java.util.Locale;

public class PostJobActivity extends AppCompatActivity {

    EditText title, desc, location, wage, duration, skills, count, dateInput;
    Button post;
    DBHelper dbHelper;
    int employerId;
    final Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.employer_activity_post_job);

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        employerId = sharedPreferences.getInt("userId", -1);

        title = findViewById(R.id.etTitle);
        desc = findViewById(R.id.etDesc);
        location = findViewById(R.id.etLocation);
        wage = findViewById(R.id.etWage);
        duration = findViewById(R.id.etDuration);
        skills = findViewById(R.id.etSkills);
        count = findViewById(R.id.etCount);
        dateInput = findViewById(R.id.etDate);
        post = findViewById(R.id.btnPost);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        dbHelper = new DBHelper(this);

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

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("employer_id", employerId);
            values.put("title", titleStr);
            values.put("description", desc.getText().toString());
            values.put("location", location.getText().toString());
            values.put("wage", Double.parseDouble(wageStr));
            values.put("duration_days", Integer.parseInt(durationStr));
            values.put("required_skills", skills.getText().toString());
            values.put("workers_needed", Integer.parseInt(countStr));
            values.put("job_date", dateStr);
            values.put("status", "OPEN");

            long id = db.insert("jobs", null, values);

            if (id != -1) {
                Toast.makeText(this, "Job Posted Successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error posting job", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateLabel() {
        String myFormat = "yyyy-MM-dd";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(myFormat, Locale.US);
        dateInput.setText(sdf.format(calendar.getTime()));
    }
}
