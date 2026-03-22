package com.example.worklink.worker;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

public class WorkerProfileActivity extends AppCompatActivity {

    EditText skills, experience;
    Button save;
    DBHelper dbHelper;
    int workerId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_profile);

        skills = findViewById(R.id.etSkills);
        experience = findViewById(R.id.etExperience);
        save = findViewById(R.id.btnSave);

        dbHelper = new DBHelper(this);

        save.setOnClickListener(v -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put("skills", skills.getText().toString());
            values.put("experience", Integer.parseInt(experience.getText().toString()));

            db.update("worker_profile", values, "worker_id=?",
                    new String[]{String.valueOf(workerId)});

            Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
        });
    }
}
