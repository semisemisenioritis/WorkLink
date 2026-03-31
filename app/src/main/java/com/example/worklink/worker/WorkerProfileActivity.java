package com.example.worklink.worker;

import android.content.Context;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

public class WorkerProfileActivity extends AppCompatActivity {

    EditText etSkills, etExperience;
    TextView tvCurrentSkills, tvCurrentExperience;
    Button btnAddSkill, btnUpdateExp;
    DBHelper dbHelper;
    int workerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_profile);

        // Retrieve user session
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        workerId = sharedPreferences.getInt("userId", -1);

        etSkills = findViewById(R.id.etSkills);
        etExperience = findViewById(R.id.etExperience);
        tvCurrentSkills = findViewById(R.id.tvCurrentSkills);
        tvCurrentExperience = findViewById(R.id.tvCurrentExperience);
        btnAddSkill = findViewById(R.id.btnAddSkill);
        btnUpdateExp = findViewById(R.id.btnUpdateExp);

        dbHelper = new DBHelper(this);
        
        loadProfileData();

        // Handle Adding a Single Skill
        btnAddSkill.setOnClickListener(v -> {
            String newSkill = etSkills.getText().toString().trim();

            if (TextUtils.isEmpty(newSkill)) {
                Toast.makeText(this, "Enter a skill to add", Toast.LENGTH_SHORT).show();
                return;
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // Get existing skills
            String currentSkills = "";
            Cursor cursor = db.rawQuery("SELECT skills FROM worker_profile WHERE worker_id=?", 
                    new String[]{String.valueOf(workerId)});
            if (cursor.moveToFirst()) {
                currentSkills = cursor.getString(0);
            }
            cursor.close();

            String updatedSkills;
            if (currentSkills == null || currentSkills.isEmpty() || currentSkills.equals("None")) {
                updatedSkills = newSkill;
            } else {
                // Prevent adding same skill twice
                if (currentSkills.toLowerCase().contains(newSkill.toLowerCase())) {
                    Toast.makeText(this, "Skill already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
                updatedSkills = currentSkills + ", " + newSkill;
            }

            ContentValues values = new ContentValues();
            values.put("skills", updatedSkills);

            db.update("worker_profile", values, "worker_id=?",
                    new String[]{String.valueOf(workerId)});

            Toast.makeText(this, "Skill added", Toast.LENGTH_SHORT).show();
            etSkills.setText("");
            loadProfileData();
        });

        // Handle Updating Experience separately
        btnUpdateExp.setOnClickListener(v -> {
            String expStr = etExperience.getText().toString().trim();

            if (TextUtils.isEmpty(expStr)) {
                Toast.makeText(this, "Enter years of experience", Toast.LENGTH_SHORT).show();
                return;
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("experience", Integer.parseInt(expStr));

            db.update("worker_profile", values, "worker_id=?",
                    new String[]{String.valueOf(workerId)});

            Toast.makeText(this, "Experience updated", Toast.LENGTH_SHORT).show();
            etExperience.setText("");
            loadProfileData();
        });
    }

    private void loadProfileData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT skills, experience FROM worker_profile WHERE worker_id=?",
                new String[]{String.valueOf(workerId)});
        
        if (cursor.moveToFirst()) {
            String skills = cursor.getString(0);
            int exp = cursor.getInt(1);
            
            tvCurrentSkills.setText("Skills: " + (TextUtils.isEmpty(skills) ? "None" : skills));
            tvCurrentExperience.setText("Experience: " + exp + " years");
        }
        cursor.close();
    }
}
