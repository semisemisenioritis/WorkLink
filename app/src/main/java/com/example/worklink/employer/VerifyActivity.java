package com.example.worklink.employer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

public class VerifyActivity extends AppCompatActivity {

    ListView listView;
    DBHelper dbHelper;
    ArrayList<String> bookingDisplayList;
    ArrayList<Integer> bookingIds;
    ArrayList<Integer> workerIds;
    ArrayList<Double> wages;
    int employerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_job_feed); // Reusing layout for simple list

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        employerId = sharedPreferences.getInt("userId", -1);

        listView = findViewById(R.id.jobListView);
        dbHelper = new DBHelper(this);
        
        loadBookings();

        listView.setOnItemClickListener((p, v, pos, id) -> {
            int bookingId = bookingIds.get(pos);
            int workerId = workerIds.get(pos);
            double wage = wages.get(pos);

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.execSQL("UPDATE bookings SET status='COMPLETED' WHERE booking_id=?",
                    new Object[]{bookingId});

            Toast.makeText(this, "Marked Completed. Proceeding to Payment.", Toast.LENGTH_SHORT).show();

            // Open Payment Activity and pass details
            Intent intent = new Intent(this, PaymentRatingActivity.class);
            intent.putExtra("bookingId", bookingId);
            intent.putExtra("workerId", workerId);
            intent.putExtra("amount", wage);
            startActivity(intent);
            finish();
        });
    }

    private void loadBookings() {
        bookingDisplayList = new ArrayList<>();
        bookingIds = new ArrayList<>();
        workerIds = new ArrayList<>();
        wages = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Find bookings for jobs posted by THIS employer
        Cursor cursor = db.rawQuery(
                "SELECT b.booking_id, u.name, j.title, j.wage, b.worker_id " +
                        "FROM bookings b " +
                        "JOIN jobs j ON b.job_id = j.job_id " +
                        "JOIN users u ON b.worker_id = u.id " +
                        "WHERE j.employer_id=? AND b.status='ACCEPTED'",
                new String[]{String.valueOf(employerId)});

        while (cursor.moveToNext()) {
            bookingIds.add(cursor.getInt(0));
            workerIds.add(cursor.getInt(4));
            wages.add(cursor.getDouble(3));
            
            String display = "Worker: " + cursor.getString(1) + 
                             "\nJob: " + cursor.getString(2) + 
                             "\nWage: $" + cursor.getDouble(3);
            bookingDisplayList.add(display);
        }
        cursor.close();

        listView.setAdapter(new ArrayAdapter<>(this,
                R.layout.list_item_white_text, bookingDisplayList));
        
        if (bookingDisplayList.isEmpty()) {
            Toast.makeText(this, "No active bookings to verify.", Toast.LENGTH_SHORT).show();
        }
    }
}
