package com.example.worklink.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

import java.util.ArrayList;

public class ApplicationsActivity extends AppCompatActivity {

    ListView listView;
    DBHelper dbHelper;
    ArrayList<String> applicationList;
    ArrayList<Integer> applicationIds;
    ArrayList<String> applicationStatuses;
    int workerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_applications);

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        workerId = sharedPreferences.getInt("userId", -1);

        listView = findViewById(R.id.lvApplications);
        dbHelper = new DBHelper(this);
        applicationList = new ArrayList<>();
        applicationIds = new ArrayList<>();
        applicationStatuses = new ArrayList<>();

        loadApplications();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (applicationIds.isEmpty()) return;
            
            int appId = applicationIds.get(position);
            String status = applicationStatuses.get(position);

            if (status.equals("pending")) {
                new AlertDialog.Builder(this)
                        .setTitle("Withdraw Application")
                        .setMessage("Are you sure you want to withdraw this application?")
                        .setPositiveButton("Yes", (dialog, which) -> withdrawApplication(appId))
                        .setNegativeButton("No", null)
                        .show();
            } else if (status.equals("withdrawn")) {
                Toast.makeText(this, "This application has already been withdrawn", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cannot withdraw an application that is " + status, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadApplications() {
        applicationList.clear();
        applicationIds.clear();
        applicationStatuses.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT j.title, a.status, a.applied_at, a.application_id FROM applications a " +
                "JOIN jobs j ON a.job_id = j.job_id " +
                "WHERE a.worker_id = ? " +
                "ORDER BY a.applied_at DESC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(workerId)});

        while (cursor.moveToNext()) {
            String title = cursor.getString(0);
            String status = cursor.getString(1);
            String date = cursor.getString(2);
            int id = cursor.getInt(3);

            applicationIds.add(id);
            applicationStatuses.add(status);

            String displayStatus = status.substring(0, 1).toUpperCase() + status.substring(1);
            String entry = "Job: " + title + "\nStatus: " + displayStatus + "\nApplied on: " + date;
            applicationList.add(entry);
        }
        cursor.close();

        if (applicationList.isEmpty()) {
            applicationList.add("No applications found.");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.list_item_white_text, applicationList);
        listView.setAdapter(adapter);
    }

    private void withdrawApplication(int appId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE applications SET status='withdrawn' WHERE application_id=?", new Object[]{appId});
        Toast.makeText(this, "Application Withdrawn", Toast.LENGTH_SHORT).show();
        loadApplications();
    }
}
