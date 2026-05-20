package com.example.worklink.employer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.R;
import com.example.worklink.models.Payment;
import com.example.worklink.models.Rating;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PaymentRatingActivity extends AppCompatActivity {

    private static final String TAG = "PaymentRatingActivity";
    EditText etAmount, etReview;
    RatingBar ratingBar;
    Button btnSubmit;
    String bookingId, employerId, workerId, newStatus;
    int actualDays;
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
        newStatus = getIntent().getStringExtra("newStatus");
        actualDays = getIntent().getIntExtra("actualDays", 1);
        double suggestedAmount = getIntent().getDoubleExtra("amount", 0.0);

        if (TextUtils.isEmpty(bookingId) || TextUtils.isEmpty(workerId)) {
            Toast.makeText(this, "Error: Missing booking or worker information.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        etAmount = findViewById(R.id.etAmount);
        etReview = findViewById(R.id.etReview);
        ratingBar = findViewById(R.id.ratingBar);
        btnSubmit = findViewById(R.id.btnSubmit);
        
        if (suggestedAmount > 0) {
            etAmount.setText(String.valueOf(suggestedAmount));
        }

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
            DocumentReference payRef = db.collection("payments").document();
            Payment payment = new Payment();
            payment.setPaymentId(payRef.getId());
            payment.setBookingId(bookingId);
            payment.setWorkerId(workerId);
            payment.setEmployerId(employerId);
            payment.setAmount(paidAmount);
            payment.setPaymentStatus("PAID");
            payment.setPaymentDate(Timestamp.now());

            payRef.set(payment).addOnSuccessListener(aVoid -> {
                // 2. Record Rating
                DocumentReference rateRef = db.collection("ratings").document();
                Rating rating = new Rating();
                rating.setRatingId(rateRef.getId());
                rating.setBookingId(bookingId);
                rating.setGivenBy(employerId);
                rating.setGivenTo(workerId);
                rating.setRating(givenRating);
                rating.setReview(reviewText);
                rating.setCreatedAt(Timestamp.now());

                rateRef.set(rating).addOnSuccessListener(aVoid2 -> {
                    // 3. Finalize Booking Status
                    finalizeBookingAndStats();
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

    private void finalizeBookingAndStats() {
        Map<String, Object> bookingUpdates = new HashMap<>();
        bookingUpdates.put("status", !TextUtils.isEmpty(newStatus) ? newStatus : "COMPLETED");
        bookingUpdates.put("actualDays", actualDays);

        db.collection("bookings").document(bookingId).update(bookingUpdates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Booking updated to " + newStatus);
                    updateWorkerStats(workerId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update booking status", e);
                    Toast.makeText(this, "Failed to finalize work: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                });
    }

    private void updateWorkerStats(String workerId) {
        db.collection("ratings").whereEqualTo("givenTo", workerId).get()
            .addOnSuccessListener(snaps -> {
                int count = snaps.size();
                double sum = 0;
                for (com.google.firebase.firestore.DocumentSnapshot doc : snaps) {
                    Long r = doc.getLong("rating");
                    if (r != null) sum += r;
                }
                double avg = count > 0 ? sum / count : 0.0;

                Map<String, Object> updates = new HashMap<>();
                updates.put("workerRating", avg);
                updates.put("totalJobs", count);

                db.collection("users").document(workerId).update(updates).addOnCompleteListener(task -> {
                    Toast.makeText(this, "Verified Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Stats update failed", e);
                finish();
            });
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Payment?")
                .setMessage("Verification will not be saved until you submit. Exit anyway?")
                .setPositiveButton("Yes, Exit", (dialog, which) -> super.onBackPressed())
                .setNegativeButton("No, Stay", null)
                .show();
    }
}
