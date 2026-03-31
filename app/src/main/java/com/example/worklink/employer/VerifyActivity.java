package com.example.worklink.employer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

public class VerifyActivity extends AppCompatActivity {

    ListView listView;
    DBHelper dbHelper;
    ArrayList<VerifyItem> verifyItems;
    VerifyAdapter adapter;
    int employerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_job_feed);

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        employerId = sharedPreferences.getInt("userId", -1);

        listView = findViewById(R.id.jobListView);
        dbHelper = new DBHelper(this);
        verifyItems = new ArrayList<>();
        
        loadBookings();
    }

    private void loadBookings() {
        verifyItems.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT b.booking_id, u.name, j.title, j.wage, b.worker_id, j.duration_days " +
                        "FROM bookings b " +
                        "JOIN jobs j ON b.job_id = j.job_id " +
                        "JOIN users u ON b.worker_id = u.id " +
                        "WHERE j.employer_id=? AND b.status='ACCEPTED'",
                new String[]{String.valueOf(employerId)});

        while (cursor.moveToNext()) {
            verifyItems.add(new VerifyItem(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getDouble(3),
                cursor.getInt(4),
                cursor.getInt(5)
            ));
        }
        cursor.close();

        adapter = new VerifyAdapter(this, verifyItems);
        listView.setAdapter(adapter);
        
        if (verifyItems.isEmpty()) {
            Toast.makeText(this, "No active bookings to verify.", Toast.LENGTH_SHORT).show();
        }
    }

    static class VerifyItem {
        int bookingId, workerId, duration;
        String workerName, jobTitle;
        double dailyWage, total;

        VerifyItem(int bId, String wName, String jTitle, double wage, int wId, int dur) {
            this.bookingId = bId;
            this.workerName = wName;
            this.jobTitle = jTitle;
            this.dailyWage = wage;
            this.workerId = wId;
            this.duration = dur;
            this.total = wage * dur;
        }
    }

    private class VerifyAdapter extends ArrayAdapter<VerifyItem> {
        VerifyAdapter(Context context, ArrayList<VerifyItem> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_verify, parent, false);
            }

            VerifyItem item = getItem(position);
            TextView info = convertView.findViewById(R.id.tvVerifyInfo);
            Button btnComplete = convertView.findViewById(R.id.btnMarkComplete);
            Button btnTerminate = convertView.findViewById(R.id.btnTerminate);

            info.setText("Worker: " + item.workerName + "\nJob: " + item.jobTitle + 
                         "\nTotal: ₹" + item.total + " (" + item.duration + " days)");

            btnComplete.setOnClickListener(v -> {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("UPDATE bookings SET status='COMPLETED' WHERE booking_id=?",
                        new Object[]{item.bookingId});

                Intent intent = new Intent(VerifyActivity.this, PaymentRatingActivity.class);
                intent.putExtra("bookingId", item.bookingId);
                intent.putExtra("workerId", item.workerId);
                intent.putExtra("amount", item.total);
                startActivity(intent);
                finish();
            });

            btnTerminate.setOnClickListener(v -> {
                showTerminateDialog(item);
            });

            return convertView;
        }
    }

    private void showTerminateDialog(VerifyItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Terminate Employment");
        builder.setMessage("Enter the actual number of days worked by " + item.workerName + ":");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Max days: " + item.duration);
        builder.setView(input);

        builder.setPositiveButton("Proceed to Payment", (dialog, which) -> {
            String daysStr = input.getText().toString();
            if (daysStr.isEmpty()) {
                Toast.makeText(this, "Please enter days worked", Toast.LENGTH_SHORT).show();
                return;
            }

            int daysWorked = Integer.parseInt(daysStr);
            if (daysWorked > item.duration) {
                Toast.makeText(this, "Days worked cannot exceed original duration", Toast.LENGTH_SHORT).show();
                return;
            }

            double newTotal = daysWorked * item.dailyWage;

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.execSQL("UPDATE bookings SET status='TERMINATED' WHERE booking_id=?",
                    new Object[]{item.bookingId});

            Intent intent = new Intent(VerifyActivity.this, PaymentRatingActivity.class);
            intent.putExtra("bookingId", item.bookingId);
            intent.putExtra("workerId", item.workerId);
            intent.putExtra("amount", newTotal);
            startActivity(intent);
            finish();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
