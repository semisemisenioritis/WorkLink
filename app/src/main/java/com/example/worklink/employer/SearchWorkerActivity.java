package com.example.worklink.employer;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

public class SearchWorkerActivity extends AppCompatActivity {

    EditText skill;
    Button search;
    ListView listView;
    DBHelper dbHelper;
    ArrayList<String> workerDisplayList;
    ArrayList<Integer> workerIds;
    int employerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.employer_activity_search_worker);

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        employerId = sharedPreferences.getInt("userId", -1);

        skill = findViewById(R.id.etSkill);
        search = findViewById(R.id.btnSearch);
        listView = findViewById(R.id.listWorkers);
        dbHelper = new DBHelper(this);

        search.setOnClickListener(v -> {
            workerDisplayList = new ArrayList<>();
            workerIds = new ArrayList<>();
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor cursor = db.rawQuery(
                    "SELECT u.id, u.name, w.skills, w.rating FROM users u " +
                            "JOIN worker_profile w ON u.id=w.worker_id " +
                            "WHERE w.skills LIKE ? AND w.availability=1",
                    new String[]{"%" + skill.getText().toString() + "%"}
            );

            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String data = cursor.getString(1) + " | " +
                        cursor.getString(2) + " | ⭐" +
                        cursor.getDouble(3);
                workerIds.add(id);
                workerDisplayList.add(data);
            }
            cursor.close();

            listView.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, workerDisplayList));
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            int selectedWorkerId = workerIds.get(position);
            String selectedWorkerName = workerDisplayList.get(position).split(" \\| ")[0];

            new AlertDialog.Builder(this)
                    .setTitle("Employ Worker")
                    .setMessage("Do you want to employ " + selectedWorkerName + "?")
                    .setPositiveButton("Yes", (dialog, which) -> employWorker(selectedWorkerId))
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void employWorker(int workerId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // In a real app, you'd select which job this is for. 
        // For simplicity, we'll find the latest open job by this employer.
        Cursor cursor = db.rawQuery("SELECT job_id FROM jobs WHERE employer_id=? AND status='OPEN' ORDER BY created_at DESC LIMIT 1",
                new String[]{String.valueOf(employerId)});

        if (cursor.moveToFirst()) {
            int jobId = cursor.getInt(0);
            
            ContentValues values = new ContentValues();
            values.put("job_id", jobId);
            values.put("worker_id", workerId);
            values.put("status", "ACCEPTED"); // Directly accepted for this flow

            db.insert("bookings", null, values);
            
            // Update job status to 'FILLED' so it doesn't show up again
            db.execSQL("UPDATE jobs SET status='FILLED' WHERE job_id=?", new Object[]{jobId});

            Toast.makeText(this, "Worker Employed!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please post a job first!", Toast.LENGTH_LONG).show();
        }
        cursor.close();
    }
}
