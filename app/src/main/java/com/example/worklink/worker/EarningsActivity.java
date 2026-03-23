package com.example.worklink.worker;

import android.content.Context;
import android.content.SharedPreferences;
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
    int workerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_earnings);

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        workerId = sharedPreferences.getInt("userId", -1);

        earningsText = findViewById(R.id.tvEarnings);
        dbHelper = new DBHelper(this);

        loadEarnings();
    }

    private void loadEarnings() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query to sum up all PAID payments for this specific worker
        Cursor cursor = db.rawQuery(
                "SELECT SUM(p.amount) FROM payments p " +
                        "JOIN bookings b ON p.booking_id = b.booking_id " +
                        "WHERE b.worker_id=? AND p.payment_status='PAID'",
                new String[]{String.valueOf(workerId)}
        );

        if (cursor.moveToFirst()) {
            double total = cursor.getDouble(0);
            earningsText.setText("Total Earnings: ₹" + String.format("%.2f", total));
        } else {
            earningsText.setText("Total Earnings: ₹0.00");
        }
        cursor.close();
    }
}
