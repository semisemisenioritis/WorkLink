package com.example.worklink.employer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.R;
import com.example.worklink.models.Payment;
import com.example.worklink.models.Rating;
import com.example.worklink.models.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PaymentRatingActivity extends AppCompatActivity {

    EditText etAmount, etReview;
    RatingBar ratingBar;
    Button btnSubmit;
    String bookingId, employerId, workerId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.employer_activity_payment_rating);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        employerId = sharedPreferences.getString("userId", "");

        if (TextUtils.isEmpty(employerId) && FirebaseAuth.getInstance().getCurrentUser() != null) {
            employerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Get data from Intent
        bookingId = getIntent().getStringExtra("bookingId");
        workerId = getIntent().getStringExtra("workerId");
        double suggestedAmount = getIntent().getDoubleExtra("amount", 0.0);

        etAmount = findViewById(R.id.etAmount);
        etReview = findViewById(R.id.etReview);
        ratingBar = findViewById(R.id.ratingBar);
        btnSubmit = findViewById(R.id.btnSubmit);
        
        etAmount.setText(String.valueOf(suggestedAmount));

        btnSubmit.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSubmit.setEnabled(false);
            double paidAmount = Double.parseDouble(amountStr);
            int givenRating = (int) ratingBar.getRating();
            String reviewText = etReview.getText().toString();

            // 1. Record Payment
            Payment payment = new Payment();
            payment.setBookingId(bookingId);
            payment.setWorkerId(workerId);
            payment.setEmployerId(employerId);
            payment.setAmount(paidAmount);
            payment.setPaymentStatus("PAID");
            payment.setPaymentDate(Timestamp.now());

            db.collection("payments").add(payment).addOnSuccessListener(ref -> {
                // 2. Record Rating
                Rating rating = new Rating();
                rating.setBookingId(bookingId);
                rating.setGivenBy(employerId);
                rating.setGivenTo(workerId);
                rating.setRating(givenRating);
                rating.setReview(reviewText);
                rating.setCreatedAt(Timestamp.now());

                db.collection("ratings").add(rating).addOnSuccessListener(rateRef -> {
                    // 3. Update Worker Profile Stats
                    updateWorkerStats(workerId);
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Rating failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Payment failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(true);
            });
        });
    }

    private void updateWorkerStats(String workerId) {
        // We attempt to fetch ratings. If it fails due to index, we still finish the activity
        // so the user isn't stuck. The payment was already saved successfully.
        db.collection("ratings").whereEqualTo("givenTo", workerId).get()
            .addOnSuccessListener(snaps -> {
                int count = snaps.size();
                double sum = 0;
                for (com.google.firebase.firestore.DocumentSnapshot doc : snaps) {
                    Long r = doc.getLong("rating");
                    if (r != null) sum += r;
                }
                double avg = count > 0 ? sum / count : 0;

                Map<String, Object> updates = new HashMap<>();
                updates.put("workerRating", avg);
                updates.put("totalJobs", count);

                db.collection("users").document(workerId).update(updates).addOnCompleteListener(task -> {
                    Toast.makeText(this, "Submitted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            })
            .addOnFailureListener(e -> {
                Log.e("Firestore", "Stats update failed (likely index): " + e.getMessage());
                // Even if stats update fails, the payment is done.
                Toast.makeText(this, "Payment recorded. Stats will update once index is ready.", Toast.LENGTH_LONG).show();
                finish();
            });
    }
}
