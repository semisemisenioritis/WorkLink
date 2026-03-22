package com.example.worklink.worker;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

public class EarningsActivity extends AppCompatActivity {

    DBHelper dbHelper;
    TextView earningsText;
    int workerId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_earnings);

        earningsText = findViewById(R.id.tvEarnings);
        dbHelper = new DBHelper(this);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT SUM(amount) FROM payments p " +
                        "JOIN bookings b ON p.booking_id = b.booking_id " +
                        "WHERE b.worker_id=? AND p.payment_status='PAID'",
                new String[]{String.valueOf(workerId)}
        );

        if (cursor.moveToFirst()) {
            earningsText.setText("Total Earnings: ₹" + cursor.getDouble(0));
        }
    }
}
