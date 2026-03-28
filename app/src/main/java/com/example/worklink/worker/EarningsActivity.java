package com.example.worklink.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Locale;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

public class EarningsActivity extends AppCompatActivity {

    DBHelper dbHelper;
    TextView earningsText, ratingText;
    ListView reviewsList;
    int workerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_earnings);

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        workerId = sharedPreferences.getInt("userId", -1);

        earningsText = findViewById(R.id.tvEarnings);
        ratingText = findViewById(R.id.tvOverallRating);
        reviewsList = findViewById(R.id.lvReviews);
        dbHelper = new DBHelper(this);

        loadData();
    }

    private void loadData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 1. Load Total Earnings
        Cursor earningsCursor = db.rawQuery(
                "SELECT SUM(p.amount) FROM payments p " +
                        "JOIN bookings b ON p.booking_id = b.booking_id " +
                        "WHERE b.worker_id=? AND p.payment_status='PAID'",
                new String[]{String.valueOf(workerId)}
        );
        if (earningsCursor.moveToFirst()) {
            earningsText.setText(String.format(Locale.getDefault(), "Total: ₹%.2f", earningsCursor.getDouble(0)));
        }
        earningsCursor.close();

        // 2. Load Overall Rating
        Cursor profileCursor = db.rawQuery(
                "SELECT rating FROM worker_profile WHERE worker_id=?",
                new String[]{String.valueOf(workerId)}
        );
        if (profileCursor.moveToFirst()) {
            ratingText.setText(String.format(Locale.getDefault(), "Rating: ⭐ %.1f", profileCursor.getDouble(0)));
        }
        profileCursor.close();

        // 3. Load Individual Reviews and Payments
        ArrayList<String> reviews = new ArrayList<>();
        Cursor reviewCursor = db.rawQuery(
                "SELECT r.rating, r.review, p.amount, j.title " +
                        "FROM ratings r " +
                        "JOIN bookings b ON r.booking_id = b.booking_id " +
                        "JOIN payments p ON b.booking_id = p.booking_id " +
                        "JOIN jobs j ON b.job_id = j.job_id " +
                        "WHERE b.worker_id=? " +
                        "ORDER BY r.created_at DESC",
                new String[]{String.valueOf(workerId)}
        );

        while (reviewCursor.moveToNext()) {
            int stars = reviewCursor.getInt(0);
            String comment = reviewCursor.getString(1);
            double amount = reviewCursor.getDouble(2);
            String jobTitle = reviewCursor.getString(3);

            String display = jobTitle + "\n" +
                             "Earned: ₹" + String.format(Locale.getDefault(), "%.2f", amount) + " | Rating: " + stars + "⭐\n" +
                             "\"" + (comment.isEmpty() ? "No comment" : comment) + "\"";
            reviews.add(display);
        }
        reviewCursor.close();

        if (reviews.isEmpty()) {
            reviews.add("No reviews yet.");
        }

        reviewsList.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, reviews));
    }
}
