package com.example.worklink.employer;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

public class PaymentRatingActivity extends AppCompatActivity {

    EditText amount, review;
    RatingBar ratingBar;
    Button submit;
    DBHelper dbHelper;
    int bookingId;
    int employerId;
    int workerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.employer_activity_payment_rating);

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        employerId = sharedPreferences.getInt("userId", -1);

        // Get data from Intent
        bookingId = getIntent().getIntExtra("bookingId", -1);
        workerId = getIntent().getIntExtra("workerId", -1);
        double suggestedAmount = getIntent().getDoubleExtra("amount", 0.0);

        amount = findViewById(R.id.etAmount);
        review = findViewById(R.id.etReview);
        ratingBar = findViewById(R.id.ratingBar);
        submit = findViewById(R.id.btnSubmit);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        amount.setText(String.valueOf(suggestedAmount));
        
        dbHelper = new DBHelper(this);

        submit.setOnClickListener(v -> {
            String amountStr = amount.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // 1. Record Payment
            ContentValues pay = new ContentValues();
            pay.put("booking_id", bookingId);
            pay.put("amount", Double.parseDouble(amountStr));
            pay.put("payment_status", "PAID");
            pay.put("payment_date", System.currentTimeMillis()); // Using current time

            db.insert("payments", null, pay);

            // 2. Record Rating
            int givenRating = (int) ratingBar.getRating();
            ContentValues rate = new ContentValues();
            rate.put("booking_id", bookingId);
            rate.put("given_by", employerId);
            rate.put("given_to", workerId);
            rate.put("rating", givenRating);
            rate.put("review", review.getText().toString());

            db.insert("ratings", null, rate);

            // 3. Update Worker Profile (Average Rating and Total Jobs)
            updateWorkerStats(db, workerId);

            Toast.makeText(this, "Payment & Rating Submitted", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void updateWorkerStats(SQLiteDatabase db, int workerId) {
        // Calculate new average rating
        Cursor cursor = db.rawQuery("SELECT AVG(rating), COUNT(rating) FROM ratings WHERE given_to=?",
                new String[]{String.valueOf(workerId)});
        
        if (cursor.moveToFirst()) {
            double avgRating = cursor.getDouble(0);
            int totalJobs = cursor.getInt(1);

            ContentValues values = new ContentValues();
            values.put("rating", avgRating);
            values.put("total_jobs", totalJobs);

            db.update("worker_profile", values, "worker_id=?",
                    new String[]{String.valueOf(workerId)});
        }
        cursor.close();
    }
}
