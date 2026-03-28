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
    Button btnSave;
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
        btnSave = findViewById(R.id.btnSave);

        dbHelper = new DBHelper(this);
        
        loadProfileData();

        btnSave.setOnClickListener(v -> {
            String newSkills = etSkills.getText().toString().trim();
            String newExp = etExperience.getText().toString().trim();

            if (TextUtils.isEmpty(newSkills) || TextUtils.isEmpty(newExp)) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // Logic: Append new skills to existing ones
            String currentSkills = "";
            Cursor cursor = db.rawQuery("SELECT skills FROM worker_profile WHERE worker_id=?", 
                    new String[]{String.valueOf(workerId)});
            if (cursor.moveToFirst()) {
                currentSkills = cursor.getString(0);
            }
            cursor.close();

            String updatedSkills;
            if (currentSkills == null || currentSkills.isEmpty() || currentSkills.equals("None")) {
                updatedSkills = newSkills;
            } else {
                updatedSkills = currentSkills + ", " + newSkills;
            }

            ContentValues values = new ContentValues();
            values.put("skills", updatedSkills);
            values.put("experience", Integer.parseInt(newExp));

            int rows = db.update("worker_profile", values, "worker_id=?",
                    new String[]{String.valueOf(workerId)});

            if (rows > 0) {
                Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                etSkills.setText(""); // Clear input
                etExperience.setText(""); // Clear input
                loadProfileData(); // Refresh the "Current Profile" display
            } else {
                Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show();
            }
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
