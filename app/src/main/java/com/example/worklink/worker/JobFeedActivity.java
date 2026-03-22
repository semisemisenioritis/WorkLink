package com.example.worklink.worker;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

import java.util.ArrayList;

public class JobFeedActivity extends AppCompatActivity {

    ListView listView;
    DBHelper dbHelper;
    ArrayList<String> jobsList;
    ArrayAdapter<String> adapter;
    int workerId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_job_feed);

        listView = findViewById(R.id.jobListView);
        dbHelper = new DBHelper(this);
        jobsList = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM jobs WHERE status='OPEN'", null);

        while (cursor.moveToNext()) {
            String job = cursor.getString(2) + " - ₹" + cursor.getDouble(5);
            jobsList.add(job);
        }

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, jobsList);
        listView.setAdapter(adapter);

        // Apply job on click
        listView.setOnItemClickListener((parent, view, position, id) -> {

            Cursor c = db.rawQuery("SELECT job_id FROM jobs WHERE status='OPEN' LIMIT 1 OFFSET " + position, null);

            if (c.moveToFirst()) {
                int jobId = c.getInt(0);

                db.execSQL("INSERT INTO bookings (job_id, worker_id, status) VALUES (?, ?, 'PENDING')",
                        new Object[]{jobId, workerId});

                Toast.makeText(this, "Applied for Job", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
