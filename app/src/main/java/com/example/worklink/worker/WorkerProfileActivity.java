package com.example.worklink.worker;

import android.content.Context;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
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
    int workerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_profile);

        // Retrieve user session
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        workerId = sharedPreferences.getInt("userId", -1);

        skills = findViewById(R.id.etSkills);
        experience = findViewById(R.id.etExperience);
        save = findViewById(R.id.btnSave);

        dbHelper = new DBHelper(this);
        
        loadProfileData();

        save.setOnClickListener(v -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put("skills", skills.getText().toString());
            String expStr = experience.getText().toString();
            values.put("experience", expStr.isEmpty() ? 0 : Integer.parseInt(expStr));

            int rows = db.update("worker_profile", values, "worker_id=?",
                    new String[]{String.valueOf(workerId)});

            if (rows > 0) {
                Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
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
            skills.setText(cursor.getString(0));
            experience.setText(String.valueOf(cursor.getInt(1)));
        }
        cursor.close();
    }
}
