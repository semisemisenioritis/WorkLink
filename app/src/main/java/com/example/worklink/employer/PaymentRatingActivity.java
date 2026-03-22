package com.example.worklink.employer;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

public class PaymentRatingActivity extends AppCompatActivity {

    EditText amount, review;
    RatingBar ratingBar = findViewById(R.id.ratingBar);
    Button submit;
    DBHelper dbHelper;
    int bookingId = 1;
    int employerId = 2;
    int workerId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.employer_activity_payment_rating);

        amount = findViewById(R.id.etAmount);
        submit = findViewById(R.id.btnSubmit);
        review = findViewById(R.id.etReview);
        dbHelper = new DBHelper(this);

        submit.setOnClickListener(v -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // Payment
            ContentValues pay = new ContentValues();
            pay.put("booking_id", bookingId);
            pay.put("amount", Double.parseDouble(amount.getText().toString()));
            pay.put("payment_status", "PAID");

            db.insert("payments", null, pay);

            // Rating
            ContentValues rate = new ContentValues();
            rate.put("booking_id", bookingId);
            rate.put("given_by", employerId);
            rate.put("given_to", workerId);
            rate.put("rating", (int) ratingBar.getRating());

            db.insert("ratings", null, rate);

            Toast.makeText(this, "Payment & Rating Submitted", Toast.LENGTH_SHORT).show();
        });
    }
}
