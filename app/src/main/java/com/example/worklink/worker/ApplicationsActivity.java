package com.example.worklink.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

import java.util.ArrayList;

public class ApplicationsActivity extends AppCompatActivity {

    ListView listView;
    DBHelper dbHelper;
    ArrayList<ApplicationItem> applicationItems;
    ApplicationAdapter adapter;
    ImageButton btnBack;
    int workerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_applications);

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        workerId = sharedPreferences.getInt("userId", -1);

        listView = findViewById(R.id.lvApplications);
        btnBack = findViewById(R.id.btnBack);
        dbHelper = new DBHelper(this);
        applicationItems = new ArrayList<>();

        btnBack.setOnClickListener(v -> finish());

        loadApplications();
    }

    private void loadApplications() {
        applicationItems.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT j.title, a.status, a.applied_at, a.application_id FROM applications a " +
                "JOIN jobs j ON a.job_id = j.job_id " +
                "WHERE a.worker_id = ? " +
                "ORDER BY a.applied_at DESC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(workerId)});

        while (cursor.moveToNext()) {
            applicationItems.add(new ApplicationItem(
                cursor.getInt(3),
                cursor.getString(0),
                cursor.getString(1),
                cursor.getString(2)
            ));
        }
        cursor.close();

        adapter = new ApplicationAdapter(this, applicationItems);
        listView.setAdapter(adapter);
    }

    private void withdrawApplication(int appId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE applications SET status='withdrawn' WHERE application_id=?", new Object[]{appId});
        Toast.makeText(this, "Application Withdrawn", Toast.LENGTH_SHORT).show();
        loadApplications();
    }

    // Data class for applications
    static class ApplicationItem {
        int id;
        String title, status, date;

        ApplicationItem(int id, String title, String status, String date) {
            this.id = id;
            this.title = title;
            this.status = status;
            this.date = date;
        }
    }

    // Custom Adapter to handle the Withdraw button
    private class ApplicationAdapter extends ArrayAdapter<ApplicationItem> {
        ApplicationAdapter(Context context, ArrayList<ApplicationItem> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_application, parent, false);
            }

            ApplicationItem item = getItem(position);
            TextView info = convertView.findViewById(R.id.tvAppInfo);
            Button btnWithdraw = convertView.findViewById(R.id.btnWithdraw);

            String displayStatus = item.status.substring(0, 1).toUpperCase() + item.status.substring(1);
            info.setText("Job: " + item.title + "\nStatus: " + displayStatus + "\nApplied on: " + item.date);

            // Show withdraw button only for pending applications
            if (item.status.equals("pending")) {
                btnWithdraw.setVisibility(View.VISIBLE);
                btnWithdraw.setOnClickListener(v -> {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Withdraw Application")
                            .setMessage("Are you sure you want to withdraw this application?")
                            .setPositiveButton("Yes", (dialog, which) -> withdrawApplication(item.id))
                            .setNegativeButton("No", null)
                            .show();
                });
            } else {
                btnWithdraw.setVisibility(View.GONE);
            }

            return convertView;
        }
    }
}
