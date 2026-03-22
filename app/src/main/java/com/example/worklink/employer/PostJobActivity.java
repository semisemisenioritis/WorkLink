package com.example.worklink.employer;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

public class PostJobActivity extends AppCompatActivity {

    EditText title, desc, location, wage, skills, count;
    Button post;
    DBHelper dbHelper;
    int employerId = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.employer_activity_post_job);

        title = findViewById(R.id.etTitle);
        desc = findViewById(R.id.etDesc);
        location = findViewById(R.id.etLocation);
        wage = findViewById(R.id.etWage);
        skills = findViewById(R.id.etSkills);
        count = findViewById(R.id.etCount);
        post = findViewById(R.id.btnPost);

        dbHelper = new DBHelper(this);

        post.setOnClickListener(v -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            int num = Integer.parseInt(count.getText().toString());

            for (int i = 0; i < num; i++) {
                ContentValues values = new ContentValues();
                values.put("employer_id", employerId);
                values.put("title", title.getText().toString());
                values.put("description", desc.getText().toString());
                values.put("location", location.getText().toString());
                values.put("wage", Double.parseDouble(wage.getText().toString()));
                values.put("required_skills", skills.getText().toString());

                db.insert("jobs", null, values);
            }

            Toast.makeText(this, "Job(s) Posted", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
