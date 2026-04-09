package com.example.worklink.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

public class EarningsActivity extends AppCompatActivity {

    DBHelper dbHelper;
    TextView earningsText, ratingText, tvSelectedRange;
    Button btnDateRange, btnClear;
    ListView reviewsList;
    int workerId;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    SimpleDateFormat displaySdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    
    String startDateFilter = null;
    String endDateFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_earnings);

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        workerId = sharedPreferences.getInt("userId", -1);

        earningsText = findViewById(R.id.tvEarnings);
        ratingText = findViewById(R.id.tvOverallRating);
        tvSelectedRange = findViewById(R.id.tvSelectedRange);
        reviewsList = findViewById(R.id.lvReviews);
        btnDateRange = findViewById(R.id.btnDateRange);
        btnClear = findViewById(R.id.btnClearFilter);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        dbHelper = new DBHelper(this);

        btnDateRange.setOnClickListener(v -> showDateRangePicker());
        
        btnClear.setOnClickListener(v -> {
            startDateFilter = null;
            endDateFilter = null;
            tvSelectedRange.setText("Showing All Time");
            btnClear.setVisibility(View.GONE);
            loadData();
        });

        loadData();
    }

    private void showDateRangePicker() {
        MaterialDatePicker<Pair<Long, Long>> dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select Date Range")
                        .setSelection(
                            new Pair<>(
                                MaterialDatePicker.thisMonthInUtcMilliseconds(),
                                MaterialDatePicker.todayInUtcMilliseconds()
                            )
                        )
                        .build();

        dateRangePicker.show(getSupportFragmentManager(), "DATE_RANGE_PICKER");

        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            Long startDate = selection.first;
            Long endDate = selection.second;

            if (startDate != null && endDate != null) {
                startDateFilter = sdf.format(new Date(startDate));
                endDateFilter = sdf.format(new Date(endDate));
                
                String displayRange = displaySdf.format(new Date(startDate)) + " - " + displaySdf.format(new Date(endDate));
                tvSelectedRange.setText(displayRange);
                btnClear.setVisibility(View.VISIBLE);
                loadData();
            }
        });
    }

    private void loadData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 1. Load Total Earnings
        String earningsQuery = "SELECT SUM(p.amount) FROM payments p " +
                "JOIN bookings b ON p.booking_id = b.booking_id " +
                "WHERE b.worker_id=? AND p.payment_status='PAID'";
        
        ArrayList<String> args = new ArrayList<>();
        args.add(String.valueOf(workerId));

        if (startDateFilter != null && endDateFilter != null) {
            earningsQuery += " AND date(p.payment_date/1000, 'unixepoch') BETWEEN ? AND ?";
            args.add(startDateFilter);
            args.add(endDateFilter);
        }

        Cursor earningsCursor = db.rawQuery(earningsQuery, args.toArray(new String[0]));
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

        // 3. Load Individual Reviews
        ArrayList<String> reviews = new ArrayList<>();
        String reviewsQuery = "SELECT r.rating, r.review, p.amount, j.title, p.payment_date " +
                "FROM ratings r " +
                "JOIN bookings b ON r.booking_id = b.booking_id " +
                "JOIN payments p ON b.booking_id = p.booking_id " +
                "JOIN jobs j ON b.job_id = j.job_id " +
                "WHERE b.worker_id=?";

        ArrayList<String> rArgs = new ArrayList<>();
        rArgs.add(String.valueOf(workerId));

        if (startDateFilter != null && endDateFilter != null) {
            reviewsQuery += " AND date(p.payment_date/1000, 'unixepoch') BETWEEN ? AND ?";
            rArgs.add(startDateFilter);
            rArgs.add(endDateFilter);
        }

        reviewsQuery += " ORDER BY p.payment_date DESC";

        Cursor reviewCursor = db.rawQuery(reviewsQuery, rArgs.toArray(new String[0]));

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
            reviews.add("No earnings found for this period.");
        }

        reviewsList.setAdapter(new ArrayAdapter<>(this,
                R.layout.list_item_white_text, reviews));
    }
}
