package com.example.worklink.employer;

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
    ArrayList<String> bookings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_job_feed);

        listView = findViewById(R.id.jobListView);
        dbHelper = new DBHelper(this);
        bookings = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT booking_id, job_id FROM bookings WHERE status='ACCEPTED'",
                null);

        while (cursor.moveToNext()) {
            bookings.add("Booking #" + cursor.getInt(0));
        }

        listView.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, bookings));

        listView.setOnItemClickListener((p, v, pos, id) -> {

            Cursor c = db.rawQuery(
                    "SELECT booking_id FROM bookings WHERE status='ACCEPTED' LIMIT 1 OFFSET " + pos,
                    null);

            if (c.moveToFirst()) {
                int bookingId = c.getInt(0);

                db.execSQL("UPDATE bookings SET status='COMPLETED' WHERE booking_id=?",
                        new Object[]{bookingId});

                Toast.makeText(this, "Marked Completed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
